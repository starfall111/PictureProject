package org.example.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.CodeCoupon;

/**
 * 编码券服务接口
 *
 * @author Zou
 */
public interface CouponService extends IService<CodeCoupon> {

    /**
     * 激活编码券（将 VIP 天数写入用户表）
     *
     * @param userId   用户ID
     * @param couponId 编码券ID
     */
    void activateCoupon(Long userId, Long couponId);
}
