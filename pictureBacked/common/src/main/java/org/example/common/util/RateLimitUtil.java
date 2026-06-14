package org.example.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
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
     * 限流结果状态。
     *
     * <p>三态语义：
     * <ul>
     *   <li>{@link #ALLOWED}：正常允许（窗口内未超限）</li>
     *   <li>{@link #REJECTED}：被拒绝（窗口内已超限）</li>
     *   <li>{@link #DEGRADED}：降级（Redis 故障或脚本返回 null），由 AOP 根据 {@code fallback} 决策放行/拒绝</li>
     * </ul>
     *
     * <p>引入三态的目的：让 {@code RateLimitAop} 能区分「Redis 故障」与「超限拒绝」，
     * 从而对关键接口（如秒杀 grab）启用 {@code FAIL_CLOSE} 兜底。
     */
    public enum Status {
        /**
         * 正常允许（窗口内未超限）。
         */
        ALLOWED,
        /**
         * 被拒绝（窗口内已超限，滑动窗口内请求数达到上限）。
         */
        REJECTED,
        /**
         * 降级（Redis 故障或脚本返回 null）。
         * <p>需配合 {@code RateLimitAop} 的 {@code fallback} 策略决策：FAIL_OPEN 放行 / FAIL_CLOSE 拒绝。</p>
         */
        DEGRADED
    }

    /**
     * 限流结果。
     *
     * <p>{@code status} 为决策主依据；{@code remaining}/{@code resetMs} 供日志和 429 响应使用。</p>
     *
     * <h3>向后兼容</h3>
     * <p>保留 {@link #allowed()} 方法供旧调用点（UserServiceImpl/SeckillServiceImpl/CouponServiceImpl 等
     * 未迁移到 AOP 的代码）继续使用：{@code DEGRADED} 视为允许（与 RateLimitUtil 历史 FAIL_OPEN 行为一致）。
     * 新代码（RateLimitAop）应使用 {@link #status()} 精细判断。</p>
     */
    public record Result(Status status, int remaining, long resetMs) {

        /**
         * 正常允许（{@link Status#ALLOWED}）。
         */
        public static Result allowed(int remaining, long resetMs) {
            return new Result(Status.ALLOWED, remaining, resetMs);
        }

        /**
         * 被拒绝（{@link Status#REJECTED}）。
         *
         * @param remaining 剩余次数（一般为 0）
         * @param resetMs   窗口重置时间戳（毫秒，一般传 {@code nowMs + windowMs} 以便 AOP 计算 Retry-After）
         */
        public static Result rejected(int remaining, long resetMs) {
            return new Result(Status.REJECTED, remaining, resetMs);
        }

        /**
         * 被拒绝（便捷工厂，remaining=0, resetMs=0）。供旧调用点兼容。
         */
        public static Result rejected() {
            return new Result(Status.REJECTED, 0, 0);
        }

        /**
         * 降级（{@link Status#DEGRADED}）。由 AOP 根据 fallback 决策放行/拒绝。
         */
        public static Result degraded(int remaining, long resetMs) {
            return new Result(Status.DEGRADED, remaining, resetMs);
        }

        /**
         * 兼容旧调用点（UserServiceImpl/SeckillServiceImpl/CouponServiceImpl 等未迁移代码）。
         *
         * <p>语义：{@code DEGRADED} 视为允许（FAIL_OPEN 是 RateLimitUtil 传统行为）。
         * 新代码（RateLimitAop）应使用 {@link #status()} 精细判断。</p>
         */
        public boolean allowed() {
            return status != Status.REJECTED;
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
        long resetMs = nowMs + windowMs;

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
                log.warn("限流脚本返回 null，降级: key={} | fallback 由 AOP 决策", redisKey);
                return Result.degraded(maxAttempts - 1, resetMs);
            }

            if (result == -1L) {
                // 窗口已满：resetMs 传真实窗口结束时间，便于 AOP 计算 Retry-After
                return Result.rejected(0, resetMs);
            }

            return Result.allowed(result.intValue(), resetMs);
        } catch (Exception e) {
            log.error("限流检查异常，降级: key={} | fallback 由 AOP 决策", redisKey, e);
            // 限流器故障：交由 AOP 根据 fallback 策略决策（FAIL_OPEN 放行 / FAIL_CLOSE 拒绝）
            return Result.degraded(maxAttempts - 1, resetMs);
        }
    }
}
