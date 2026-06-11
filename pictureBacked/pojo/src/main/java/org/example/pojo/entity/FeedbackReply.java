package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈回复
 *
 * @author Zou
 * @TableName feedback_reply
 */
@TableName(value = "feedback_reply")
@Data
public class FeedbackReply implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联反馈ID */
    private Long feedbackId;

    /** 回复者用户ID */
    private Long userId;

    /** 回复类型：USER_REPLY/ADMIN_REPLY/INTERNAL_NOTE */
    private String replyType;

    /** 回复内容 */
    private String content;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
