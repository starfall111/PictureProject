package org.example.picture.social.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.social.domain.model.UserFollow;
import org.example.picture.social.interfaces.vo.FollowCountVO;
import org.example.picture.social.interfaces.vo.FollowUserVO;

/**
 * 用户关注服务接口
 *
 * @author Zou
 * @description 针对表【user_follow(用户关注关系)】的数据库操作Service
 * @createDate 2026-06-01
 * @Entity org.example.picture.social.domain.model.UserFollow
 */
public interface FollowService extends IService<UserFollow> {

    /**
     * 关注/取关用户
     *
     * @param currentUserId 当前登录用户ID
     * @param targetUserId  目标用户ID
     * @return true=关注，false=取关
     */
    boolean toggleFollow(Long currentUserId, Long targetUserId);

    /**
     * 分页获取关注列表
     *
     * @param userId   用户ID
     * @param current  当前页码
     * @param pageSize 每页数量
     * @return 关注列表
     */
    Page<FollowUserVO> listFollowing(Long userId, Integer current, Integer pageSize);

    /**
     * 分页获取粉丝列表
     *
     * @param userId         用户ID
     * @param currentUserId  当前登录用户ID（用于判断是否关注）
     * @param current        当前页码
     * @param pageSize       每页数量
     * @return 粉丝列表
     */
    Page<FollowUserVO> listFollowers(Long userId, Long currentUserId, Integer current, Integer pageSize);

    /**
     * 获取关注数和粉丝数
     *
     * @param userId 用户ID
     * @return 关注统计
     */
    FollowCountVO getFollowCount(Long userId);

    /**
     * 查询是否关注某用户
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 是否关注
     */
    boolean isFollowing(Long followerId, Long followeeId);
}
