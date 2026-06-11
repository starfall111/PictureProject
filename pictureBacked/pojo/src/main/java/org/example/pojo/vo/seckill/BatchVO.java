package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 批次管理 VO
 */
@Data
public class BatchVO {

    private Long id;

    private String batchNo;

    private String name;

    private Integer type;

    private String typeName;

    private Integer totalStock;

    private Integer currentStock;

    private Date startTime;

    private Date endTime;

    private Integer status;

    private String statusName;

    private Date createTime;
}
