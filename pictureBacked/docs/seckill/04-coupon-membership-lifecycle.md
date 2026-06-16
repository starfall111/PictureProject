# Phase 4 — 编码券管理与会员生命周期

> 优先级：P0 | 依赖：Phase 2 + Phase 3 | 预估工时：4d

## 目标

实现编码券的完整生命周期管理：我的券包、激活为会员、退款、VIP 过期降级、到期提醒、过期券回收与库存对账。此阶段完成后，秒杀编码券系统从抢购到使用的完整闭环已打通。

---

## 1. VO 层

### 1.1 CouponVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/CouponVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 编码券列表 VO
 */
@Data
public class CouponVO {

    private Long id;

    private String code;

    /**
     * 券类型天数: 3/7/30
     */
    private Integer type;

    /**
     * 类型名称: "3天VIP" / "7天VIP" / "30天VIP"
     */
    private String typeName;

    /**
     * 状态: 0-未发放, 1-已领取未使用, 2-已激活, 3-已过期, 4-已退款
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    private Date issuedAt;

    private Date activatedAt;

    private Date expireAt;

    /**
     * 剩余有效天数（仅 status=1 时有值）
     */
    private Long remainingDays;
}
```

### 1.2 CouponActivateVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/CouponActivateVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 编码券激活结果 VO
 */
@Data
public class CouponActivateVO {

    private Boolean activated;

    private Integer vipType;

    private String vipTypeName;

    private Date vipExpireTime;

    private Integer couponType;

    private String couponTypeName;

    private String message;
}
```

### 1.3 VipStatusVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/VipStatusVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * VIP 会员状态 VO
 */
@Data
public class VipStatusVO {

    private Boolean isVip;

    private Integer vipType;

    private String vipTypeName;

    private Date expireTime;

    /**
     * 剩余天数
     */
    private Long remainingDays;

    private Date activatedAt;

