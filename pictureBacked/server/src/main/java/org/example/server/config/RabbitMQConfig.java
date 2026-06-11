package org.example.server.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 系统消息发送
 *
 * @author Zou
 */
@Configuration
public class RabbitMQConfig {

    public static final String SYSTEM_MSG_QUEUE = "system.message.queue";
    public static final String SYSTEM_MSG_EXCHANGE = "system.message.exchange";
    public static final String SYSTEM_MSG_ROUTING_KEY = "system.message.routing";

    @Bean
    public Queue systemMsgQueue() {
        return QueueBuilder.durable(SYSTEM_MSG_QUEUE)
                .withArgument("x-dead-letter-exchange", "system.message.dlx")
                .withArgument("x-dead-letter-routing-key", "system.message.dead")
                .build();
    }

    @Bean
    public DirectExchange systemMsgExchange() {
        return new DirectExchange(SYSTEM_MSG_EXCHANGE, true, false);
    }

    @Bean
    public Binding systemMsgBinding() {
        return BindingBuilder.bind(systemMsgQueue()).to(systemMsgExchange()).with(SYSTEM_MSG_ROUTING_KEY);
    }

    // ---- 死信队列（消费失败的消息） ----

    public static final String SYSTEM_MSG_DLQ = "system.message.dead.queue";
    public static final String SYSTEM_MSG_DLX = "system.message.dlx";
    public static final String SYSTEM_MSG_DL_ROUTING_KEY = "system.message.dead";

    @Bean
    public Queue systemMsgDeadQueue() {
        return QueueBuilder.durable(SYSTEM_MSG_DLQ).build();
    }

    @Bean
    public DirectExchange systemMsgDeadExchange() {
        return new DirectExchange(SYSTEM_MSG_DLX, true, false);
    }

    @Bean
    public Binding systemMsgDeadBinding() {
        return BindingBuilder.bind(systemMsgDeadQueue()).to(systemMsgDeadExchange()).with(SYSTEM_MSG_DL_ROUTING_KEY);
    }

    // ---- 批量获取图片任务队列 ----

    public static final String BATCH_PIC_QUEUE = "batch.picture.queue";
    public static final String BATCH_PIC_EXCHANGE = "batch.picture.exchange";
    public static final String BATCH_PIC_ROUTING_KEY = "batch.picture.task";

    public static final String BATCH_PIC_DLQ = "batch.picture.dead.queue";
    public static final String BATCH_PIC_DLX = "batch.picture.dlx";
    public static final String BATCH_PIC_DL_ROUTING_KEY = "batch.picture.dead";

    @Bean
    public Queue batchPicQueue() {
        return QueueBuilder.durable(BATCH_PIC_QUEUE)
                .withArgument("x-dead-letter-exchange", BATCH_PIC_DLX)
                .withArgument("x-dead-letter-routing-key", BATCH_PIC_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange batchPicExchange() {
        return new DirectExchange(BATCH_PIC_EXCHANGE, true, false);
    }

    @Bean
    public Binding batchPicBinding() {
        return BindingBuilder.bind(batchPicQueue()).to(batchPicExchange()).with(BATCH_PIC_ROUTING_KEY);
    }

    @Bean
    public Queue batchPicDeadQueue() {
        return QueueBuilder.durable(BATCH_PIC_DLQ).build();
    }

    @Bean
    public DirectExchange batchPicDeadExchange() {
        return new DirectExchange(BATCH_PIC_DLX, true, false);
    }

    @Bean
    public Binding batchPicDeadBinding() {
        return BindingBuilder.bind(batchPicDeadQueue()).to(batchPicDeadExchange()).with(BATCH_PIC_DL_ROUTING_KEY);
    }

    // ---- 秒杀抢购队列 ----

    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order.task";

    public static final String SECKILL_DLQ = "seckill.order.dead.queue";
    public static final String SECKILL_DLX = "seckill.order.dlx";
    public static final String SECKILL_DL_ROUTING_KEY = "seckill.order.dead";

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX)
                .withArgument("x-dead-letter-routing-key", SECKILL_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public Queue seckillDeadQueue() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public DirectExchange seckillDeadExchange() {
        return new DirectExchange(SECKILL_DLX, true, false);
    }

    @Bean
    public Binding seckillDeadBinding() {
        return BindingBuilder.bind(seckillDeadQueue()).to(seckillDeadExchange()).with(SECKILL_DL_ROUTING_KEY);
    }

    // ---- 社交操作队列（点赞/收藏异步 DB 写入） ----

    public static final String SOCIAL_ACTION_QUEUE = "social.action.queue";
    public static final String SOCIAL_ACTION_EXCHANGE = "social.action.exchange";
    public static final String SOCIAL_ACTION_ROUTING_KEY = "social.action.task";

    public static final String SOCIAL_ACTION_DLQ = "social.action.dead.queue";
    public static final String SOCIAL_ACTION_DLX = "social.action.dlx";
    public static final String SOCIAL_ACTION_DL_ROUTING_KEY = "social.action.dead";

    @Bean
    public Queue socialActionQueue() {
        return QueueBuilder.durable(SOCIAL_ACTION_QUEUE)
                .withArgument("x-dead-letter-exchange", SOCIAL_ACTION_DLX)
                .withArgument("x-dead-letter-routing-key", SOCIAL_ACTION_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange socialActionExchange() {
        return new DirectExchange(SOCIAL_ACTION_EXCHANGE, true, false);
    }

    @Bean
    public Binding socialActionBinding() {
        return BindingBuilder.bind(socialActionQueue()).to(socialActionExchange()).with(SOCIAL_ACTION_ROUTING_KEY);
    }

    @Bean
    public Queue socialActionDeadQueue() {
        return QueueBuilder.durable(SOCIAL_ACTION_DLQ).build();
    }

    @Bean
    public DirectExchange socialActionDeadExchange() {
        return new DirectExchange(SOCIAL_ACTION_DLX, true, false);
    }

    @Bean
    public Binding socialActionDeadBinding() {
        return BindingBuilder.bind(socialActionDeadQueue()).to(socialActionDeadExchange()).with(SOCIAL_ACTION_DL_ROUTING_KEY);
    }
}
