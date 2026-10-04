package org.example.picture.social.infrastructure.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 社交操作队列（点赞/收藏异步 DB 写入）
 *
 * @author Zou
 */
@Configuration
public class SocialActionMQConfig {

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
