package org.example.picture.core.application.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.core.domain.model.PictureStatistics;
import org.example.shared.constants.RedisKeyConstants;
import org.example.picture.core.infrastructure.persistence.PictureStatisticsMapper;
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
 * 统计数据定时同步任务
 * 从 Redis 脏集合中取出 pictureId，将 Redis Hash 数据同步到 DB
 *
 * @author Zou
 */
@Slf4j
@Component
public class PictureStatisticsSyncScheduled {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private PictureStatisticsMapper pictureStatisticsMapper;

    /**
     * 每 5 分钟执行一次
     * TODO: 上线前确认 Redis 已开启 AOF 持久化（appendonly yes + appendfsync everysec）
     *       防止 Redis 重启导致未同步的 view/download/share 计数永久丢失
     */
    @Scheduled(fixedRate = 60000 * 5)
    public void syncStatistics() {
        // 分布式锁防多实例重复执行
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.STATS_SYNC_LOCK_KEY, "1", 50, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("统计同步任务已在其他实例执行，跳过");
            return;
        }

        try {
            doSync();
        } catch (Exception e) {
            log.error("统计同步任务执行异常", e);
        } finally {
            stringRedisTemplate.delete(RedisKeyConstants.STATS_SYNC_LOCK_KEY);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doSync() {
        Set<String> dirtyIds = stringRedisTemplate.opsForSet()
                .members(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        log.info("开始同步统计数据，待同步数量: {}", dirtyIds.size());

        // 转为有序列表，保证 Pipeline 读取和结果解析顺序一致
        List<String> dirtyIdList = new ArrayList<>(dirtyIds);

        // ========================
        // Phase 1: Pipeline 批量读取所有 Hash 数据（1 次网络往返）
        // ========================
        List<Object> rawResults = stringRedisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (String pictureIdStr : dirtyIdList) {
                            Long pictureId = Long.parseLong(pictureIdStr);
                            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
                            operations.opsForHash().get((K) statsKey, "likeCount");
                            operations.opsForHash().get((K) statsKey, "favoriteCount");
                            operations.opsForHash().get((K) statsKey, "shareCount");
                            operations.opsForHash().get((K) statsKey, "viewCount");
                            operations.opsForHash().get((K) statsKey, "downloadCount");
                        }
                        return null;
                    }
                }
        );

        // ========================
        // Phase 2: 解析结果 + DB 写入
        // ========================
        int successCount = 0;
        List<String> successIds = new ArrayList<>();

        for (int i = 0; i < dirtyIdList.size(); i++) {
            String pictureIdStr = dirtyIdList.get(i);
            try {
                Long pictureId = Long.parseLong(pictureIdStr);
                int base = i * 5;  // 每个 picture 有 5 个 Hash field

                PictureStatistics stat = new PictureStatistics();
                stat.setPictureId(pictureId);
                stat.setLikeCount(parseIntFromPipeline(rawResults.get(base)));
                stat.setFavoriteCount(parseIntFromPipeline(rawResults.get(base + 1)));
                stat.setShareCount(parseIntFromPipeline(rawResults.get(base + 2)));
                stat.setViewCount(parseIntFromPipeline(rawResults.get(base + 3)));
                stat.setDownloadCount(parseIntFromPipeline(rawResults.get(base + 4)));

                // 原子 UPSERT：INSERT ON DUPLICATE KEY UPDATE 消除竞态
                pictureStatisticsMapper.insertOrUpdate(stat);

                successIds.add(pictureIdStr);
                successCount++;
            } catch (Exception e) {
                log.warn("同步 pictureId={} 失败，下个周期重试: {}", pictureIdStr, e.getMessage());
            }
        }

        // ========================
        // Phase 3: Pipeline 批量清理 + 通知热度重算（1 次网络往返）
        // ========================
        if (!successIds.isEmpty()) {
            stringRedisTemplate.executePipelined(
                    new SessionCallback<Object>() {
                        @Override
                        public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                            for (String pictureIdStr : successIds) {
                                operations.opsForSet().remove((K) RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, pictureIdStr);
                                operations.opsForSet().add((K) RedisKeyConstants.REC_HOT_DIRTY_KEY, (V) pictureIdStr);
                            }
                            return null;
                        }
                    }
            );
        }

        log.info("统计同步完成，成功: {}/{}", successCount, dirtyIds.size());
    }

    /**
     * 从 Pipeline 返回结果中解析 int 值
     */
    private int parseIntFromPipeline(Object obj) {
        if (obj == null) {
            return 0;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
