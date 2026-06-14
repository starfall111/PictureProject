package org.example.common.annotation;

/**
 * 限流故障兜底策略
 *
 * <p>当 Redis 不可用时，AOP 根据 {@link RateLimit#fallback()} 决定放行还是拒绝。</p>
 *
 * @author Zou
 */
public enum FallbackStrategy {
    /**
     * 故障放行：Redis 异常时允许请求通过（默认，与 {@code RateLimitUtil} 现有策略一致）。
     * 适用于绝大多数业务接口——业务可用性优先于限流保护。
     */
    FAIL_OPEN,

    /**
     * 故障拒绝：Redis 异常时直接拒绝请求。
     * 仅用于关键安全接口（如秒杀抢券），避免 Redis 故障期间被刷穿。
     */
    FAIL_CLOSE
}
