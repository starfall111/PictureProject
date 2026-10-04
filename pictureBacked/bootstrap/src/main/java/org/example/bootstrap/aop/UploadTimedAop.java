package org.example.bootstrap.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.shared.annotation.UploadTimed;
import org.springframework.stereotype.Component;

/**
 * URL 上传耗时监控切面
 * <p>
 * 拦截标注 {@link UploadTimed} 的类/方法，记录执行耗时。
 * 正常调用使用 INFO 级别，超过阈值自动升级为 WARN。
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Aspect
@Component
public class UploadTimedAop {

    @Around("@annotation(uploadTimed)")
    public Object timed(ProceedingJoinPoint joinPoint, UploadTimed uploadTimed) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String tag = uploadTimed.value().isEmpty() ? methodName : uploadTimed.value();
        long warnThreshold = uploadTimed.warnThreshold();

        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String logMsg = "[UPLOAD-TIMED] {} | method={} | elapsed={}ms";

            if (elapsed >= warnThreshold) {
                log.warn(logMsg, tag, methodName, elapsed);
            } else {
                log.info(logMsg, tag, methodName, elapsed);
            }
        }
    }
}
