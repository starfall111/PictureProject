package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关注统计 VO
 */
@Data
public class FollowCountVO implements Serializable {

    /**
     * 关注数（正在关注多少人）
     */
    private Integer followCount;

    /**
     * 粉丝数（被多少人关注）
     */
    private Integer followerCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
