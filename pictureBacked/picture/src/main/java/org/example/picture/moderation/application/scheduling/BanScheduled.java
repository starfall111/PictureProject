package org.example.picture.moderation.application.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.shared.contract.NotificationTypeEnum;
import org.example.shared.util.RedisCacheUtil;
import org.example.identity.api.model.User;
import org.example.picture.moderation.application.BanService;
import org.example.identity.application.UserService;
import org.example.shared.contract.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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
    private RedisCacheUtil redisCacheUtil;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 自动解封过期用户 - 每5分钟
     */
    @Scheduled(fixedRate = 300000)
    public void autoUnbanExpiredUsers() {
        boolean executed = redisCacheUtil.tryExecuteWithLock("lock:ban:auto-unban", () -> {
            try {
                banService.autoUnbanExpiredUsers();
            } catch (Exception e) {
                log.error("自动解封任务执行失败", e);
            }
        });
        if (!executed) {
            log.info("自动解封任务：另一个实例正在执行");
        }
    }

    /**
     * 封禁到期提醒 - 每天凌晨1点
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void sendBanExpiringNotification() {
        boolean executed = redisCacheUtil.tryExecuteWithLock("lock:ban:expiring-notify", () -> {
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
            }
        });
        if (!executed) {
            log.info("封禁到期提醒：另一个实例正在执行");
        }
    }

    /**
     * 重置过期违规计数 - 每天凌晨2点
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void resetViolationCounts() {
        boolean executed = redisCacheUtil.tryExecuteWithLock("lock:ban:reset-violation", () -> {
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
            }
        });
        if (!executed) {
            log.info("违规计数重置：另一个实例正在执行");
        }
    }
}
