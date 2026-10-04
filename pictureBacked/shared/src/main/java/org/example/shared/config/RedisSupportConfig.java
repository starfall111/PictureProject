package org.example.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis 基础设施配置 — 模板与通用 Lua 脚本（限流）
 * 分布式锁已改由 Redisson RLock 承载（见 {@link RedissonConfig}），原 release_lock.lua 已退役
 *
 * @author Zou
 */
@Configuration
public class RedisSupportConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate_limit_sliding_window.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
