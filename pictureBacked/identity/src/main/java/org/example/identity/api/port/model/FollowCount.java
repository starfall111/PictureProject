package org.example.identity.api.port.model;

import lombok.Data;

/**
 * 关注计数（发布语言：身份 ← 图片·社交子域）
 *
 * @author Zou
 */
@Data
public class FollowCount {

    /** 关注数 */
    private Integer followCount;

    /** 粉丝数 */
    private Integer followerCount;
}
