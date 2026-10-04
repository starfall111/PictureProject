package org.example.picture.moderation.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 反馈详情 VO
 *
 * @author Zou
 */
@Data
public class FeedbackVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 反馈 ID
     */
    private Long id;

    /**
     * 提交用户 ID
     */
    private Long userId;

    /**
     * 提交用户名
     */
    private String userName;

    /**
     * 提交用户头像
     */
    private String userAvatar;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 反馈类型
     */
    private String type;

    /**
     * 反馈类型描述
     */
    private String typeDesc;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 来源
     */
    private String source;

    /**
     * 关联图片 ID
     */
    private Long relatedPictureId;

    /**
     * 关联用户 ID
     */
    private Long relatedUserId;

    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 重新打开次数
     */
    private Integer reopenCount;

    /**
     * 转换后的举报记录 ID
     */
    private Long convertedReportId;

    /**
     * 回复列表
     */
    private List<FeedbackReplyVO> replies;

    /**
     * 状态变更日志列表
     */
    private List<FeedbackStatusLogVO> statusLogs;

    /**
     * 附件列表
     */
    private List<FeedbackAttachmentVO> attachments;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
