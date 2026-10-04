package org.example.shared.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置 — 仅承载分布式锁（RLock），数据读写仍走 Lettuce StringRedisTemplate，
 * 因此刻意不使用 redisson-spring-boot-starter（避免其替换全局 RedisConnectionFactory）。
 *
 * 连接信息复用 bootstrap application.yml 的 spring.data.redis.*，不重复维护一份。
 *
 * @author Zou
 */
@Configuration
public class RedissonConfig {

    /**
     * 看门狗续期基准：业务未结束每 1/3 周期自动续期，JVM 崩溃后最多 30s 自动释放
     */
    private static final long LOCK_WATCHDOG_TIMEOUT_MS = 30_000;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database) {
        Config config = new Config();
        // 看门狗续期基准（Config 级别配置）：业务未结束每 1/3 周期自动续期，JVM 崩溃后最多 30s 自动释放
        config.setLockWatchdogTimeout(LOCK_WATCHDOG_TIMEOUT_MS);
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(password.isEmpty() ? null : password)
                // 锁场景流量小，刻意给小连接池，避免与 Lettuce 池（max-active=50）叠加浪费
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);
        return Redisson.create(config);
    }
}
