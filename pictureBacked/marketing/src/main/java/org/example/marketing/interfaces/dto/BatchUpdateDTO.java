package org.example.marketing.interfaces.dto;

import lombok.Data;

import java.util.Date;

/**
 * 修改发放批次 DTO（仅草稿阶段可修改）
 *
 * @author Zou
 */
@Data
public class BatchUpdateDTO {

    /**
     * 批次ID（必填）
     */
    private Long id;

    /**
     * 批次名称
     */
    private String name;

    /**
     * 券类型: 3/7/30 天
     */
    private Integer type;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 秒杀开始时间
     */
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    private Date endTime;
}
