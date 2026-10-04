package org.example.picture.core.infrastructure.websocket;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量任务 WebSocket 处理器
 * <p>
 * 管理 userId → WebSocketSession 映射，提供按用户推送消息的能力。
 *
 * @author Zou
 */
@Slf4j
@Component
public class BatchWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("WebSocket 关闭无 userId 的连接失败", e);
            }
            return;
        }
        // 踢掉旧连接
        WebSocketSession old = sessions.put(userId, session);
        if (old != null && old.isOpen()) {
            try {
                old.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.debug("关闭旧 WebSocket 连接失败: userId={}", userId);
            }
        }
        log.info("WebSocket 连接建立: userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            sessions.remove(userId, session);
            log.info("WebSocket 连接关闭: userId={}, status={}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = extractUserId(session);
        log.warn("WebSocket 传输错误: userId={}", userId, exception);
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.debug("关闭出错 WebSocket 连接失败: userId={}", userId);
            }
        }
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId  目标用户 ID
     * @param message 消息对象（会被序列化为 JSON）
     */
    public void sendToUser(Long userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket 推送跳过: userId={} 连接不存在或已关闭", userId);
            return;
        }
        try {
            String json = JSONUtil.toJsonStr(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("WebSocket 推送失败: userId={}", userId, e);
        }
    }

    /**
     * 从 session attributes 中提取 userId
     */
    private Long extractUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long l) {
            return l;
        }
        return null;
    }
}
