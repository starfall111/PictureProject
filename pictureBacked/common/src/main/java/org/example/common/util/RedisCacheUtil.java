package org.example.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
            return NULL_MARKER.equals(cacheValue) ? null : cacheValue;
        }

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
}
