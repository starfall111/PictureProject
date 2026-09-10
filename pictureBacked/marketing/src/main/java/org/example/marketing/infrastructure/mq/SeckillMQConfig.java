package org.example.marketing.infrastructure.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 秒杀抢购队列
 *
 * @author Zou
 */
@Configuration
public class SeckillMQConfig {

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
}
