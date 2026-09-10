package org.example.picture.moderation.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈状态日志
 *
 * @author Zou
 * @TableName feedback_status_log
 */
@TableName(value = "feedback_status_log")
@Data
public class FeedbackStatusLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联反馈ID */
    private Long feedbackId;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人类型：USER/ADMIN/SYSTEM */
    private String operatorType;

    /** 备注 */
    private String remark;

    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
