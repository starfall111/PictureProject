package org.example.server.service.event.listener;

import cn.hutool.core.util.ObjUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.pojo.entity.Notification;
import org.example.server.service.NotificationService;
import org.example.server.service.event.NotificationEvent;
import org.example.server.service.sse.SsePushService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 通知事件监听器 — 异步处理通知创建和推送
 *
 * @author Zou
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Resource(name = "dbNotificationService")
    private NotificationService notificationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SsePushService ssePushService;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            // 1. 不给自己发通知
            if (ObjUtil.equal(event.getReceiverId(), event.getSenderId())) {
                return;
            }

            // 2. 保存通知到数据库
            Notification notification = new Notification();
            notification.setReceiverId(event.getReceiverId());
            notification.setSenderId(event.getSenderId());
            notification.setSenderName(event.getSenderName());
            notification.setSenderAvatar(event.getSenderAvatar());
            notification.setType(event.getType().getType());
            notification.setTitle(event.getTitle());
            notification.setContent(event.getContent());
            notification.setResourceId(event.getResourceId());
            notification.setResourceUrl(event.getResourceUrl());
            notification.setIsRead(0);
            notificationService.save(notification);

            // 3. Redis 未读计数 +1
            String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, event.getReceiverId());
            Long newCount = stringRedisTemplate.opsForValue().increment(unreadKey);
            if (newCount != null && newCount == 1L) {
                // 首次创建 key，设置 TTL
                stringRedisTemplate.expire(unreadKey, RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);
            }

            // 4. SSE 推送未读数
            if (newCount != null) {
                ssePushService.pushUnreadCount(event.getReceiverId(), newCount);
            }

            log.info("通知已创建：receiverId={}, type={}, senderId={}",
                    event.getReceiverId(), event.getType(), event.getSenderId());
        } catch (Exception e) {
            log.error("处理通知事件失败：receiverId={}, type={}", event.getReceiverId(), event.getType(), e);
        }
    }
}
