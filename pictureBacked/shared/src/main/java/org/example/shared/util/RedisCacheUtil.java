package org.example.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;

/**
 * Redis 缓存工具类
 * 提供分布式锁缓存读取（防击穿 + 防穿透）和 SCAN 模式删除（替代 keys）
 *
 * 锁实现基于 Redisson RLock：看门狗自动续期（业务未结束锁不过期）、可重入、
 * 订阅通知式等待（锁释放立即唤醒，替代旧版固定 TTL + 自旋重试）
 *
 * @author Zou
 */
@Slf4j
@Component
public class RedisCacheUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 空值标记，用于防穿透
     */
    private static final String NULL_MARKER = "NULL";

    /**
     * 空值缓存过期时间（秒）
     */
    private static final int NULL_TTL_SECONDS = 60;

    /**
     * getWithLock 场景抢锁最长等待时间（秒）：等不到说明缓存即将被其他线程回填，直接快速失败
     */
    private static final int GET_LOCK_WAIT_SECONDS = 1;

    /**
     * executeWithLock 写操作场景抢锁最长等待时间（秒）
     */
    private static final int EXECUTE_LOCK_WAIT_SECONDS = 3;

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

        // 2. 抢锁：最多等 1s；不传 leaseTime → 看门狗自动续期（慢查询不会导致锁提前失效）
        RLock lock = redissonClient.getLock("lock:" + cacheKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(GET_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                lockFailCount.incrementAndGet();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前请求过多，请稍后重试");
            }

            // 3. 双重检查：获取锁后再次查缓存（锁等待期间可能已被回填）
            cacheValue = ops.get(cacheKey);
            if (cacheValue != null) {
                return NULL_MARKER.equals(cacheValue) ? null : cacheValue;
            }

            // 4. 查数据库并回填（空值短 TTL 防穿透）
            String result = loader.get();
            if (result == null) {
                ops.set(cacheKey, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
            } else {
                ops.set(cacheKey, result, ttlSeconds, TimeUnit.SECONDS);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前请求过多，请稍后重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();   // 看门狗随 unlock 停止续期
            }
        }
    }

    /**
     * 分布式锁执行器（写操作场景）
     * 封装 lock-acquire → action → lock-release 生命周期，避免重复编写锁模板代码
     *
     * @param lockKey   锁 key
     * @param action    业务逻辑
     * @param errorMsg  获取锁失败时的错误提示
     * @return 业务逻辑返回值
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action, String errorMsg) {
        return executeWithLock(lockKey, action, ErrorCode.OPERATION_ERROR, errorMsg);
    }

    /**
     * 分布式锁执行器（写操作场景，支持自定义抢锁失败的错误码）
     *
     * @param lockKey          锁 key
     * @param action           业务逻辑
     * @param lockFailErrorCode 抢锁失败抛出的错误码
     * @param lockFailMsg      抢锁失败抛出的错误信息
     * @return 业务逻辑返回值
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action, ErrorCode lockFailErrorCode, String lockFailMsg) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(EXECUTE_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            ThrowUtils.throwIf(!locked, lockFailErrorCode, lockFailMsg);
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(lockFailErrorCode, lockFailMsg);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 非阻塞式锁执行器：抢不到锁立即跳过且不抛异常
     * 适用于定时任务防多实例并发——任务已由其他实例执行时静默跳过本轮
     *
     * @param lockKey 锁 key
     * @param action  业务逻辑
     * @return true=已执行业务；false=未抢到锁（跳过）
     */
    public boolean tryExecuteWithLock(String lockKey, Runnable action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!locked) {
            lockFailCount.incrementAndGet();
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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
