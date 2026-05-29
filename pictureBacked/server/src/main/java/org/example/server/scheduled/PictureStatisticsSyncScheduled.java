package org.example.server.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.entity.PictureStatistics;
import org.example.common.constants.RedisKeyConstants;
import org.example.server.mapper.PictureStatisticsMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
     */
    @Scheduled(fixedRate = 300000)
    public void syncStatistics() {
        // 分布式锁防多实例重复执行
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.STATS_SYNC_LOCK_KEY, "1", 240, TimeUnit.SECONDS);
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

    private void doSync() {
        Set<String> dirtyIds = stringRedisTemplate.opsForSet()
                .members(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        log.info("开始同步统计数据，待同步数量: {}", dirtyIds.size());

        int successCount = 0;
        for (String pictureIdStr : dirtyIds) {
            try {
                Long pictureId = Long.parseLong(pictureIdStr);
                String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

                // 读取 Redis Hash
                String likeCount = (String) stringRedisTemplate.opsForHash().get(statsKey, "likeCount");
                String favoriteCount = (String) stringRedisTemplate.opsForHash().get(statsKey, "favoriteCount");
                String shareCount = (String) stringRedisTemplate.opsForHash().get(statsKey, "shareCount");
                String viewCount = (String) stringRedisTemplate.opsForHash().get(statsKey, "viewCount");
                String downloadCount = (String) stringRedisTemplate.opsForHash().get(statsKey, "downloadCount");

                // 组装实体
                PictureStatistics stat = new PictureStatistics();
                stat.setPictureId(pictureId);
                stat.setLikeCount(likeCount != null ? Integer.parseInt(likeCount) : 0);
                stat.setFavoriteCount(favoriteCount != null ? Integer.parseInt(favoriteCount) : 0);
                stat.setShareCount(shareCount != null ? Integer.parseInt(shareCount) : 0);
                stat.setViewCount(viewCount != null ? Integer.parseInt(viewCount) : 0);
                stat.setDownloadCount(downloadCount != null ? Integer.parseInt(downloadCount) : 0);

                // UPSERT：先尝试 update，失败则 insert
                int updated = pictureStatisticsMapper.updateById(stat);
                if (updated == 0) {
                    pictureStatisticsMapper.insert(stat);
                }

                // 同步成功，从脏集合移除
                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, pictureIdStr);
                successCount++;
            } catch (Exception e) {
                log.warn("同步 pictureId={} 失败，下个周期重试: {}", pictureIdStr, e.getMessage());
            }
        }

        log.info("统计同步完成，成功: {}/{}", successCount, dirtyIds.size());
    }
}
