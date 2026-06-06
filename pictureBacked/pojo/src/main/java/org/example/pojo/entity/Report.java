package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 举报
 *
 * @author Zou
 * @TableName report
 */
@TableName(value = "report")
@Data
public class Report implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 举报对象类型：PICTURE/USER */
    private String targetType;

    /** 举报目标ID */
    private Long targetId;

    /** 举报原因 */
    private String reasonType;

    /** 补充描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 处理人ID */
    private Long handlerId;

    /** 处理结果 */
    private String handleResult;

    /** 处理理由 */
    private String handleReason;

    /** 处理时间 */
    private Date handleTime;

    /** 该目标累计被举报次数 */
    private Integer reportCount;

    /** 来源反馈ID */
    private Long sourceFeedbackId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
