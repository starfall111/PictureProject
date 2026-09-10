package org.example.picture.social.application.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.picture.core.domain.model.PictureWithStats;
import org.example.picture.core.infrastructure.persistence.PictureMapper;
import org.example.picture.social.application.RecommendService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 热度评分定时同步任务
 * <p>
 * 增量：每 10 分钟从 rec:hot:dirty 读取变更图片，重算热度分数
 * 全量：每天凌晨 3 点全量重算并持久化到 DB
 * <p>
 * 脏集合流转：
 * 社交操作 → social:stats:dirty
 * → PictureStatisticsSyncScheduled（每 5 分钟，同步 DB 后写入 rec:hot:dirty）
 * → HotScoreSyncScheduled（每 10 分钟，消费 rec:hot:dirty）
 *
 * @author Zou
 */
@Slf4j
@Component
public class HotScoreSyncScheduled {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private RecommendService recommendService;

    /**
     * 每 10 分钟增量更新热度分数
     */
    @Scheduled(fixedRate = 600000)
    public void incrementalScoreUpdate() {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.LOCK_REC_SCORE_KEY, "1", 120, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("热度增量更新任务已在其他实例执行，跳过");
            return;
        }

        try {
            doIncrementalUpdate();
        } catch (Exception e) {
            log.error("热度增量更新任务异常", e);
        } finally {
            stringRedisTemplate.delete(RedisKeyConstants.LOCK_REC_SCORE_KEY);
        }
    }

    /**
     * 每天凌晨 3 点全量重算热度分数（含 DB 持久化）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void fullScoreRebuild() {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.LOCK_REC_SCORE_KEY, "1", 600, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("热度全量重算任务已在其他实例执行，跳过");
            return;
        }

        try {
            int count = recommendService.rebuildHotScores();
            log.info("每日全量热度重算完成，共处理 {} 张图片", count);
        } catch (Exception e) {
            log.error("热度全量重算任务异常", e);
        } finally {
            stringRedisTemplate.delete(RedisKeyConstants.LOCK_REC_SCORE_KEY);
        }
    }

    /**
     * 增量更新：从 rec:hot:dirty 读取变更图片 → 联表查数据 → Pipeline 写入 ZSET
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doIncrementalUpdate() {
        Set<String> dirtyIds = stringRedisTemplate.opsForSet()
                .members(RedisKeyConstants.REC_HOT_DIRTY_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        log.info("开始增量更新热度分数，脏图片数量: {}", dirtyIds.size());

        List<Long> pictureIds = new ArrayList<>();
        for (String idStr : dirtyIds) {
            try {
                pictureIds.add(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                log.warn("脏集合中存在无效 pictureId: {}", idStr);
            }
        }

        if (pictureIds.isEmpty()) {
            return;
        }

        // 联表查询图片及统计数据
        List<PictureWithStats> statsList = pictureMapper.selectWithStats(pictureIds);
        if (statsList == null || statsList.isEmpty()) {
            // 清理无效的脏标记
            for (String idStr : dirtyIds) {
                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.REC_HOT_DIRTY_KEY, idStr);
            }
            return;
        }

        // Pipeline 批量写入 ZSET（减少 Redis 网络往返）
        stringRedisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (PictureWithStats stats : statsList) {
                            double score = recommendService.calculateHotScore(stats);
                            operations.opsForZSet().add(
                                    (K) RedisKeyConstants.REC_HOT_ZSET_KEY,
                                    (V) String.valueOf(stats.getId()),
                                    score
                            );
                            if (stats.getCategoryId() != null) {
                                String catKey = String.format(RedisKeyConstants.REC_HOT_CAT_ZSET_KEY, stats.getCategoryId());
                                operations.opsForZSet().add((K) catKey, (V) String.valueOf(stats.getId()), score);
                            }
                        }
                        return null;
                    }
                }
        );

        // 清理已处理的脏标记
        for (String idStr : dirtyIds) {
            stringRedisTemplate.opsForSet().remove(RedisKeyConstants.REC_HOT_DIRTY_KEY, idStr);
        }

        log.info("热度增量更新完成，更新 {} 张图片", statsList.size());
    }
}