    /**
     * VIP 累计总天数
     */
    private Integer totalDays;
}
```

---

## 2. CouponController

创建 `server/src/main/java/org/example/server/controller/CouponController.java`：

```java
package org.example.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.vo.seckill.CouponActivateVO;
import org.example.pojo.vo.seckill.CouponVO;
import org.example.server.service.CouponService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 编码券管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Resource
    private CouponService couponService;

    /**
     * 我的券包
     * GET /api/coupon/my?page=1&size=10&status=1
     */
    @GetMapping("/my")
    @CheckAuth
    public BaseResponse<Page<CouponVO>> listMyCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.get().getId();
        Page<CouponVO> result = couponService.listMyCouponsVO(userId, status, page, size);
        return ResultUtils.success(result);
    }

    /**
     * 激活编码券
     * POST /api/coupon/activate
     */
    @PostMapping("/activate")
    @CheckAuth
    public BaseResponse<CouponActivateVO> activateCoupon(
            @RequestBody Map<String, Long> body) {
        Long userId = UserContext.get().getId();
        Long couponId = body.get("couponId");
        CouponActivateVO result = (CouponActivateVO) couponService.activateCoupon(userId, couponId);
        return ResultUtils.success(result);
    }
}
```

---

## 3. CouponServiceImpl — 核心实现

创建 `server/src/main/java/org/example/server/service/impl/CouponServiceImpl.java`：

```java
package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.CouponStatusEnum;
import org.example.common.enums.VipTypeEnum;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.VipUtil;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.Space;
import org.example.pojo.entity.User;
import org.example.pojo.vo.seckill.CouponActivateVO;
import org.example.pojo.vo.seckill.CouponVO;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.service.CouponService;
import org.example.server.service.SpaceService;
import org.example.server.service.UserService;
import org.example.server.service.event.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 编码券服务实现
 */
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CodeCouponMapper, CodeCoupon>
        implements CouponService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Value("${seckill.coupon.refund-hours:1}")
    private int couponRefundHours;

    @Override
    public CouponActivateVO activateCoupon(Long userId, Long couponId) {
        // 1. 参数校验
        ThrowUtils.throwIf(couponId == null, ErrorCode.PARAMS_ERROR);

        // 2. 限流：每用户每分钟最多激活 5 张
        String rateLimitKey = String.format(RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_KEY, userId);
        String count = stringRedisTemplate.opsForValue().get(rateLimitKey);
        if (count != null && Integer.parseInt(count) >= RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
        }
        stringRedisTemplate.opsForValue().increment(rateLimitKey);
        stringRedisTemplate.expire(rateLimitKey, RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);

        // 3. 防重锁：同一用户同一券 10 秒内不可重复请求
        String lockKey = String.format(RedisKeyConstants.COUPON_ACTIVATE_LOCK_KEY, userId, couponId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        ThrowUtils.throwIf(Boolean.FALSE.equals(locked), ErrorCode.SYSTEM_ERROR, "操作进行中，请稍后");

        try {
            // 4. 查询编码券
            CodeCoupon coupon = this.getById(couponId);
            ThrowUtils.throwIf(coupon == null, ErrorCode.PARAMS_ERROR, "编码券不存在");

            // 5. 归属校验
            ThrowUtils.throwIf(!userId.equals(coupon.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权操作此编码券");

            // 6. 状态校验：仅已领取未使用(status=1)可激活
            ThrowUtils.throwIf(coupon.getStatus() != 1, ErrorCode.PARAMS_ERROR, "编码券状态异常，无法激活");

            // 7. 过期校验
            ThrowUtils.throwIf(coupon.getExpireAt() != null && coupon.getExpireAt().before(new Date()),
                    ErrorCode.PARAMS_ERROR, "编码券已过期，无法激活");

            // 8. 查询用户当前会员状态
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

            Date currentExpireTime = user.getVipExpireTime();
            Date newExpireTime = VipUtil.calculateNewExpireTime(currentExpireTime, coupon.getType());

            // 9. 事务内执行
            transactionTemplate.executeWithoutResult(status -> {
                // 9a. 更新 user 表会员字段
                user.setVipType(VipTypeEnum.VIP.getValue());
                user.setVipExpireTime(newExpireTime);
                if (user.getVipActivatedAt() == null) {
                    user.setVipActivatedAt(new Date()); // 首次激活
                }
                user.setVipTotalDays(
                        (user.getVipTotalDays() != null ? user.getVipTotalDays() : 0) + coupon.getType()
                );
                boolean userUpdated = userService.updateById(user);
                ThrowUtils.throwIf(!userUpdated, ErrorCode.SYSTEM_ERROR, "激活失败");

                // 9b. 更新 code_coupon 表
                coupon.setStatus(CouponStatusEnum.ACTIVATED.getValue());
                coupon.setActivatedAt(new Date());
                int rows = codeCouponMapper.updateById(coupon); // 乐观锁 version 校验
                ThrowUtils.throwIf(rows == 0, ErrorCode.SYSTEM_ERROR, "激活失败，请重试");
            });

            // 10. 失效用户缓存
            invalidateUserCache(userId);

            // 11. 发送激活成功通知
            eventPublisher.publishEvent(new NotificationEvent(
                    this, userId, 0L, "系统", null,
                    "VIP_ACTIVATED", "VIP激活成功",
                    String.format("激活成功，您已获得%d天VIP会员", coupon.getType()),
                    null, null
            ));

            // 12. 异步升级空间（VIP 自动获得专业版空间）
            try {
                upgradeSpaceForVip(userId);
            } catch (Exception e) {
                log.error("VIP空间升级失败, userId={}", userId, e);
                // 不影响激活结果
            }

            // 13. 构造返回
            CouponActivateVO vo = new CouponActivateVO();
            vo.setActivated(true);
            vo.setVipType(VipTypeEnum.VIP.getValue());
            vo.setVipTypeName(VipTypeEnum.VIP.getText());
            vo.setVipExpireTime(newExpireTime);
            vo.setCouponType(coupon.getType());
            vo.setCouponTypeName(coupon.getType() + "天VIP");
            vo.setMessage(String.format("激活成功，您已获得%d天VIP会员", coupon.getType()));
            return vo;

        } finally {
            // 释放防重锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Page<CodeCoupon> listMyCoupons(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(CodeCoupon::getStatus, status);
        }
        wrapper.orderByDesc(CodeCoupon::getCreateTime);
        return this.page(new Page<>(page, size), wrapper);
    }

    /**
     * 返回 VO 分页（含计算字段）
     */
    public Page<CouponVO> listMyCouponsVO(Long userId, Integer status, int page, int size) {
        Page<CodeCoupon> couponPage = listMyCoupons(userId, status, page, size);

        Page<CouponVO> voPage = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        voPage.setRecords(couponPage.getRecords().stream().map(coupon -> {
            CouponVO vo = new CouponVO();
            vo.setId(coupon.getId());
            vo.setCode(coupon.getCode());
            vo.setType(coupon.getType());
            vo.setTypeName(coupon.getType() + "天VIP");
            vo.setStatus(coupon.getStatus());
            vo.setStatusName(CouponStatusEnum.getEnumByValue(coupon.getStatus()).getText());
            vo.setIssuedAt(coupon.getIssuedAt());
            vo.setActivatedAt(coupon.getActivatedAt());
            vo.setExpireAt(coupon.getExpireAt());

            // 计算剩余天数
            if (coupon.getStatus() == 1 && coupon.getExpireAt() != null) {
                long remainingMs = coupon.getExpireAt().getTime() - System.currentTimeMillis();
                vo.setRemainingDays(Math.max(0, remainingMs / (24 * 3600 * 1000)));
            }
            return vo;
        }).toList());

        return voPage;
    }

    // todo 不实现退款服务，当前功能只用于免费抢购
    @Override
    public void refundCoupon(Long userId, Long couponId) {
        // 1. 参数校验
        ThrowUtils.throwIf(couponId == null, ErrorCode.PARAMS_ERROR);

        // 2. 查询编码券
        CodeCoupon coupon = this.getById(couponId);
        ThrowUtils.throwIf(coupon == null, ErrorCode.PARAMS_ERROR, "编码券不存在");

        // 3. 归属校验
        ThrowUtils.throwIf(!userId.equals(coupon.getUserId()), ErrorCode.NO_AUTH_ERROR);

        // 4. 状态校验：仅已激活(status=2)可退款
        ThrowUtils.throwIf(coupon.getStatus() != 2, ErrorCode.PARAMS_ERROR, "券状态不支持退款");

        // 5. 时间窗口校验
        if (coupon.getActivatedAt() != null) {
            long activatedAt = coupon.getActivatedAt().getTime();
            long now = System.currentTimeMillis();
            long refundWindow = (long) couponRefundHours * 3600 * 1000;
            ThrowUtils.throwIf(now - activatedAt > refundWindow, ErrorCode.PARAMS_ERROR, "已超过退款时限");
        }

        // 6. 查询用户
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        // 7. 计算退款后的到期时间
        Date newExpireTime = new Date(
                user.getVipExpireTime().getTime() - (long) coupon.getType() * 24 * 3600 * 1000
        );

        // 8. 事务内执行
        transactionTemplate.executeWithoutResult(status -> {
            // 8a. 扣减会员时间
            user.setVipExpireTime(newExpireTime);
            user.setVipTotalDays(
                    Math.max(0, (user.getVipTotalDays() != null ? user.getVipTotalDays() : 0) - coupon.getType())
            );
            if (newExpireTime.before(new Date())) {
                user.setVipType(VipTypeEnum.NORMAL.getValue());
            }
            userService.updateById(user);

            // 8b. 更新券状态
            coupon.setStatus(CouponStatusEnum.REFUNDED.getValue());
            codeCouponMapper.updateById(coupon);
        });

        // 9. 失效缓存
        invalidateUserCache(userId);

        // 10. 降级空间（如果会员过期）
        if (newExpireTime.before(new Date())) {
            try {
                downgradeSpaceForExpiredVip(userId);
            } catch (Exception e) {
                log.error("退款后空间降级失败, userId={}", userId, e);
            }
        }

        log.info("编码券退款成功, userId={}, couponId={}", userId, couponId);
    }

    // ---- 内部方法 ----

    @Resource
    private CodeCouponMapper codeCouponMapper;

    /**
     * VIP 空间自动升级
     */
    @Async("asyncExecutor")
    private void upgradeSpaceForVip(Long userId) {
        Space space = spaceService.getOne(
                new LambdaQueryWrapper<Space>().eq(Space::getUserId, userId)
        );

        if (space == null) {
            // 无空间，创建专业版
            Space newSpace = new Space();
            newSpace.setSpaceName("默认空间");
            newSpace.setSpaceLevel(1); // 专业版
            newSpace.setMaxCount(1000L);
            newSpace.setMaxSize(1024L * 1024 * 1024); // 1GB
            newSpace.setTotalSize(0L);
            newSpace.setTotalCount(0L);
            newSpace.setUserId(userId);
            spaceService.save(newSpace);
        } else if (space.getSpaceLevel() < 1) {
            // 升级到专业版
            space.setSpaceLevel(1);
            space.setMaxCount(1000L);
            space.setMaxSize(1024L * 1024 * 1024);
            spaceService.updateById(space);
        }
    }

    /**
     * VIP 过期后空间降级
     * 仅降级专业版空间，旗舰版不动（管理员空间）
     * 仅在当前用量不超过普通版限额时才降级
     */
    @Async("asyncExecutor")
    private void downgradeSpaceForExpiredVip(Long userId) {
        Space space = spaceService.getOne(
                new LambdaQueryWrapper<Space>()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceLevel, 1) // 仅专业版
        );
        if (space != null) {
            // 普通版限额: 100张 / 100MB
            if (space.getTotalCount() <= 100 && space.getTotalSize() <= 100L * 1024 * 1024) {
                space.setSpaceLevel(0);
                space.setMaxCount(100L);
                space.setMaxSize(100L * 1024 * 1024);
                spaceService.updateById(space);
            }
            // 超出限额则保持专业版，但用户无法继续上传
        }
    }

    /**
     * 失效用户缓存
     */
    private void invalidateUserCache(Long userId) {
        try {
            String userInfoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
            stringRedisTemplate.delete(userInfoKey);
            String userProfileKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
            stringRedisTemplate.delete(userProfileKey);
        } catch (Exception e) {
            log.warn("失效用户缓存异常, userId={}", userId, e);
        }
    }
}
```

---

## 4. VIP 状态查询接口

在 `server/src/main/java/org/example/server/controller/UserController.java` 中追加：

```java
/**
 * VIP 会员状态查询
 * GET /api/user/vip/status
 */
@GetMapping("/vip/status")
@CheckAuth
public BaseResponse<VipStatusVO> getVipStatus() {
    Long userId = UserContext.get().getId();
    User user = userService.getById(userId);

    VipStatusVO vo = new VipStatusVO();
    boolean isActiveVip = VipUtil.isActiveVip(user);
    vo.setIsVip(isActiveVip);
    vo.setVipType(user.getVipType());
    vo.setVipTypeName(user.getVipType() != null
            ? VipTypeEnum.getEnumByValue(user.getVipType()).getText() : "普通用户");
    vo.setExpireTime(user.getVipExpireTime());
    vo.setActivatedAt(user.getVipActivatedAt());
    vo.setTotalDays(user.getVipTotalDays());

    if (isActiveVip && user.getVipExpireTime() != null) {
        long remainingMs = user.getVipExpireTime().getTime() - System.currentTimeMillis();
        vo.setRemainingDays(Math.max(0L, remainingMs / (24 * 3600 * 1000)));
    }

    return ResultUtils.success(vo);
}
```

需要追加 import：
```java
import org.example.common.util.VipUtil;
import org.example.common.enums.VipTypeEnum;
import org.example.pojo.vo.seckill.VipStatusVO;
```

---

## 5. VIP 过期定时任务

创建 `server/src/main/java/org/example/server/scheduled/VipScheduled.java`：

```java
package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.VipTypeEnum;
import org.example.pojo.entity.Space;
import org.example.pojo.entity.User;
import org.example.server.service.SpaceService;
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
 * VIP 会员相关定时任务
 */
@Slf4j
@Component
public class VipScheduled {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * VIP 过期处理 — 每小时执行
     * 扫描已过期会员，降级用户状态和空间
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireVipMembers() {
        String lockKey = RedisKeyConstants.LOCK_VIP_EXPIRE;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 300, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
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

            for (User user : expiredUsers) {
                try {
                    handleVipExpiration(user);
                } catch (Exception e) {
                    log.error("处理VIP过期失败, userId={}", user.getId(), e);
                }
            }

            log.info("VIP过期处理完成, 处理 {} 位用户", expiredUsers.size());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * VIP 到期提醒 — 每天上午 10 点
     * 提前 3 天和 1 天发送提醒通知
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void remindVipExpiring() {
        Date now = new Date();
        int[] daysBefore = {3, 1};

        for (int days : daysBefore) {
            Date targetTime = new Date(now.getTime() + (long) days * 24 * 3600 * 1000);
            Date windowStart = new Date(targetTime.getTime() - 12 * 3600 * 1000);

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getVipType, VipTypeEnum.VIP.getValue())
                   .between(User::getVipExpireTime, windowStart, targetTime);

            List<User> expiringUsers = userService.list(wrapper);

            for (User user : expiringUsers) {
                // 防重：今天是否已发过提醒
                String remindKey = String.format(RedisKeyConstants.VIP_EXPIRE_REMIND_KEY, user.getId());
                Boolean sent = stringRedisTemplate.opsForValue()
                        .setIfAbsent(remindKey, "1", 24, TimeUnit.HOURS);
                if (Boolean.FALSE.equals(sent)) {
                    continue;
                }

                long remainingMs = user.getVipExpireTime().getTime() - now.getTime();
                int remainingDays = (int) (remainingMs / (24 * 3600 * 1000));

                eventPublisher.publishEvent(new NotificationEvent(
                        this, user.getId(), 0L, "系统", null,
                        "VIP_EXPIRING", "VIP即将到期",
                        String.format("您的VIP会员将在 %d 天后到期，届时部分功能将受到限制。", remainingDays),
                        null, null
                ));
            }
        }

        log.info("VIP到期提醒发送完成");
    }

    private void handleVipExpiration(User user) {
        // 1. 降级 VIP 状态
        user.setVipType(VipTypeEnum.NORMAL.getValue());
        boolean updated = userService.updateById(user);

        if (updated) {
            // 2. 失效缓存
            invalidateUserCache(user.getId());

            // 3. 降级空间
            downgradeSpaceForExpiredVip(user.getId());

            // 4. 发送到期通知
            eventPublisher.publishEvent(new NotificationEvent(
                    this, user.getId(), 0L, "系统", null,
                    "VIP_EXPIRED", "VIP会员已到期",
                    "您的VIP会员已到期，部分功能将受到限制。获取新的编码券可继续享受VIP权益。",
                    null, null
            ));

            log.info("VIP过期处理完成, userId={}", user.getId());
        }
    }

    private void downgradeSpaceForExpiredVip(Long userId) {
        Space space = spaceService.getOne(
                new LambdaQueryWrapper<Space>()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceLevel, 1) // 仅专业版
        );
        if (space != null) {
            if (space.getTotalCount() <= 100 && space.getTotalSize() <= 100L * 1024 * 1024) {
                space.setSpaceLevel(0);
                space.setMaxCount(100L);
                space.setMaxSize(100L * 1024 * 1024);
                spaceService.updateById(space);
            }
        }
    }

    private void invalidateUserCache(Long userId) {
        try {
            String userInfoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
            stringRedisTemplate.delete(userInfoKey);
            String userProfileKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
            stringRedisTemplate.delete(userProfileKey);
        } catch (Exception e) {
            log.warn("失效用户缓存异常, userId={}", userId, e);
        }
    }
}
```

---

## 6. 券过期回收 + 库存对账定时任务

创建 `server/src/main/java/org/example/server/scheduled/CouponExpireScheduled.java`：

```java
package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.CouponStatusEnum;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.CodeCouponMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 编码券过期回收 + Redis-DB 库存对账
 */
