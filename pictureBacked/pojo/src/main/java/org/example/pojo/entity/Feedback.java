package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈
 *
 * @author Zou
 * @TableName feedback
 */
@TableName(value = "feedback")
@Data
public class Feedback implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 提交者用户ID（匿名反馈为NULL） */
    private Long userId;

    /** 反馈标题 */
    private String title;

    /** 内容描述 */
    private String content;

    /** 类型：BUG/FEATURE/ACCOUNT/EXPERIENCE/OTHER */
    private String type;

    /** 优先级：P0/P1/P2/P3 */
    private String priority;

    /** 状态：PENDING/PROCESSING/RESOLVED/REJECTED/REOPENED/CLOSED */
    private String status;

    /** 来源：APP/SYSTEM/ADMIN */
    private String source;

    /** 关联图片ID（可选） */
    private Long relatedPictureId;

    /** 关联用户ID（可选） */
    private Long relatedUserId;

    /** 当前处理人管理员ID */
    private Long handlerId;

    /** 是否匿名提交 */
    private Integer isAnonymous;

    /** 关闭原因 */
    private String closeReason;

    /** 重新打开次数，上限2 */
    private Integer reopenCount;

    /** 转为举报后的report.id */
    private Long convertedReportId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
