package org.example.pojo.vo.seckill;

import lombok.Data;

/**
 * 秒杀数据统计 VO
 */
@Data
public class SeckillStatsVO {

    /**
     * 总发放批次数
     */
    private Integer totalBatches;

    /**
     * 总编码券数
     */
    private Integer totalCoupons;

    /**
     * 已领取数
     */
    private Integer claimedCount;

    /**
     * 已激活数
     */
    private Integer activatedCount;

    /**
     * 已过期数
     */
    private Integer expiredCount;

    /**
     * 领取率
     */
    private Double claimRate;

    /**
     * 激活率
     */
    private Double activationRate;
}