@Slf4j
@Component
public class CouponExpireScheduled {

    @Resource
    private CodeCouponMapper codeCouponMapper;

    @Resource
    private CodeCouponBatchMapper batchMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "seckillRollbackScript")
    private DefaultRedisScript<Long> seckillRollbackScript;

    /**
     * 过期券回收 — 每小时执行
     * 扫描已领取但未使用且已过期的券，更新状态并归还库存
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireCoupons() {
        String lockKey = "lock:coupon:expire";
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 300, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return;
        }

        try {
            Date now = new Date();

            // 扫描过期券
            LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CodeCoupon::getStatus, CouponStatusEnum.CLAIMED.getValue())
                   .lt(CodeCoupon::getExpireAt, now);

            List<CodeCoupon> expiredCoupons = codeCouponMapper.selectList(wrapper);

            for (CodeCoupon coupon : expiredCoupons) {
                try {
                    // 更新状态
                    coupon.setStatus(CouponStatusEnum.EXPIRED.getValue());
                    codeCouponMapper.updateById(coupon);

                    // 使用 Lua 脚本归还库存（原子性）
                    String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, coupon.getBatchId());
                    stringRedisTemplate.execute(
                            seckillRollbackScript,
                            java.util.Collections.singletonList(stockKey),
                            "1"
                    );

                    // 清理防重标记
                    String dedupeKey = String.format(RedisKeyConstants.SECKILL_DEDUPE,
                            coupon.getUserId(), coupon.getBatchId());
                    stringRedisTemplate.delete(dedupeKey);

                } catch (Exception e) {
                    log.error("过期券处理失败, couponId={}", coupon.getId(), e);
                }
            }

            log.info("过期券回收完成, 处理 {} 张券", expiredCoupons.size());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * Redis-DB 库存对账 — 每 10 分钟执行
     * 比对 Redis 库存与 DB 实际可用券数量，超阈值告警并修正
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileStock() {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.LOCK_SECKILL_RECONCILE, "1", 600, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return;
        }

        try {
            // 查询所有进行中的批次
            LambdaQueryWrapper<CodeCouponBatch> batchWrapper = new LambdaQueryWrapper<>();
            batchWrapper.in(CodeCouponBatch::getStatus, Arrays.asList(1, 2));
            List<CodeCouponBatch> activeBatches = batchMapper.selectList(batchWrapper);

            for (CodeCouponBatch batch : activeBatches) {
                try {
                    // DB 实际可用库存
                    Long dbStock = codeCouponMapper.selectCount(
                            new LambdaQueryWrapper<CodeCoupon>()
                                    .eq(CodeCoupon::getBatchId, batch.getId())
                                    .eq(CodeCoupon::getStatus, CouponStatusEnum.UNSOLD.getValue())
                    );

                    // Redis 库存
                    String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batch.getId());
                    Object redisStockObj = stringRedisTemplate.opsForHash().get(stockKey, "stock");

                    if (redisStockObj == null) {
                        continue; // Redis 未预热，跳过
                    }

                    long redisStock = Long.parseLong(redisStockObj.toString());
                    long diff = redisStock - dbStock;

                    if (Math.abs(diff) > 5) {
                        log.error("库存对账异常! batchId={}, Redis={}, DB={}, diff={}",
                                batch.getId(), redisStock, dbStock, diff);
                        // 以 DB 为准修正 Redis
                        stringRedisTemplate.opsForHash().put(stockKey, "stock", String.valueOf(dbStock));
                    }
                } catch (Exception e) {
                    log.error("对账异常, batchId={}", batch.getId(), e);
                }
            }

            log.info("库存对账完成, 检查 {} 个批次", activeBatches.size());
        } finally {
            stringRedisTemplate.delete(RedisKeyConstants.LOCK_SECKILL_RECONCILE);
        }
    }
}
```

---

## 验证清单

- [ ] `GET /api/coupon/my` 返回当前用户的券列表，含状态名称和剩余天数
- [ ] `POST /api/coupon/activate` 成功激活券，User 表 VIP 字段更新
- [ ] 激活后用户空间自动升级为专业版
- [ ] 多张券叠加激活，到期时间正确累加
- [ ] 激活已过期/非本人/已使用的券被拒绝
- [ ] 激活防重：同一券 10 秒内不可重复激活
- [ ] 激活限流：每分钟超过 5 次被拒绝
- [ ] `GET /api/user/vip/status` 返回正确的 VIP 状态和剩余天数
- [ ] VIP 过期定时任务正确降级用户状态和空间
- [ ] VIP 到期前 3 天和 1 天收到提醒通知
- [ ] 过期券回收定时任务更新券状态并归还 Redis 库存
- [ ] 库存归还使用 Lua 脚本保证原子性
- [ ] Redis-DB 对账任务检测到差异超过 5 时修正 Redis
- [ ] 封禁用户的券/会员过期计时不暂停（按预期行为）
