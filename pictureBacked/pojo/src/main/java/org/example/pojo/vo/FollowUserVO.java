package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 关注用户简要信息 VO
 */
@Data
public class FollowUserVO implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 当前登录用户是否关注了该用户（仅粉丝列表需要）
     */
    private Boolean isFollowing;

    /**
     * 关注时间（仅粉丝列表需要）
     */
    private Date followTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
