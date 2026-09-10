package org.example.shared.exception;

import lombok.Getter;

/**
 * 限流异常
 *
 * <p>当请求被限流拦截时抛出。继承 {@link BusinessException} 以复用 {@code GlobalExceptionHandler} 的处理链，
 * 同时携带限流相关的元数据（剩余次数、重置时间、建议重试等待秒数），
 * 由 {@code RateLimitExceptionHandler} 统一转换为 HTTP 429 响应。</p>
 *
 * @author Zou
 */
@Getter
public class RateLimitException extends BusinessException {

    /**
     * 窗口内剩余可用次数（被拒绝时通常为 0）。
     */
    private final int remaining;

    /**
     * 窗口重置时间（毫秒时间戳）。前端可据此显示倒计时。
     */
    private final long resetMs;

    /**
     * 建议客户端等待重试的秒数（至少为 1）。
     * 通过 {@code Retry-After} 响应头返回。
     */
    private final int retryAfterSec;

    /**
     * @param remaining    剩余次数（被拒绝时为 0）
     * @param resetMs      窗口重置毫秒时间戳
     * @param retryAfterSec 建议重试秒数
     */
    public RateLimitException(int remaining, long resetMs, int retryAfterSec) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.remaining = remaining;
        this.resetMs = resetMs;
        this.retryAfterSec = retryAfterSec;
    }
}
