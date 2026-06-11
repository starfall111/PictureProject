package org.example.server.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.enums.CouponStatusEnum;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.entity.CodeCoupon;
import org.example.server.mapper.CodeCouponMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 管理后台 — 编码券管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/coupon")
public class AdminCouponController {

    @Resource
    private CodeCouponMapper couponMapper;

    /**
     * 编码券列表（支持按状态、用户、批次筛选）
     * GET /api/admin/coupon/list
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<CodeCoupon>> listCoupons(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long batchId) {
        LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CodeCoupon::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(CodeCoupon::getUserId, userId);
        }
        if (batchId != null) {
            wrapper.eq(CodeCoupon::getBatchId, batchId);
        }
        wrapper.orderByDesc(CodeCoupon::getCreateTime);
        Page<CodeCoupon> page = couponMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return ResultUtils.success(page);
    }

    /**
     * 吊销编码券
     * POST /api/admin/coupon/{id}/revoke
     */
    @PostMapping("/{id}/revoke")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<?> revoke(@PathVariable Long id) {
        CodeCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            return ResultUtils.error(40000, "编码券不存在");
        }
        // 仅已领取未使用的券可吊销
        if (coupon.getStatus() != CouponStatusEnum.CLAIMED.getValue()) {
            return ResultUtils.error(40000, "券状态不支持吊销");
        }
        coupon.setStatus(CouponStatusEnum.EXPIRED.getValue());
        couponMapper.updateById(coupon);
        return ResultUtils.success(true);
    }
}
