package org.example.picture.social.application.scheduling;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.picture.social.infrastructure.persistence.UserFollowMapper;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 关注计数定时同步任务
 * 每天凌晨 4 点执行，以 DB 为准校准 Redis 中的关注数/粉丝数缓存
 *
 * @author Zou
 */
@Slf4j
@Component
public class FollowCountSyncScheduled {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserFollowMapper userFollowMapper;

    /**
     * 每天凌晨 4 点执行（避免与热度 3 点冲突）
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void syncFollowCount() {
        // 分布式锁防多实例重复执行，30 分钟超时
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.FOLLOW_SYNC_LOCK_KEY, "1", 30 * 60, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("关注计数同步任务已在其他实例执行，跳过");
            return;
        }

        try {
            doSync();
        } catch (Exception e) {
            log.error("关注计数同步任务执行异常", e);
        } finally {
            stringRedisTemplate.delete(RedisKeyConstants.FOLLOW_SYNC_LOCK_KEY);
        }
    }

    private void doSync() {
        // 1. SCAN 匹配所有 follow:count:* 的 key，收集 userId
        Set<Long> userIds = scanUserIds();

        if (userIds.isEmpty()) {
            log.info("关注计数同步任务：无缓存 key 需要校准");
            return;
        }

        log.info("开始同步关注计数，待校准用户数: {}", userIds.size());

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                // 2. 查 DB 真实计数
                long dbFollowCount = userFollowMapper.countFollowing(userId);
                long dbFollowerCount = userFollowMapper.countFollowers(userId);

                // 3. 以 DB 为准，覆盖 Redis
                String followKey = String.format(RedisKeyConstants.FOLLOW_COUNT_KEY, userId);
                String followerKey = String.format(RedisKeyConstants.FOLLOWER_COUNT_KEY, userId);
                int ttl = RedisKeyConstants.FOLLOW_COUNT_TTL_BASE
                        + RandomUtil.randomInt(0, RedisKeyConstants.FOLLOW_COUNT_TTL_JITTER);

                stringRedisTemplate.opsForValue().set(followKey, String.valueOf(dbFollowCount), ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().set(followerKey, String.valueOf(dbFollowerCount), ttl, TimeUnit.SECONDS);

                successCount++;
            } catch (Exception e) {
                log.warn("同步关注计数失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        log.info("关注计数同步完成，成功: {}/{}", successCount, userIds.size());
    }

    /**
     * SCAN 匹配 follow:count:* 的 key，解析出 userId 集合
     */
    private Set<Long> scanUserIds() {
        Set<Long> userIds = new HashSet<>();

        // SCAN 匹配 follow:count:*:following
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match("follow:count:*").count(200).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long userId = parseUserId(key);
                if (userId != null) {
                    userIds.add(userId);
                }
            }
        }

        return userIds;
    }

    /**
     * 从 key 中解析 userId
     * follow:count:{userId}:following → userId
     * follow:count:{userId}:followers → userId
     */
    private Long parseUserId(String key) {
        try {
            // key 格式: follow:count:123:following 或 follow:count:456:followers
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                return Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.warn("解析 userId 失败: key={}", key);
        }
        return null;
    }
}
