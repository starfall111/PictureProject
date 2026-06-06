package org.example.pojo.dto.feedback;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 反馈提交 DTO
 *
 * @author Zou
 */
@Data
public class FeedbackSubmitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 关联图片 ID
     */
    private Long relatedPictureId;

    /**
     * 关联用户 ID
     */
    private Long relatedUserId;

    /**
     * 附件 ID 列表
     */
    private List<Long> attachmentIds;

    /**
     * 是否匿名提交
     */
    private Boolean isAnonymous;
}
