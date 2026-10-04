package org.example.shared.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取工具
 *
 * <p>按标准代理链路解析客户端真实 IP：</p>
 * <pre>
 * X-Forwarded-For（取第一个非 unknown 值）
 *      ↓
 * X-Real-IP
 *      ↓
 * Proxy-Client-IP
 *      ↓
 * WL-Proxy-Client-IP
 *      ↓
 * request.getRemoteAddr()（最终兜底）
 * </pre>
 *
 * <p><b>防伪造提示</b>：Phase 1 按标准链路解析，未校验可信代理 IP。
 * 攻击者可通过伪造 X-Forwarded-For 绕过 IP 维度限流。
 * Phase 3 将引入可信代理 IP 白名单，仅当 RemoteAddr 在可信列表中时才信任 X-Forwarded-For。</p>
 *
 * @author Zou
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 未知 IP 占位符。
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 从请求头中按优先级提取客户端真实 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 字符串；若所有来源都为空则返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        // 1. X-Forwarded-For：标准代理头，可能含多个值（client, proxy1, proxy2），取第一个非 unknown
        String ip = parseFirstValid(request.getHeader("X-Forwarded-For"));

        // 2. X-Real-IP：Nginx 直连时常用
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        // 3. Proxy-Client-IP：Apache HTTP Server
        if (isInvalid(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        // 4. WL-Proxy-Client-IP：WebLogic
        if (isInvalid(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        // 5. RemoteAddr：Servlet 容器直连地址（最终兜底）
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多级代理逗号分隔场景，取第一个非 unknown 值
        ip = parseFirstValid(ip);

        return isInvalid(ip) ? UNKNOWN : ip.trim();
    }

    /**
     * 解析逗号分隔的 IP 列表，取第一个非 unknown 的有效值。
     */
    private static String parseFirstValid(String raw) {
        if (raw == null || raw.isEmpty() || UNKNOWN.equalsIgnoreCase(raw)) {
            return null;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !UNKNOWN.equalsIgnoreCase(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * 判断 IP 是否无效（null、空串、"unknown"）。
     */
    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
