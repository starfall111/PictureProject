package org.example.server.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 秒杀降级服务
 * <p>
 * 降级等级:
 * <ul>
 *   <li>0: 正常</li>
 *   <li>1: Redis 限流降级（跳过限流检查）</li>
 *   <li>2: MQ 降级（不发 MQ，同步写 DB）</li>
 *   <li>3: 只读降级（禁止抢购，可查看）</li>
 *   <li>4: 全部降级（秒杀模块不可用）</li>
 * </ul>
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Service
public class SeckillDegradationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取当前降级等级
     *
     * @return 降级等级 0-4
     */
    public int getDegradationLevel() {
        try {
            String levelStr = stringRedisTemplate.opsForValue().get(RedisKeyConstants.SECKILL_DEGRADE);
            if (levelStr != null) {
                return Integer.parseInt(levelStr);
            }
        } catch (Exception e) {
            log.warn("读取降级等级异常，默认正常模式", e);
        }
        return 0;
    }

    /**
     * 设置降级等级
     *
     * @param level 降级等级 0-4
     */
    public void setDegradationLevel(int level) {
        if (level < 0 || level > 4) {
            throw new IllegalArgumentException("降级等级必须在 0-4 之间");
        }
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.SECKILL_DEGRADE, String.valueOf(level));
        log.warn("秒杀降级等级已调整 | level={}", level);
    }

    /**
     * 是否允许抢购操作
     *
     * @return true=允许
     */
    public boolean isGrabAllowed() {
        return getDegradationLevel() < 3;
    }

    /**
     * 是否允许发送 MQ
     *
     * @return true=允许
     */
    public boolean isMqAllowed() {
        return getDegradationLevel() < 2;
    }

    /**
     * 是否允许限流检查
     *
     * @return true=允许
     */
    public boolean isRateLimitAllowed() {
        return getDegradationLevel() < 1;
    }
}
