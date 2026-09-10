package org.example.marketing.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.marketing.domain.enums.CouponStatusEnum;
import org.example.shared.contract.NotificationTypeEnum;
import org.example.picture.space.domain.enums.SpaceLevelEnum;
import org.example.marketing.domain.enums.VipTypeEnum;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.api.VipUtil;
import org.example.marketing.domain.model.CodeCoupon;
import org.example.picture.space.domain.model.Space;
import org.example.identity.api.model.User;
import org.example.marketing.interfaces.vo.CouponActivateVO;
import org.example.marketing.interfaces.vo.CouponVO;
import org.example.marketing.infrastructure.persistence.CodeCouponBatchMapper;
import org.example.marketing.infrastructure.persistence.CodeCouponMapper;
import org.example.marketing.application.CouponService;
import org.example.picture.space.application.SpaceService;
import org.example.identity.application.UserService;
import org.example.shared.contract.NotificationEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 编码券服务实现
 * <p>
 * 核心功能：激活编码券、查询我的券、退款券
 * 激活流程：限流检查 -> 分布式锁 -> 事务内更新券状态+用户VIP信息+空间升级 -> 发送通知
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CodeCouponMapper, CodeCoupon>
        implements CouponService {

    @Resource
    private CodeCouponMapper codeCouponMapper;

    @Resource
    private CodeCouponBatchMapper codeCouponBatchMapper;

    @Resource
    @Qualifier("dbUserService")
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private DefaultRedisScript<Long> seckillRollbackScript;

    @Value("${seckill.coupon.refund-hours:1}")
    private int refundHours;

    // ==================== 激活编码券 ====================

    @Override
    public CouponActivateVO activateCoupon(Long userId, Long couponId) {
        ThrowUtils.throwIf(userId == null || couponId == null, ErrorCode.PARAMS_ERROR);

        // 1. 限流检查
        checkRateLimit(userId);

        // 2. 分布式锁防止重复激活
        String lockKey = String.format(RedisKeyConstants.COUPON_ACTIVATE_LOCK_KEY, userId, couponId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        try {
            return doActivateCoupon(userId, couponId);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 执行激活逻辑
     */
    private CouponActivateVO doActivateCoupon(Long userId, Long couponId) {
        // 3. 查询券信息并校验
        CodeCoupon coupon = codeCouponMapper.selectById(couponId);
        ThrowUtils.throwIf(coupon == null, ErrorCode.PARAMS_ERROR, "编码券不存在");
        ThrowUtils.throwIf(!userId.equals(coupon.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权操作此券");

        CouponStatusEnum statusEnum = CouponStatusEnum.getEnumByValue(coupon.getStatus());
        ThrowUtils.throwIf(statusEnum != CouponStatusEnum.CLAIMED,
                ErrorCode.OPERATION_ERROR, "券状态不是已领取，无法激活：" + (statusEnum != null ? statusEnum.getText() : "未知"));

        // 4. 编程式事务
        CouponActivateVO result = transactionTemplate.execute(status -> {
            try {
                return doActivateInTransaction(userId, coupon);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        // 5. 发送 VIP 激活成功通知（事务外）
        try {
            Integer couponType = coupon.getType();
            String couponTypeName = couponType + "天VIP";
            NotificationEvent event = new NotificationEvent(
                    this, userId, 0L, "系统", null,
                    NotificationTypeEnum.VIP_ACTIVATED, "VIP会员激活成功",
                    "您已成功激活" + couponTypeName + "会员，感谢您的支持！",
                    couponId, null
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("发送VIP激活通知失败 | userId={}, couponId={}", userId, couponId, e);
        }

        // 6. 失效用户缓存
        invalidateUserCache(userId);

        return result;
    }

    /**
     * 事务内激活操作：更新券状态 + 更新用户VIP信息 + 升级空间
     */
    private CouponActivateVO doActivateInTransaction(Long userId, CodeCoupon coupon) {
        Date now = new Date();
        int couponTypeDays = coupon.getType() != null ? coupon.getType() : 0;

        // 4a. 更新券状态为已激活
        LambdaUpdateWrapper<CodeCoupon> couponUpdate = new LambdaUpdateWrapper<>();
        couponUpdate.eq(CodeCoupon::getId, coupon.getId())
                .eq(CodeCoupon::getVersion, coupon.getVersion())
                .set(CodeCoupon::getStatus, CouponStatusEnum.ACTIVATED.getValue())
                .set(CodeCoupon::getActivatedAt, now)
                .set(CodeCoupon::getExpireAt, new Date(now.getTime() + (long) couponTypeDays * 24 * 3600 * 1000));
        int rows = codeCouponMapper.update(null, couponUpdate);
        ThrowUtils.throwIf(rows == 0, ErrorCode.OPERATION_ERROR, "券状态更新失败，请重试");

        // 4b. 更新用户VIP信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");

        Date newExpireTime = VipUtil.calculateNewExpireTime(user.getVipExpireTime(), couponTypeDays);
        int newTotalDays = (user.getVipTotalDays() != null ? user.getVipTotalDays() : 0) + couponTypeDays;

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setVipType(VipTypeEnum.VIP.getValue());
        updateUser.setVipExpireTime(newExpireTime);
        updateUser.setVipActivatedAt(now);
        updateUser.setVipTotalDays(newTotalDays);
        boolean userUpdated = userService.updateById(updateUser);
        ThrowUtils.throwIf(!userUpdated, ErrorCode.SYSTEM_ERROR, "用户VIP信息更新失败");

        // 4c. 升级空间（如果用户有空间且当前为普通版）
        upgradeSpaceForVip(userId);

        // 构造返回 VO
        CouponActivateVO vo = new CouponActivateVO();
        vo.setActivated(true);
        vo.setVipType(VipTypeEnum.VIP.getValue());
        vo.setVipTypeName(VipTypeEnum.VIP.getText());
        vo.setVipExpireTime(newExpireTime);
        vo.setCouponType(coupon.getType());
        vo.setCouponTypeName(couponTypeDays + "天VIP");
        vo.setMessage("VIP会员激活成功，有效期至" + newExpireTime);
        return vo;
    }

    // ==================== 限流 ====================

    /**
     * 编码券激活限流检查
     */
    private void checkRateLimit(Long userId) {
        String rateLimitKey = String.format(RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_KEY, userId);
        String countStr = stringRedisTemplate.opsForValue().get(rateLimitKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 计数 +1
        Long newCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (newCount != null && newCount == 1L) {
            stringRedisTemplate.expire(rateLimitKey,
                    RedisKeyConstants.COUPON_ACTIVATE_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
    }

    // ==================== 查询我的券 ====================

    @Override
    public Page<CouponVO> listMyCouponsVO(Long userId, Integer status, int page, int size) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(CodeCoupon::getStatus, status);
        }
        wrapper.orderByDesc(CodeCoupon::getCreateTime);

        Page<CodeCoupon> entityPage = codeCouponMapper.selectPage(
                new Page<>(page, size), wrapper);

        // 转换为 VO
        Page<CouponVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CouponVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public List<CodeCoupon> listMyCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodeCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(CodeCoupon::getStatus, status);
        }
        wrapper.orderByDesc(CodeCoupon::getCreateTime);
        return codeCouponMapper.selectList(wrapper);
    }

    // ==================== 退款 ====================

    @Override
    public void refundCoupon(Long couponId, Long operatorId) {
        ThrowUtils.throwIf(couponId == null || operatorId == null, ErrorCode.PARAMS_ERROR);

        CodeCoupon coupon = codeCouponMapper.selectById(couponId);
        ThrowUtils.throwIf(coupon == null, ErrorCode.PARAMS_ERROR, "编码券不存在");
        ThrowUtils.throwIf(coupon.getStatus() != CouponStatusEnum.ACTIVATED.getValue(),
                ErrorCode.OPERATION_ERROR, "只有已激活的券才能退款");

        // 检查退款时间窗口
        if (coupon.getActivatedAt() != null) {
            long activatedMs = coupon.getActivatedAt().getTime();
            long refundWindowMs = (long) refundHours * 3600 * 1000;
            ThrowUtils.throwIf(System.currentTimeMillis() - activatedMs > refundWindowMs,
                    ErrorCode.OPERATION_ERROR, "已超过" + refundHours + "小时退款窗口");
        }

        // 事务内执行退款
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 回退券状态为已领取
                LambdaUpdateWrapper<CodeCoupon> couponUpdate = new LambdaUpdateWrapper<>();
                couponUpdate.eq(CodeCoupon::getId, couponId)
                        .set(CodeCoupon::getStatus, CouponStatusEnum.CLAIMED.getValue())
                        .set(CodeCoupon::getActivatedAt, null)
                        .set(CodeCoupon::getExpireAt, null);
                int rows = codeCouponMapper.update(null, couponUpdate);
                ThrowUtils.throwIf(rows == 0, ErrorCode.OPERATION_ERROR, "券状态回退失败");

                // 扣减用户VIP天数
                int couponDays = coupon.getType() != null ? coupon.getType() : 0;
                User user = userService.getById(coupon.getUserId());
                if (user != null && user.getVipTotalDays() != null) {
                    int remainingDays = Math.max(0, user.getVipTotalDays() - couponDays);
                    User updateUser = new User();
                    updateUser.setId(user.getId());
                    updateUser.setVipTotalDays(remainingDays);

                    // 如果扣减后天数为0，降级为普通用户
                    if (remainingDays == 0) {
                        updateUser.setVipType(VipTypeEnum.NORMAL.getValue());
                        updateUser.setVipExpireTime(null);
                    }

                    boolean updated = userService.updateById(updateUser);
                    ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "用户VIP信息回退失败");
                }

                log.info("编码券退款成功 | couponId={}, operatorId={}", couponId, operatorId);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        // 事务外回滚 Redis 库存
        rollbackRedisStock(coupon.getBatchId());

        // 失效用户缓存
        if (coupon.getUserId() != null) {
            invalidateUserCache(coupon.getUserId());
        }
    }

    // ==================== 空间升级/降级 ====================

    /**
     * 升级用户空间为专业版（VIP 特权）
     */
    public void upgradeSpaceForVip(Long userId) {
        try {
            LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Space::getUserId, userId);
            Space space = spaceService.getOne(wrapper);

            if (space == null) {
                return; // 用户没有空间，跳过
            }

            // 只升级普通版空间到专业版
            if (space.getSpaceLevel() != null && space.getSpaceLevel() == SpaceLevelEnum.COMMON.getValue()) {
                Space updateSpace = new Space();
                updateSpace.setId(space.getId());
                updateSpace.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());
                updateSpace.setMaxCount(SpaceLevelEnum.PROFESSIONAL.getMaxCount());
                updateSpace.setMaxSize(SpaceLevelEnum.PROFESSIONAL.getMaxSize());
                spaceService.updateById(updateSpace);
                log.info("VIP空间升级完成 | userId={}, spaceId={}", userId, space.getId());
            }
        } catch (Exception e) {
            log.error("VIP空间升级失败 | userId={}", userId, e);
        }
    }

    /**
     * VIP到期后降级空间为普通版
     * <p>
     * 将用户所有非 COMMON 级别的空间降级为 COMMON，降级后检查用量是否超限并记录告警日志
     * </p>
     */
    @Override
    public void downgradeSpaceForExpiredVip(Long userId) {
        try {
            LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Space::getUserId, userId);
            List<Space> spaces = spaceService.list(wrapper);

            if (spaces == null || spaces.isEmpty()) {
                return;
            }

            for (Space space : spaces) {
                // 只降级非普通版空间
                if (space.getSpaceLevel() == null || space.getSpaceLevel() == SpaceLevelEnum.COMMON.getValue()) {
                    continue;
                }

                // 降级为普通版
                Space updateSpace = new Space();
                updateSpace.setId(space.getId());
                updateSpace.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
                updateSpace.setMaxCount(SpaceLevelEnum.COMMON.getMaxCount());
                updateSpace.setMaxSize(SpaceLevelEnum.COMMON.getMaxSize());
                spaceService.updateById(updateSpace);

                log.info("VIP到期空间降级完成 | userId={}, spaceId={}, 原级别={}",
                        userId, space.getId(), space.getSpaceLevel());

                // 降级后检查用量是否超限
                long currentCount = space.getTotalCount() != null ? space.getTotalCount() : 0;
                long currentSize = space.getTotalSize() != null ? space.getTotalSize() : 0;
                long commonMaxCount = SpaceLevelEnum.COMMON.getMaxCount();
                long commonMaxSize = SpaceLevelEnum.COMMON.getMaxSize();

                if (currentCount > commonMaxCount || currentSize > commonMaxSize) {
                    log.warn("VIP到期空间降级后用量超限 | userId={}, spaceId={}, " +
                                    "当前数量={}/{}张, 当前大小={}/{}字节，将限制新上传",
                            userId, space.getId(),
                            currentCount, commonMaxCount,
                            currentSize, commonMaxSize);
                }
            }

            // 失效用户缓存，确保后续查询拿到降级后的空间信息
            invalidateUserCache(userId);
        } catch (Exception e) {
            log.error("VIP到期空间降级失败 | userId={}", userId, e);
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 转换实体为 VO
     */
    private CouponVO convertToVO(CodeCoupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCode(coupon.getCode());
        vo.setType(coupon.getType());
        vo.setStatus(coupon.getStatus());
        vo.setIssuedAt(coupon.getIssuedAt());
        vo.setActivatedAt(coupon.getActivatedAt());
        vo.setExpireAt(coupon.getExpireAt());

        // 券类型名称
        if (coupon.getType() != null) {
            vo.setTypeName(coupon.getType() + "天VIP");
        }

        // 状态名称
        CouponStatusEnum statusEnum = CouponStatusEnum.getEnumByValue(coupon.getStatus());
        if (statusEnum != null) {
            vo.setStatusName(statusEnum.getText());
        }

        // 剩余天数（仅已激活的券）
        if (coupon.getStatus() != null && coupon.getStatus() == CouponStatusEnum.ACTIVATED.getValue()
                && coupon.getExpireAt() != null) {
            long remainingMs = coupon.getExpireAt().getTime() - System.currentTimeMillis();
            vo.setRemainingDays(Math.max(0L, remainingMs / (24 * 3600 * 1000)));
        }

        return vo;
    }

    /**
     * 回滚 Redis 库存（退款时 +1）
     */
    private void rollbackRedisStock(Long batchId) {
        if (batchId == null) {
            return;
        }
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        try {
            stringRedisTemplate.execute(seckillRollbackScript, List.of(stockKey), "1");
            log.info("退款回滚Redis库存成功 | batchId={}", batchId);
        } catch (Exception e) {
            log.error("退款回滚Redis库存失败 | batchId={}", batchId, e);
        }
    }

    /**
     * 失效用户缓存
     */
    private void invalidateUserCache(Long userId) {
        try {
            String infoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
            String profileKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
            stringRedisTemplate.delete(infoKey);
            stringRedisTemplate.delete(profileKey);
        } catch (Exception e) {
            log.warn("失效用户缓存失败 | userId={}", userId, e);
        }
    }
}
