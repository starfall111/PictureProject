package org.example.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.exception.RateLimitException;
import org.example.shared.result.BaseResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 限流异常处理器
 *
 * <p>统一处理 {@link RateLimitException}，返回 HTTP 429 + 标准 429 响应头：</p>
 * <ul>
 *   <li>{@code Retry-After}：建议客户端等待重试秒数</li>
 *   <li>{@code X-RateLimit-Remaining}：剩余次数（被拒绝时为 0）</li>
 *   <li>{@code X-RateLimit-Reset}：窗口重置毫秒时间戳</li>
 * </ul>
 *
 * <p>放在 server 模块而非 common 模块，避免 common 引入 spring-web 依赖。
 * 与 common 模块的 {@code GlobalExceptionHandler} 各司其职：本类只处理限流异常，
 * 其他业务异常仍由 {@code GlobalExceptionHandler} 处理。</p>
 *
 * <h3>前端收到的响应示例</h3>
 * <pre>
 * HTTP/1.1 429 Too Many Requests
 * Retry-After: 58
 * X-RateLimit-Remaining: 0
 * X-RateLimit-Reset: 1718299980000
 * Content-Type: application/json
 *
 * {"code":42900,"data":null,"message":"请求过于频繁，请稍后再试"}
 * </pre>
 *
 * @author Zou
 */
@Slf4j
@RestControllerAdvice
public class RateLimitExceptionHandler {

    /**
     * 处理限流异常，返回 HTTP 429。
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<BaseResponse<?>> handleRateLimitException(RateLimitException e) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(e.getRetryAfterSec()));
        headers.set("X-RateLimit-Remaining", String.valueOf(e.getRemaining()));
        headers.set("X-RateLimit-Reset", String.valueOf(e.getResetMs()));

        BaseResponse<?> body = new BaseResponse<>(e.getCode(), null, e.getMessage());

        log.debug("[RATE-LIMIT] 返回 429 | retryAfter={}s | resetMs={}",
                e.getRetryAfterSec(), e.getResetMs());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(body);
    }
}
