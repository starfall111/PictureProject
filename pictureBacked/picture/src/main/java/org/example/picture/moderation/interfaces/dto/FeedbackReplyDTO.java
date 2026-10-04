package org.example.picture.moderation.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈回复 DTO
 *
 * @author Zou
 */
@Data
public class FeedbackReplyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 回复内容
     */
    private String content;
}
