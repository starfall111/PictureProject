package org.example.server.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 推送服务 — 管理用户 SSE 连接并推送消息
 *
 * @author Zou
 */
@Slf4j
@Service
public class SsePushService {

    /**
     * 用户 SSE 连接映射
     */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 连接超时时间（5 分钟）
     */
    private static final long SSE_TIMEOUT = 300_000L;

    /**
     * 创建 SSE 连接
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // 移除旧连接
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成：userId={}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE 连接超时：userId={}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onError(e -> {
            log.debug("SSE 连接错误：userId={}, error={}", userId, e.getMessage());
            emitters.remove(userId, emitter);
        });

        emitters.put(userId, emitter);
        log.debug("SSE 连接建立：userId={}", userId);
        return emitter;
    }

    /**
     * 推送未读数给指定用户
     *
     * @param userId 用户ID
     * @param count  未读数
     */
    public void pushUnreadCount(Long userId, Long count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("unread")
                    .data(count));
        } catch (IOException e) {
            log.debug("SSE 推送失败，移除连接：userId={}", userId);
            emitters.remove(userId, emitter);
        }
    }

    /**
     * 移除用户 SSE 连接
     *
     * @param userId 用户ID
     */
    public void removeEmitter(Long userId) {
        SseEmitter old = emitters.remove(userId);
        if (old != null) {
            old.complete();
        }
    }
}
