package org.example.server.service.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.NotificationTypeEnum;
import org.example.pojo.entity.Notification;
import org.example.server.service.NotificationService;
import org.example.server.service.sse.SsePushService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统消息 MQ 消费者 — 批量写入 notification 并推送
 *
 * @author Zou
 */
@Slf4j
@Component
public class SystemMessageConsumer {

    @Resource(name = "dbNotificationService")
    private NotificationService notificationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SsePushService ssePushService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "system.message.queue")
    public void consume(Message<String> message, Channel channel) throws Exception {
        long deliveryTag = 0;
        try {
            MessageHeaders headers = message.getHeaders();
            deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

            String payload = message.getPayload();
            JsonNode root = objectMapper.readTree(payload);

            String title = root.get("title").asText();
            String content = root.get("content").asText();
            JsonNode userIdsNode = root.get("userIds");

            List<Long> userIds = new ArrayList<>();
            for (JsonNode node : userIdsNode) {
                userIds.add(node.asLong());
            }

            // 1. 批量构建 Notification
            List<Notification> notifications = userIds.stream()
                    .map(uid -> buildNotification(uid, title, content))
                    .toList();

            // 2. 批量插入
            notificationService.saveBatch(notifications);

            // 3. 更新 Redis 未读计数 + SSE 推送
            // todo 会产生多次redis网路连接 需要优化，使用pipeline管道批量修改，降低性能消耗
            for (Long userId : userIds) {
                try {
                    String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, userId);
                    Long newCount = stringRedisTemplate.opsForValue().increment(unreadKey);
                    if (newCount != null && newCount == 1L) {
                        stringRedisTemplate.expire(unreadKey, RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);
                    }
                    if (newCount != null) {
                        ssePushService.pushUnreadCount(userId, newCount);
                    }
                } catch (Exception e) {
                    log.warn("MQ消费：更新用户未读计数失败：userId={}", userId, e);
                }
            }

            // 4. 手动 ACK
            if (deliveryTag > 0) {
                channel.basicAck(deliveryTag, false);
            }

            log.info("MQ消费系统消息成功：title={}, 用户数={}", title, userIds.size());
        } catch (Exception e) {
            log.error("MQ消费系统消息失败", e);
            if (deliveryTag > 0) {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private Notification buildNotification(Long receiverId, String title, String content) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(null);
        notification.setSenderName("系统管理员");
        notification.setSenderAvatar(null);
        notification.setType(NotificationTypeEnum.SYSTEM.getType());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setResourceId(null);
        notification.setResourceUrl(null);
        notification.setIsRead(0);
        return notification;
    }
}
