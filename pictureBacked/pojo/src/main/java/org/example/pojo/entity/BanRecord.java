package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 封禁记录
 *
 * @author Zou
 * @TableName ban_record
 */
@TableName(value = "ban_record")
@Data
public class BanRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 被封禁用户ID */
    private Long userId;

    /** 封禁类型：TEMP/PERMANENT */
    private String banType;

    /** 封禁天数 */
    private Integer banDuration;

    /** 封禁开始时间 */
    private Date banStartTime;

    /** 封禁结束时间（永久封禁为NULL） */
    private Date banEndTime;

    /** 封禁原因 */
    private String banReason;

    /** 封禁时累计违规次数 */
    private Integer violationCount;

    /** 是否已解封 */
    private Integer unbanned;

    /** 解封时间 */
    private Date unbanTime;

    /** 解封理由 */
    private String unbanReason;

    /** 解封操作人ID */
    private Long unbanOperatorId;

    /** 封禁操作人ID */
    private Long banOperatorId;

    /** 封禁操作人姓名 */
    private String banOperatorName;

    /** 关联举报ID */
    private Long reportId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
