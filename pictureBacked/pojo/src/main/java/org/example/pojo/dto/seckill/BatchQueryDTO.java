package org.example.pojo.dto.seckill;

import lombok.Data;

/**
 * 批次查询 DTO
 */
@Data
public class BatchQueryDTO {

    /**
     * 状态筛选: 0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消
     */
    private Integer status;

    private int current = 1;

    private int pageSize = 10;
}
