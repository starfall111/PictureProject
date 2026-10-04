package org.example.marketing.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 秒杀 Redis Lua 脚本配置 — 库存扣减/回滚
 *
 * @author Zou
 */
@Configuration
public class SeckillRedisConfig {

    @Bean
    public DefaultRedisScript<Long> seckillDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_deduct.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> seckillRollbackScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_rollback.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
