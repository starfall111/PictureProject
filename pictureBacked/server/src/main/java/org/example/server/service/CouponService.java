package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.vo.seckill.CouponActivateVO;
import org.example.pojo.vo.seckill.CouponVO;

import java.util.List;

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
     * @return
     */
    CouponActivateVO activateCoupon(Long userId, Long couponId);

    Page<CouponVO> listMyCouponsVO(Long userId, Integer status, int page, int size);

    List<CodeCoupon> listMyCoupons(Long userId, Integer status);

    void refundCoupon(Long couponId, Long operatorId);

    /**
     * VIP到期后降级空间为普通版
     *
     * @param userId 用户ID
     */
    void downgradeSpaceForExpiredVip(Long userId);
}
