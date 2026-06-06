package org.example.pojo.dto.feedback;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈转举报 DTO
 *
 * @author Zou
 */
@Data
public class FeedbackConvertDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报目标类型
     */
    private String targetType;

    /**
     * 举报目标 ID
     */
    private Long targetId;

    /**
     * 举报原因类型
     */
    private String reasonType;

    /**
     * 举报描述
     */
    private String description;
}
