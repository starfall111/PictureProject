package org.example.identity.api.port;

import org.example.identity.api.port.model.FollowCount;

/**
 * 关注统计端口 — 由图片模块（社交子域）提供实现（防腐层）
 *
 * @author Zou
 */
public interface FollowStatsPort {

    /**
     * 查询用户的关注/粉丝数
     *
     * @param userId 用户 ID
     * @return 关注计数
     */
    FollowCount getFollowCount(Long userId);

    /**
     * 判断当前用户是否关注了目标用户
     *
     * @param followerUserId 当前用户 ID
     * @param targetUserId   目标用户 ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerUserId, Long targetUserId);
}
