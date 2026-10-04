package org.example.picture.social.infrastructure.adapter;

import jakarta.annotation.Resource;
import org.example.identity.api.port.FollowStatsPort;
import org.example.identity.api.port.model.FollowCount;
import org.example.picture.social.application.FollowService;
import org.example.picture.social.interfaces.vo.FollowCountVO;
import org.springframework.stereotype.Component;

/**
 * 关注统计适配器 — 实现身份上下文的端口（防腐层实现）
 *
 * @author Zou
 */
@Component
public class FollowStatsAdapter implements FollowStatsPort {

    @Resource(name = "dbFollowService")
    private FollowService followService;

    @Override
    public FollowCount getFollowCount(Long userId) {
        FollowCountVO vo = followService.getFollowCount(userId);
        FollowCount count = new FollowCount();
        count.setFollowCount(vo.getFollowCount());
        count.setFollowerCount(vo.getFollowerCount());
        return count;
    }

    @Override
    public boolean isFollowing(Long followerUserId, Long targetUserId) {
        return followService.isFollowing(followerUserId, targetUserId);
    }
}
