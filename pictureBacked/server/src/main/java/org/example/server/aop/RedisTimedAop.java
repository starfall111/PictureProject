package org.example.server.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.RedisTimed;
import org.springframework.stereotype.Component;

/**
 * Redis 操作耗时监控切面
 * <p>
 * 拦截标注 {@link RedisTimed} 的方法，记录执行耗时。
 * 正常调用使用 DEBUG 级别，超过阈值自动升级为 WARN。
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Aspect
@Component
public class RedisTimedAop {

    @Around("@annotation(redisTimed)")
    public Object timed(ProceedingJoinPoint joinPoint, RedisTimed redisTimed) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String tag = redisTimed.value().isEmpty() ? methodName : redisTimed.value();
        long warnThreshold = redisTimed.warnThreshold();

        Object[] args = joinPoint.getArgs();
        StringBuilder argsStr = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                argsStr.append(", ");
            }
            argsStr.append(args[i] != null ? args[i].toString() : "null");
        }

        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String logMsg = "[REDIS-TIMED] {} | args=[{}] | elapsed={}ms";

            if (elapsed >= warnThreshold) {
                log.warn(logMsg, tag, argsStr, elapsed);
            } else {
                log.debug(logMsg, tag, argsStr, elapsed);
            }
        }
    }
}
