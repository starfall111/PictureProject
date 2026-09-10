package org.example.bootstrap.aop;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.shared.annotation.FallbackStrategy;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserContext;
import org.example.identity.api.UserEnum;
import org.example.shared.exception.RateLimitException;
import org.example.shared.util.IpUtils;
import org.example.shared.util.RateLimitUtil;
import org.example.identity.api.model.User;
import org.example.shared.config.RateLimitProperties;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 限流切面
 *
 * <p>拦截标注 {@link RateLimit} 的 Controller 方法，执行限流检查：
 * 管理员白名单 → 解析 resource → 合并配置 → 提取维度组成 → 拼装 key →
 * 调用 {@link RateLimitUtil#checkRateLimit} → 放行 / 抛 {@link RateLimitException}。</p>
 *
 * <h3>执行顺序</h3>
 * <p>{@code @Order(50)}，确保在 {@code CheckAuthAop}（无 @Order，默认优先级）之后执行，
 * 保证 UserContext 在限流检查前已由 {@code LoginInterceptor} + {@code CheckAuthAop} 完成。</p>
 *
 * <h3>故障兜底（Phase 2 起）</h3>
 * <p>{@link RateLimitUtil.Status#DEGRADED} 时由注解 {@code fallback} 决策：
 * <ul>
 *   <li>{@link FallbackStrategy#FAIL_OPEN}（默认）：放行，业务可用性优先（绝大多数业务接口）</li>
 *   <li>{@link FallbackStrategy#FAIL_CLOSE}：拒绝，避免关键接口在 Redis 故障期间被刷穿（仅秒杀 grab 等安全接口）</li>
 * </ul>
 *
 * @author Zou
 */
@Slf4j
@Aspect
@Component
@Order(50)
public class RateLimitAop {

    @Resource
    private RateLimitUtil rateLimitUtil;

    @Resource
    private RateLimitProperties rateLimitProperties;

    /**
     * SpEL 解析器（懒加载，CUSTOM 维度时使用）。
     */
    private final ExpressionParser spelParser = new SpelExpressionParser();

    /**
     * 方法参数名发现器（SpEL 解析需要参数名）。
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 获取方法签名（用于自动生成 resource 和 SpEL 解析）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 2. 管理员白名单：管理员角色直接放行
        User currentUser = UserContext.get();
        if (currentUser != null && isadmin(currentUser)) {
            return joinPoint.proceed();
        }

        // 3. 解析 resource：注解为空 → "全限定类名.方法名"
        String resource = resolveResource(rateLimit, method);

        // 4. 合并配置：注解值 != -1 用注解值，否则查 yml，yml 没有则用全局默认
        int windowSeconds = resolveWindow(rateLimit, resource);
        int maxAttempts = resolveMax(rateLimit, resource);

        // 5. 提取组成元素
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        HttpServletRequest request = getCurrentRequest();
        String ip = IpUtils.getClientIp(request);
        String api = (request != null)
                ? request.getMethod() + ":" + request.getRequestURI()
                : "unknown:unknown";

        // 6. 按维度拼装 key（顺序固定：USER > IP > API > CUSTOM）
        String composedKey = composeKey(rateLimit.dimensions(), userId, ip, api, rateLimit.spelKey(),
                method, joinPoint.getArgs());

        // 7. 调用限流器
        long start = System.currentTimeMillis();
        RateLimitUtil.Result result = rateLimitUtil.checkRateLimit(resource, composedKey, windowSeconds, maxAttempts);
        long elapsed = System.currentTimeMillis() - start;

        // 8. 处理结果：基于 Status + fallback 决策
        RateLimitUtil.Status status = result.status();
        FallbackStrategy fallback = rateLimit.fallback();

        switch (status) {
            case ALLOWED -> {
                log.info("[RATE-LIMIT] PASS | resource={} | key={} | remaining={} | elapsed={}ms",
                        resource, composedKey, result.remaining(), elapsed);
                return joinPoint.proceed();
            }
            case REJECTED -> {
                int retryAfterSec = Math.max(1, (int) ((result.resetMs() - System.currentTimeMillis()) / 1000));
                log.warn("[RATE-LIMIT] REJECT | resource={} | key={} | window={}s | max={} | retryAfter={}s",
                        resource, composedKey, windowSeconds, maxAttempts, retryAfterSec);
                throw new RateLimitException(result.remaining(), result.resetMs(), retryAfterSec);
            }
            case DEGRADED -> {
                if (fallback == FallbackStrategy.FAIL_CLOSE) {
                    // FAIL_CLOSE：Redis 故障期间直接拒绝，避免关键接口被刷穿。
                    // retryAfter 用整个 windowSeconds（故障窗口期内都不可用）
                    log.error("[RATE-LIMIT] DEGRADED-REJECT | resource={} | key={} | window={}s | FAIL_CLOSE 拒绝 | elapsed={}ms",
                            resource, composedKey, windowSeconds, elapsed);
                    throw new RateLimitException(result.remaining(), result.resetMs(), windowSeconds);
                }
                // FAIL_OPEN（默认）：Redis 故障时放行，业务可用性优先
                log.error("[RATE-LIMIT] DEGRADED-OPEN | resource={} | key={} | window={}s | FAIL_OPEN 放行 | elapsed={}ms",
                        resource, composedKey, windowSeconds, elapsed);
                return joinPoint.proceed();
            }
            default -> {
                // 理论上不可达（枚举完整覆盖），兜底放行以避免未知状态导致业务中断
                log.error("[RATE-LIMIT] UNKNOWN-STATUS | resource={} | key={} | status={} | 放行",
                        resource, composedKey, status);
                return joinPoint.proceed();
            }
        }
    }

    /**
     * 判断用户是否为管理员。
     */
    private boolean isadmin(User user) {
        return UserEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 解析 resource：注解为空时自动生成 "全限定类名.方法名"。
     */
    private String resolveResource(RateLimit rateLimit, Method method) {
        String resource = rateLimit.resource();
        if (resource == null || resource.isEmpty()) {
            resource = method.getDeclaringClass().getName() + "." + method.getName();
        }
        return resource;
    }

    /**
     * 合并窗口配置：注解 != -1 用注解，否则查 yml。
     */
    private int resolveWindow(RateLimit rateLimit, String resource) {
        if (rateLimit.windowSeconds() != -1) {
            return rateLimit.windowSeconds();
        }
        return rateLimitProperties.resolveWindow(resource);
    }

    /**
     * 合并最大次数配置：注解 != -1 用注解，否则查 yml。
     */
    private int resolveMax(RateLimit rateLimit, String resource) {
        if (rateLimit.maxAttempts() != -1) {
            return rateLimit.maxAttempts();
        }
        return rateLimitProperties.resolveMax(resource);
    }

    /**
     * 按维度顺序拼装限流 key。
     * <p>固定顺序：USER &gt; IP &gt; API &gt; CUSTOM，用 ":" JOIN。</p>
     * <p>USER_OR_IP 维度：userId != null 取 "u:{userId}"，否则取 "ip:{ip}"。</p>
     */
    private String composeKey(RateLimitDimension[] dimensions, Long userId, String ip, String api,
                              String spelKey, Method method, Object[] args) {
        List<String> parts = new ArrayList<>(4);
        boolean userAdded = false;
        boolean ipAdded = false;
        boolean apiAdded = false;
        boolean customAdded = false;

        for (RateLimitDimension dim : dimensions) {
            switch (dim) {
                case USER:
                    if (!userAdded) {
                        // USER 维度未登录降级为 IP（LoginInterceptor 已保证需登录接口不会到达 AOP）
                        parts.add(userId != null ? "u:" + userId : "ip:" + ip);
                        userAdded = true;
                    }
                    break;
                case IP:
                    if (!ipAdded) {
                        parts.add("ip:" + ip);
                        ipAdded = true;
                    }
                    break;
                case API:
                    if (!apiAdded) {
                        parts.add("api:" + api);
                        apiAdded = true;
                    }
                    break;
                case GLOBAL:
                    parts.add("global");
                    break;
                case CUSTOM:
                    if (!customAdded) {
                        String spelResult = evaluateSpel(spelKey, method, args);
                        if (spelResult != null && !spelResult.isEmpty()) {
                            parts.add("c:" + spelResult);
                        }
                        customAdded = true;
                    }
                    break;
                case USER_OR_IP:
                    // 默认维度：登录用 userId，未登录降级 IP
                    parts.add(userId != null ? "u:" + userId : "ip:" + ip);
                    // USER_OR_IP 可能与 USER/IP 重复，但语义上独立，允许同存
                    break;
                default:
                    break;
            }
        }

        if (parts.isEmpty()) {
            return "default";
        }
        return String.join(":", parts);
    }

    /**
     * 解析 SpEL 表达式。
     * <p>解析失败时降级为「不拼 spelKey」并 log.warn，不抛异常（限流不应因表达式错误导致请求失败）。</p>
     */
    private String evaluateSpel(String spelKey, Method method, Object[] args) {
        if (spelKey == null || spelKey.isEmpty()) {
            return null;
        }
        try {
            Expression expression = spelParser.parseExpression(spelKey);
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Object value = expression.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("[RATE-LIMIT] SpEL 解析失败，降级为不拼 spelKey | spel={} | error={}",
                    spelKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 HTTP 请求。
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
