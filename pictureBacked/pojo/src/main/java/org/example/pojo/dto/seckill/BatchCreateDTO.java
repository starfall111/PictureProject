package org.example.pojo.dto.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 创建发放批次 DTO
 */
@Data
public class BatchCreateDTO {

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
     * 秒杀结束时间（必填）
     */
    private Date endTime;
}
