package org.example.notification.application.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.shared.contract.NotificationTypeEnum;
import org.example.shared.util.RedisCacheUtil;
import org.example.notification.domain.model.Notification;
import org.example.notification.application.NotificationService;
import org.example.notification.infrastructure.sse.SsePushService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Resource
    private RedisCacheUtil redisCacheUtil;

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

            // 3. Pipeline 批量更新 Redis 未读计数 + 逐个 SSE 推送
            Map<Long, Long> unreadCounts = redisCacheUtil.pipelineIncrementUnread(userIds);
            for (Map.Entry<Long, Long> entry : unreadCounts.entrySet()) {
                try {
                    ssePushService.pushUnreadCount(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    log.warn("MQ消费：SSE推送失败：userId={}", entry.getKey(), e);
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
