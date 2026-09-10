package org.example.picture.core.infrastructure.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 批量获取图片任务队列
 *
 * @author Zou
 */
@Configuration
public class BatchPictureMQConfig {

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
}
