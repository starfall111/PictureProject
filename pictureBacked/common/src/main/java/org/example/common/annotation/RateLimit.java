package org.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * <p>声明式接入限流：标注在 Controller 方法上，由 {@code RateLimitAop} 切面拦截执行。
 * 复用底层 {@code RateLimitUtil}（Redis Sorted Set + Lua 滑动窗口）。</p>
 *
 * <h3>配置优先级（从高到低）</h3>
 * <ol>
 *   <li>注解显式指定值（windowSeconds/maxAttempts 不为 -1）</li>
 *   <li>yml 配置（{@code ratelimit.rules} 按 resource 匹配）</li>
 *   <li>全局默认值（{@code ratelimit.default-window-seconds} / {@code ratelimit.default-max-attempts}）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#64;RateLimit(resource = "report.submit", dimensions = {RateLimitDimension.USER},
 *            windowSeconds = 60, maxAttempts = 3)
 * public BaseResponse&lt;?&gt; submitReport(&#64;RequestBody ReportDTO dto) { ... }
 *
 * // 未显式指定 resource 时，自动生成 "全限定类名.方法名"
 * // 未显式指定阈值时（-1），查 yml，yml 没有则用全局默认
 * &#64;RateLimit(dimensions = {RateLimitDimension.USER_OR_IP})
 * public BaseResponse&lt;User&gt; getUserById(&#64;PathVariable Long id) { ... }
 * </pre>
 *
 * @author Zou
 * @see RateLimitDimension
 * @see FallbackStrategy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 资源名（限流 key 的一部分）。
     * <p>为空时自动生成「全限定类名.方法名」（如 {@code org.example.server.controller.UserController.login}）。
     * 建议显式指定语义化资源名（如 "login"、"report.submit"）以便 yml 运维覆盖。</p>
     */
    String resource() default "";

    /**
     * 限流维度组合，决定 key 的构成方式。默认 {@code USER_OR_IP}。
     * <p>多维度组合时按固定顺序 JOIN：USER &gt; IP &gt; API &gt; CUSTOM。</p>
     */
    RateLimitDimension[] dimensions() default {RateLimitDimension.USER_OR_IP};

    /**
     * 滑动窗口大小（秒）。
     * <p>{@code -1} 为哨兵值表示「未显式设置」，AOP 会去查 yml，yml 没有则用全局默认。</p>
     */
    int windowSeconds() default -1;

    /**
     * 窗口内最大允许次数。
     * <p>{@code -1} 为哨兵值表示「未显式设置」，AOP 会去查 yml，yml 没有则用全局默认。</p>
     */
    int maxAttempts() default -1;

    /**
     * 故障兜底策略。默认 {@code FAIL_OPEN}（Redis 故障时放行）。
     */
    FallbackStrategy fallback() default FallbackStrategy.FAIL_OPEN;

    /**
     * 自定义 key 的 SpEL 表达式（仅当 {@link #dimensions()} 含 {@link RateLimitDimension#CUSTOM} 时生效）。
     * <p>例：{@code "#dto.targetType + ':' + #dto.targetId"}。
     * 解析失败时降级为「不拼 spelKey」并 log.warn，不抛异常。</p>
     */
    String spelKey() default "";
}
