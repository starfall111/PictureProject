package org.example.picture.moderation.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 举报详情 VO
 *
 * @author Zou
 */
@Data
public class ReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报记录 ID
     */
    private Long id;

    /**
     * 举报人 ID
     */
    private Long reporterId;

    /**
     * 举报人用户名
     */
    private String reporterName;

    /**
     * 举报人头像
     */
    private String reporterAvatar;

    /**
     * 举报目标类型
     */
    private String targetType;

    /**
     * 举报目标 ID
     */
    private Long targetId;

    /**
     * 举报目标信息
     */
    private ReportTargetVO targetInfo;

    /**
     * 举报原因类型
     */
    private String reasonType;

    /**
     * 举报原因描述
     */
    private String reasonDesc;

    /**
     * 举报详细描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 处理人 ID
     */
    private Long handlerId;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 处理结果描述
     */
    private String handleResultDesc;

    /**
     * 处理原因
     */
    private String handleReason;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 该目标的累计举报次数
     */
    private Integer reportCount;

    /**
     * 来源反馈 ID（若由反馈转换而来）
     */
    private Long sourceFeedbackId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
