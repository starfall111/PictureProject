package org.example.common.annotation;

/**
 * 限流维度枚举
 *
 * <p>定义限流 key 的构成方式，支持多维度组合（在 {@link RateLimit#dimensions()} 中声明）。
 * 多维度拼装时按固定顺序 JOIN：USER &gt; IP &gt; API &gt; CUSTOM。</p>
 *
 * @author Zou
 */
public enum RateLimitDimension {
    /**
     * 按登录用户 userId 限流。
     * 未登录场景降级为 IP（不跳过限流，因为 LoginInterceptor 已保证需要登录的接口不会到达 AOP）。
     */
    USER,

    /**
     * 按客户端 IP 限流。适用于公开接口、登录/注册。
     */
    IP,

    /**
     * 按接口路径限流（全局限流）。
     * key 组成：method + ":" + uri。
     */
    API,

    /**
     * 全局限流，所有请求共享一个窗口。
     */
    GLOBAL,

    /**
     * 按 SpEL 表达式解析的 key 限流。
     * 表达式在 {@link RateLimit#spelKey()} 中声明。
     */
    CUSTOM,

    /**
     * 默认维度：登录用户用 userId，未登录降级为 IP。
     * key 形如 "u:123" 或 "ip:1.2.3.4"。
     */
    USER_OR_IP
}
