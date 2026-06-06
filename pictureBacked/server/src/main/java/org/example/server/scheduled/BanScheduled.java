package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.NotificationTypeEnum;
import org.example.pojo.entity.User;
import org.example.server.service.BanService;
import org.example.server.service.UserService;
import org.example.server.service.event.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 封禁相关定时任务
 *
 * @author Zou
 */
@Slf4j
@Component
public class BanScheduled {

    @Resource
    private BanService banService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 自动解封过期用户 - 每5分钟
     */
    @Scheduled(fixedRate = 300000)
    public void autoUnbanExpiredUsers() {
        String lockKey = "lock:ban:auto-unban";
        if (!tryLock(lockKey, 240)) {
            log.info("自动解封任务：另一个实例正在执行");
            return;
        }
        try {
            banService.autoUnbanExpiredUsers();
        } catch (Exception e) {
            log.error("自动解封任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 封禁到期提醒 - 每天凌晨1点
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void sendBanExpiringNotification() {
        String lockKey = "lock:ban:expiring-notify";
        if (!tryLock(lockKey, 1800)) {
            log.info("封禁到期提醒：另一个实例正在执行");
            return;
        }
        try {
            Date now = new Date();
            Date tomorrow = new Date(now.getTime() + 24 * 3600 * 1000L);

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getBanStatus, "TEMP")
                    .isNotNull(User::getBanEndTime)
                    .between(User::getBanEndTime, now, tomorrow);

            List<User> expiringUsers = userService.list(wrapper);
            for (User user : expiringUsers) {
                try {
                    NotificationEvent event = new NotificationEvent(
                            this, user.getId(), 0L, "系统", null,
                            NotificationTypeEnum.BAN_EXPIRING, "封禁即将到期",
                            "您的账号封禁将于 " + user.getBanEndTime() + " 到期，届时可正常登录使用。",
                            null, null
                    );
                    eventPublisher.publishEvent(event);
                } catch (Exception e) {
                    log.warn("发送封禁到期通知失败，userId={}", user.getId(), e);
                }
            }
            log.info("封禁到期提醒完成，发送 {} 条通知", expiringUsers.size());
        } catch (Exception e) {
            log.error("封禁到期提醒任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 重置过期违规计数 - 每天凌晨2点
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void resetViolationCounts() {
        String lockKey = "lock:ban:reset-violation";
        if (!tryLock(lockKey, 1800)) {
            log.info("违规计数重置：另一个实例正在执行");
            return;
        }
        try {
            Date sixMonthsAgo = Date.from(LocalDate.now().minusMonths(6).atStartOfDay(ZoneId.systemDefault()).toInstant());

            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.lt(User::getLastViolationTime, sixMonthsAgo)
                    .gt(User::getViolationCount, 0)
                    .set(User::getViolationCount, 0)
                    .set(User::getLastViolationTime, null);

            boolean result = userService.update(wrapper);
            log.info("违规计数重置完成，result={}", result);
        } catch (Exception e) {
            log.error("违规计数重置任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
