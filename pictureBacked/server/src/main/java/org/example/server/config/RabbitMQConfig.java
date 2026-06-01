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
}
