package org.example.picture.social.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关注/取关请求体
 */
@Data
public class FollowActionDTO implements Serializable {

    /**
     * 目标用户ID（要关注/取关的用户）
     */
    private Long targetUserId;

    @Serial
    private static final long serialVersionUID = 1L;
}
