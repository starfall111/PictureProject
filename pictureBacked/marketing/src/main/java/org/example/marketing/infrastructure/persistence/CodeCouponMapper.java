package org.example.marketing.infrastructure.persistence;

import org.apache.ibatis.annotations.Param;
import org.example.marketing.domain.model.CodeCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【code_coupon(编码券)】的数据库操作Mapper
 */
public interface CodeCouponMapper extends BaseMapper<CodeCoupon> {

    /**
     * 行锁查询：获取一张未发放的券（FOR UPDATE SKIP LOCKED）
     * SKIP LOCKED：如果该行已被其他事务锁定则跳过，避免阻塞等待
     */
    CodeCoupon selectOneAvailableForUpdate(@Param("batchId") Long batchId);
}
