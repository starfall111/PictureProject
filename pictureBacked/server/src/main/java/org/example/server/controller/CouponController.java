package org.example.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.common.context.UserContext;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.CouponActivateDTO;
import org.example.pojo.vo.seckill.CouponActivateVO;
import org.example.pojo.vo.seckill.CouponVO;
import org.example.server.service.CouponService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 编码券 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Resource
    private CouponService couponService;

    /**
     * 查询我的编码券列表（分页）
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
     */
    @PostMapping("/activate")
    @CheckAuth
    @RateLimit(resource = "coupon.activate", dimensions = {RateLimitDimension.USER})
    public BaseResponse<CouponActivateVO> activateCoupon(
            @RequestBody CouponActivateDTO couponActivateDTO) {
        Long userId = UserContext.get().getId();
        CouponActivateVO result = couponService.activateCoupon(userId, couponActivateDTO.getCouponId());
        return ResultUtils.success(result);
    }
}
