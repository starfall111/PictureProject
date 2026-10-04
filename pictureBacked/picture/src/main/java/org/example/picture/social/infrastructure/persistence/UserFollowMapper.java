package org.example.picture.social.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.example.picture.social.domain.model.UserFollow;
import org.example.picture.social.interfaces.vo.FollowUserVO;

import java.util.List;

/**
 * 用户关注关系 Mapper
 *
 * @author Zou
 * @description 针对表【user_follow(用户关注关系)】的数据库操作Mapper
 * @createDate 2026-06-01
 * @Entity org.example.picture.social.domain.model.UserFollow
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 查询关注关系是否存在
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 关注关系
     */
    UserFollow selectByFollowerAndFollowee(@Param("followerId") Long followerId,
                                           @Param("followeeId") Long followeeId);

    /**
     * 统计关注关系是否存在
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 数量
     */
    Long countByFollowerAndFollowee(@Param("followerId") Long followerId,
                                     @Param("followeeId") Long followeeId);

    /**
     * 统计关注数
     *
     * @param userId 用户ID
     * @return 关注数
     */
    Long countFollowing(@Param("userId") Long userId);

    /**
     * 统计粉丝数
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    Long countFollowers(@Param("userId") Long userId);

    /**
     * 分页查询关注列表
     *
     * @param page    分页参数
     * @param userId  用户ID
     * @return 关注列表
     */
    Page<FollowUserVO> selectFollowingList(Page<FollowUserVO> page,
                                           @Param("userId") Long userId);

    /**
     * 分页查询粉丝列表
     *
     * @param page          分页参数
     * @param userId        用户ID
     * @param currentUserId 当前登录用户ID（用于判断是否关注）
     * @return 粉丝列表
     */
    Page<FollowUserVO> selectFollowersList(Page<FollowUserVO> page,
                                           @Param("userId") Long userId,
                                           @Param("currentUserId") Long currentUserId);

    /**
     * 查询用户关注的所有用户ID
     *
     * @param followerId 关注者ID
     * @return 被关注的用户ID列表
     */
    List<Long> selectFolloweeIds(@Param("followerId") Long followerId);

    /**
     * 按关注者和被关注者直接删除（替代 selectByFollowerAndFollowee + deleteById 两步操作）
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 删除行数
     */
    int deleteByFollowerAndFollowee(@Param("followerId") Long followerId,
                                    @Param("followeeId") Long followeeId);
}
