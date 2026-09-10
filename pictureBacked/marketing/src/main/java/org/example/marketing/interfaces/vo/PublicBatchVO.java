package org.example.marketing.interfaces.vo;

import lombok.Data;

import java.util.Date;

/**
 * 公开抢购批次 VO（用户端展示）
 *
 * @author Zou
 */
@Data
public class PublicBatchVO {

    private Long id;

    /**
     * 批次名称
     */
    private String name;

    /**
     * 券类型
     */
    private Integer type;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer remainStock;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 批次状态: 1-预热中, 2-进行中, 3-已结束
     */
    private Integer status;

    /**
     * 售出百分比 (0~100)
     */
    private Integer progress;
}
