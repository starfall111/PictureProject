package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.NotificationTypeEnum;
import org.example.common.enums.VipTypeEnum;
import org.example.common.util.VipUtil;
import org.example.pojo.entity.User;
import org.example.server.service.CouponService;
import org.example.server.service.UserService;
import org.example.server.service.event.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * VIP 会员定时任务
 * <p>
 * 包含：
 * 1. VIP 到期处理（每小时）- 将过期用户降级为普通用户
 * 2. VIP 即将到期提醒（每天10点）- 提前3天通知用户续费
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class VipScheduled {

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private CouponService couponService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * VIP 到期处理 — 每小时执行
     * <p>
     * 将 vipExpireTime < now 的用户降级为普通用户，并降级空间
     * </p>
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireVipMembers() {
        String lockKey = RedisKeyConstants.LOCK_VIP_EXPIRE;
        if (!tryLock(lockKey, 3500)) {
            log.info("VIP到期处理：另一个实例正在执行");
            return;
        }

        try {
            Date now = new Date();

            // 查询所有已过期的 VIP 用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getVipType, VipTypeEnum.VIP.getValue())
                    .isNotNull(User::getVipExpireTime)
                    .lt(User::getVipExpireTime, now);

            List<User> expiredUsers = userService.list(wrapper);
            if (expiredUsers.isEmpty()) {
                log.debug("VIP到期处理：无过期用户");
                return;
            }

            log.info("VIP到期处理：发现 {} 个过期用户", expiredUsers.size());

            for (User user : expiredUsers) {
                try {
                    // 降级用户
                    LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(User::getId, user.getId())
                            .set(User::getVipType, VipTypeEnum.NORMAL.getValue());
                    userService.update(updateWrapper);

                    // 降级空间
                    couponService.downgradeSpaceForExpiredVip(user.getId());

                    // 发送 VIP 到期通知
                    NotificationEvent event = new NotificationEvent(
                            this, user.getId(), 0L, "系统", null,
                            NotificationTypeEnum.VIP_EXPIRED, "VIP会员已到期",
                            "您的VIP会员已到期，空间已恢复为普通版。续费即可恢复专业版空间。",
                            null, null
                    );
                    eventPublisher.publishEvent(event);

                    log.info("VIP到期降级完成 | userId={}", user.getId());
                } catch (Exception e) {
                    log.error("VIP到期降级失败 | userId={}", user.getId(), e);
                }
            }

            log.info("VIP到期处理完成，处理 {} 个用户", expiredUsers.size());
        } catch (Exception e) {
            log.error("VIP到期处理任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * VIP 即将到期提醒 — 每天10点执行
     * <p>
     * 提前3天通知用户续费
     * </p>
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void remindVipExpiring() {
        String lockKey = "lock:vip:expire-remind";
        if (!tryLock(lockKey, 7200)) {
            log.info("VIP到期提醒：另一个实例正在执行");
            return;
        }

        try {
            Date now = new Date();
            Date threeDaysLater = new Date(now.getTime() + 3L * 24 * 3600 * 1000);

            // 查询3天内即将到期的 VIP 用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getVipType, VipTypeEnum.VIP.getValue())
                    .isNotNull(User::getVipExpireTime)
                    .between(User::getVipExpireTime, now, threeDaysLater);

            List<User> expiringUsers = userService.list(wrapper);
            if (expiringUsers.isEmpty()) {
                log.debug("VIP到期提醒：无即将到期用户");
                return;
            }

            log.info("VIP到期提醒：发现 {} 个即将到期用户", expiringUsers.size());

            for (User user : expiringUsers) {
                try {
                    // 防止重复提醒（每个用户每天只提醒一次）
                    String remindKey = String.format(RedisKeyConstants.VIP_EXPIRE_REMIND_KEY, user.getId());
                    String today = String.valueOf(System.currentTimeMillis() / (24 * 3600 * 1000));
                    Boolean alreadyReminded = stringRedisTemplate.opsForValue()
                            .setIfAbsent(remindKey, today, 25, TimeUnit.HOURS);
                    if (!Boolean.TRUE.equals(alreadyReminded)) {
                        continue;
                    }

                    // 计算剩余天数
                    long remainingMs = user.getVipExpireTime().getTime() - now.getTime();
                    long remainingDays = remainingMs / (24 * 3600 * 1000);

                    NotificationEvent event = new NotificationEvent(
                            this, user.getId(), 0L, "系统", null,
                            NotificationTypeEnum.VIP_EXPIRING, "VIP会员即将到期",
                            "您的VIP会员将于" + remainingDays + "天后到期，届时空间将恢复为普通版。请及时续费以保留专业版空间。",
                            null, null
                    );
                    eventPublisher.publishEvent(event);

                    log.info("VIP到期提醒发送 | userId={}, remainingDays={}", user.getId(), remainingDays);
                } catch (Exception e) {
                    log.error("VIP到期提醒发送失败 | userId={}", user.getId(), e);
                }
            }

            log.info("VIP到期提醒完成，处理 {} 个用户", expiringUsers.size());
        } catch (Exception e) {
            log.error("VIP到期提醒任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
