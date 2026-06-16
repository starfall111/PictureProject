package org.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要耗时监控的上传操作方法/类
 * <p>
 * AOP 会自动记录上传方法执行耗时，超过 {@link #warnThreshold()} 毫秒时升级为 WARN 日志
 * </p>
 *
 * @author Zou
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UploadTimed {

    /**
     * 业务描述（用于日志标识）
     */
    String value() default "";

    /**
     * 慢调用告警阈值（毫秒），超过此值使用 WARN 级别输出
     */
    long warnThreshold() default 3000;
}
