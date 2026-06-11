package org.example.pojo.vo.seckill;

import lombok.Data;

/**
 * 会员统计 VO
 */
@Data
public class VipStatsVO {

    /**
     * VIP 总人数（含已过期）
     */
    private Integer totalVipUsers;

    /**
     * 当前有效 VIP 人数
     */
    private Integer activeVipUsers;

    /**
     * 今日新增 VIP
     */
    private Integer todayNewVip;

    /**
     * 即将到期（3天内）VIP 人数
     */
    private Integer expiringVipUsers;
}
