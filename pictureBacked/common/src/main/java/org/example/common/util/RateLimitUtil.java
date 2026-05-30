package org.example.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 通用滑动窗口限流器
 *
 * <pre>
 * 使用 Redis Sorted Set + Lua 脚本实现原子滑动窗口：
 *   - key 格式: rate_limit:{resource}:{key}
 *   - 窗口内请求数 >= 阈值时拒绝
 *   - 返回剩余次数，方便前端展示
 *
 * 扩展方式:
 *   新接口限流只需调用 {@link #checkRateLimit(String, String, int, int)}
 * </pre>
 *
 * @author Zou
 */
@Slf4j
@Component
public class RateLimitUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "rateLimitScript")
    private DefaultRedisScript<Long> rateLimitScript;

    private static final String KEY_PREFIX = "rate_limit";

    /**
     * 限流结果
     */
    public record Result(boolean allowed, int remaining, long resetMs) {

        public static Result rejected() {
            return new Result(false, 0, 0);
        }

        public static Result allowed(int remaining, long resetMs) {
            return new Result(true, remaining, resetMs);
        }
    }

    /**
     * 检查是否被限流（原子操作）
     *
     * @param resource      资源标识（如 "login"、"api:upload"）
     * @param key           限流 key（如账号、IP）
     * @param windowSeconds 滑动窗口大小（秒）
     * @param maxAttempts   窗口内最大允许次数
     * @return 限流结果，包含是否允许、剩余次数、重置时间
     */
    public Result checkRateLimit(String resource, String key, int windowSeconds, int maxAttempts) {
        String redisKey = String.format("%s:%s:%s", KEY_PREFIX, resource, key);
        long nowMs = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        try {
            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    List.of(redisKey),
                    String.valueOf(windowMs),
                    String.valueOf(maxAttempts),
                    String.valueOf(nowMs),
                    String.valueOf(windowSeconds * 2) // TTL = 2倍窗口
            );

            if (result == null) {
                log.warn("限流脚本返回 null，放行请求: key={}", redisKey);
                return Result.allowed(maxAttempts - 1, nowMs + windowMs);
            }

            if (result == -1L) {
                return Result.rejected();
            }

            return Result.allowed(result.intValue(), nowMs + windowMs);
        } catch (Exception e) {
            log.error("限流检查异常，放行请求: key={}", redisKey, e);
            // 限流器故障时放行，避免影响正常业务
            return Result.allowed(maxAttempts - 1, nowMs + windowMs);
        }
    }
}
