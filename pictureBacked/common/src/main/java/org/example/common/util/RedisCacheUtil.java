package org.example.common.util;

import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Redis 缓存工具类
 * 提供分布式锁缓存读取（防击穿 + 防穿透）和 SCAN 模式删除（替代 keys）
 *
 * @author Zou
 */
@Slf4j
@Component
public class RedisCacheUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DefaultRedisScript<Long> releaseLockScript;

    /**
     * 空值标记，用于防穿透
     */
    private static final String NULL_MARKER = "NULL";

    /**
     * 空值缓存过期时间（秒）
     */
    private static final int NULL_TTL_SECONDS = 60;

    /**
     * 分布式锁超时时间（秒）
     */
    private static final int LOCK_TIMEOUT_SECONDS = 10;

    /**
     * 获取锁失败后每次等待时间（毫秒）
     */
    private static final int LOCK_WAIT_MS = 100;

    /**
     * 自旋重试次数
     */
    private static final int SPIN_RETRIES = 3;

    // ==================== 缓存监控计数器 ====================

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong lockFailCount = new AtomicLong();

    /**
     * 定时输出缓存统计日志（每 5 分钟）
     */
    // 使用 @Scheduled 需要依赖 Spring 的调度模块，这里用简单的方式
    private long lastLogTime = System.currentTimeMillis();

    /**
     * 带分布式锁的缓存读取（防击穿 + 防穿透）
     *
     * @param cacheKey   缓存 key
     * @param ttlSeconds 缓存过期时间（秒）
     * @param loader     数据库查询函数，返回 JSON 字符串，null 表示数据不存在
     * @return JSON 字符串结果，null 表示数据不存在
     */
    public String getWithLock(String cacheKey, int ttlSeconds, Supplier<String> loader) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        // 1. 查缓存
        String cacheValue = ops.get(cacheKey);
        if (cacheValue != null) {
            hitCount.incrementAndGet();
            return NULL_MARKER.equals(cacheValue) ? null : cacheValue;
        }
        missCount.incrementAndGet();
        maybeLogStats();

        // 2. 尝试获取分布式锁（使用 UUID 作为锁值，防止误解锁）
        String lockKey = "lock:" + cacheKey;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = ops.setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查：获取锁后再次查缓存
                cacheValue = ops.get(cacheKey);
                if (cacheValue != null) {
                    return NULL_MARKER.equals(cacheValue) ? null : cacheValue;
                }

                // 查数据库
                String result = loader.get();

                // 缓存结果（空值短 TTL 防穿透）
                if (result == null) {
                    ops.set(cacheKey, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                } else {
                    ops.set(cacheKey, result, ttlSeconds, TimeUnit.SECONDS);
                }
                return result;
            } finally {
                // Lua 脚本原子释放锁（仅删除自己持有的锁）
                stringRedisTemplate.execute(
                        releaseLockScript,
                        Collections.singletonList(lockKey),
                        lockValue
                );
            }
        } else {
            // 未获取锁，自旋重试等待锁持有者写入缓存
            lockFailCount.incrementAndGet();
            for (int i = 0; i < SPIN_RETRIES; i++) {
                try {
                    Thread.sleep(LOCK_WAIT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cacheValue = ops.get(cacheKey);
                if (cacheValue != null) {
                    return NULL_MARKER.equals(cacheValue) ? null : cacheValue;
                }
            }
            throw new org.example.common.exception.BusinessException(
                    org.example.common.exception.ErrorCode.OPERATION_ERROR, "当前请求过多，请稍后重试");
        }
    }

    /**
     * 使用 SCAN 按前缀删除 key（替代 keys 命令，避免阻塞 Redis）
     *
     * @param pattern key 匹配模式，如 "tag:page:*"
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 使用 Pipeline 批量 INCR 未读计数，减少 Redis 网络往返
     *
     * @param userIds 用户 ID 列表
     * @return userId → 最新未读计数的映射
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<Long, Long> pipelineIncrementUnread(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        // 第一阶段：pipeline 批量 INCR
        List<Object> rawResults = stringRedisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (Long userId : userIds) {
                            String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, userId);
                            operations.opsForValue().increment((K) unreadKey, 1);
                        }
                        return null;
                    }
                }
        );

        Map<Long, Long> result = new HashMap<>();
        if (rawResults == null) {
            return result;
        }

        // 收集需要设置 TTL 的 key（count == 1，即首次创建）
        final List<String> ttlKeys = new ArrayList<>();
        for (int i = 0; i < userIds.size() && i < rawResults.size(); i++) {
            Object raw = rawResults.get(i);
            Long count = raw instanceof Long ? (Long) raw : Long.parseLong(raw.toString());
            result.put(userIds.get(i), count);

            if (count == 1L) {
                ttlKeys.add(String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, userIds.get(i)));
            }
        }

        // 第二阶段：pipeline 批量设置 TTL（减少网络往返 + 防止中间崩溃导致 key 永不过期）
        if (!ttlKeys.isEmpty()) {
            stringRedisTemplate.executePipelined(
                    new SessionCallback<Object>() {
                        @Override
                        public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                            for (String key : ttlKeys) {
                                operations.expire((K) key, RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);
                            }
                            return null;
                        }
                    }
            );
        }

        return result;
    }

    /**
     * 每 5 分钟输出一次缓存统计日志
     * TODO: 上线前接入 Spring Boot Actuator + Prometheus，替换此简易方案
     */
    private void maybeLogStats() {
        long now = System.currentTimeMillis();
        if (now - lastLogTime < 300_000) {
            return;
        }
        lastLogTime = now;
        long hit = hitCount.get();
        long miss = missCount.get();
        long lockFail = lockFailCount.get();
        long total = hit + miss;
        String hitRate = total > 0 ? String.format("%.1f%%", hit * 100.0 / total) : "N/A";
        log.info("[Cache Stats] hit={}, miss={}, hitRate={}, lockFail={}", hit, miss, hitRate, lockFail);
    }
}
