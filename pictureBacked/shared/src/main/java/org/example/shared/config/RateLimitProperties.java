package org.example.shared.config;

import lombok.Data;
import org.example.shared.annotation.FallbackStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 限流配置属性
 *
 * <p>绑定 {@code application.yml} 中 {@code ratelimit} 前缀的配置：</p>
 * <pre>
 * ratelimit:
 *   default-window-seconds: 60      # 全局默认窗口（秒）
 *   default-max-attempts: 100       # 全局默认最大次数
 *   default-fallback: FAIL_OPEN     # 全局默认故障兜底策略
 *   rules:                          # 按 resource 覆盖
 *     - resource: "login"
 *       window-seconds: 60
 *       max-attempts: 10
 * </pre>
 *
 * <h3>三级优先级</h3>
 * <ol>
 *   <li>注解显式指定值（非 -1）</li>
 *   <li>yml 配置（{@link #rules} 按 resource 匹配）</li>
 *   <li>全局默认值（{@link #defaultWindowSeconds} / {@link #defaultMaxAttempts}）</li>
 * </ol>
 *
 * <p><b>热更新</b>：Phase 1 不支持热更新，修改配置需重启服务。</p>
 *
 * @author Zou
 */
@Data
@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /**
     * 全局默认滑动窗口大小（秒）。
     */
    private int defaultWindowSeconds = 60;

    /**
     * 全局默认窗口内最大允许次数。
     */
    private int defaultMaxAttempts = 100;

    /**
     * 全局默认故障兜底策略。
     */
    private FallbackStrategy defaultFallback = FallbackStrategy.FAIL_OPEN;

    /**
     * 按 resource 覆盖的规则列表。
     */
    private List<Rule> rules = new ArrayList<>();

    /**
     * 单条限流规则：针对特定 resource 覆盖窗口和阈值。
     */
    @Data
    public static class Rule {
        /**
         * 资源名（需与注解 {@code resource()} 或自动生成的全限定类名.方法名 一致）。
         */
        private String resource;

        /**
         * 窗口大小（秒）。
         */
        private int windowSeconds;

        /**
         * 最大次数。
         */
        private int maxAttempts;
    }

    /**
     * 解析指定 resource 的窗口大小。
     * <p>查找顺序：{@code rules} 中匹配的资源 → 全局默认值。</p>
     *
     * @param resource 资源名
     * @return 窗口大小（秒），永不为 -1
     */
    public int resolveWindow(String resource) {
        if (resource != null && rules != null) {
            for (Rule rule : rules) {
                if (resource.equals(rule.getResource()) && rule.getWindowSeconds() > 0) {
                    return rule.getWindowSeconds();
                }
            }
        }
        return defaultWindowSeconds;
    }

    /**
     * 解析指定 resource 的最大次数。
     * <p>查找顺序：{@code rules} 中匹配的资源 → 全局默认值。</p>
     *
     * @param resource 资源名
     * @return 最大次数，永不为 -1
     */
    public int resolveMax(String resource) {
        if (resource != null && rules != null) {
            for (Rule rule : rules) {
                if (resource.equals(rule.getResource()) && rule.getMaxAttempts() > 0) {
                    return rule.getMaxAttempts();
                }
            }
        }
        return defaultMaxAttempts;
    }
}
