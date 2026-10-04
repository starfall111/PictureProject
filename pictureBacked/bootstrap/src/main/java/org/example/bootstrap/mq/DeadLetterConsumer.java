package org.example.bootstrap.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者 — 仅做日志告警，记录消费失败的消息供人工排查
 *
 * @author Zou
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "system.message.dead.queue")
    public void handleSystemMsgDead(String message) {
        log.error("【死信告警】系统消息消费失败，需人工排查 | payload={}", message);
    }

    @RabbitListener(queues = "batch.picture.dead.queue")
    public void handleBatchPicDead(String message) {
        log.error("【死信告警】批量图片任务消费失败，需人工排查 | payload={}", message);
    }

    @RabbitListener(queues = "seckill.order.dead.queue")
    public void handleSeckillDead(String message) {
        log.error("【死信告警】秒杀订单消费失败，需人工排查 | payload={}", message);
    }

    @RabbitListener(queues = "social.action.dead.queue")
    public void handleSocialMediaDead(String message) {
        log.error("【死信告警】社交操作消费失败，需人工排查 | payload={}", message);
    }
}
