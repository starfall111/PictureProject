package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.NotificationTypeEnum;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.entity.User;
import org.example.pojo.entity.UserFollow;
import org.example.pojo.vo.FollowCountVO;
import org.example.pojo.vo.FollowUserVO;
import org.example.server.mapper.UserFollowMapper;
import org.example.server.mapper.UserMapper;
import org.example.server.service.event.NotificationEvent;
import org.example.server.service.FollowService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 用户关注服务实现
 * 缓存策略：Redis 双写 + DB 实时双写 + 凌晨定时任务校准
 *
 * @author Zou
 * @description 针对表【user_follow(用户关注关系)】的数据库操作Service实现
 * @createDate 2026-06-01
 */
@Slf4j
@Service("dbFollowService")
public class FollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements FollowService {

    @Resource
    private UserFollowMapper userFollowMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Override
    @Transactional
    // todo 关注时更新用户 feed 的readline 为当前时间
    public boolean toggleFollow(Long currentUserId, Long targetUserId) {
        // 1. 参数校验
        ThrowUtils.throwIf(ObjUtil.equal(currentUserId, targetUserId),
                ErrorCode.PARAMS_ERROR, "不能关注自己");
        User targetUser = userMapper.selectById(targetUserId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(targetUser), ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 2. 分布式锁（复用 RedisCacheUtil.executeWithLock）
        String lockKey = String.format(RedisKeyConstants.FOLLOW_LOCK_KEY, currentUserId, targetUserId);
        return redisCacheUtil.executeWithLock(lockKey, () -> {
            String statusKey = String.format(RedisKeyConstants.FOLLOW_STATUS_KEY, currentUserId, targetUserId);
            String followCountKey = String.format(RedisKeyConstants.FOLLOW_COUNT_KEY, currentUserId);
            String followerCountKey = String.format(RedisKeyConstants.FOLLOWER_COUNT_KEY, targetUserId);

            String existing = stringRedisTemplate.opsForValue().get(statusKey);
            if ("1".equals(existing)) {
                // 已关注 → 取关
                int ttl = RedisKeyConstants.ttlWithJitter(
                        RedisKeyConstants.FOLLOW_STATUS_TTL_BASE, RedisKeyConstants.FOLLOW_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(statusKey, "0", ttl, TimeUnit.SECONDS);
                safeDecrement(followCountKey);
                safeDecrement(followerCountKey);
                // DB 删除
                userFollowMapper.deleteByFollowerAndFollowee(currentUserId, targetUserId);
                return false;
            } else {
                // 未关注 → 关注
                int ttl = RedisKeyConstants.ttlWithJitter(
                        RedisKeyConstants.FOLLOW_STATUS_TTL_BASE, RedisKeyConstants.FOLLOW_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(statusKey, "1", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().increment(followCountKey);
                stringRedisTemplate.opsForValue().increment(followerCountKey);
                // DB 插入
                UserFollow newFollow = new UserFollow();
                newFollow.setFollowerId(currentUserId);
                newFollow.setFolloweeId(targetUserId);
                save(newFollow);

                // 发送关注通知
                publishFollowEvent(currentUserId, targetUserId);
                return true;
            }
        }, "操作过于频繁，请稍后再试");
    }

    @Override
    public Page<FollowUserVO> listFollowing(Long userId, Integer current, Integer pageSize) {
        return userFollowMapper.selectFollowingList(new Page<>(current, pageSize), userId);
    }

    @Override
    public Page<FollowUserVO> listFollowers(Long userId, Long currentUserId, Integer current, Integer pageSize) {
        return userFollowMapper.selectFollowersList(new Page<>(current, pageSize), userId, currentUserId);
    }

    @Override
    public FollowCountVO getFollowCount(Long userId) {
        String followCountKey = String.format(RedisKeyConstants.FOLLOW_COUNT_KEY, userId);
        String followerCountKey = String.format(RedisKeyConstants.FOLLOWER_COUNT_KEY, userId);

        String followCountStr = stringRedisTemplate.opsForValue().get(followCountKey);
        String followerCountStr = stringRedisTemplate.opsForValue().get(followerCountKey);

        FollowCountVO vo = new FollowCountVO();

        // 独立处理每个缓存 key，避免部分未命中时多余的 DB 查询
        if (followCountStr != null) {
            vo.setFollowCount(Math.max(0, Integer.parseInt(followCountStr)));
        } else {
            Long followCount = userFollowMapper.countFollowing(userId);
            vo.setFollowCount(followCount.intValue());
            int ttl = RedisKeyConstants.ttlWithJitter(
                    RedisKeyConstants.FOLLOW_COUNT_TTL_BASE, RedisKeyConstants.FOLLOW_COUNT_TTL_JITTER);
            stringRedisTemplate.opsForValue().setIfAbsent(followCountKey, followCount.toString(), ttl, TimeUnit.SECONDS);
        }

        if (followerCountStr != null) {
            vo.setFollowerCount(Math.max(0, Integer.parseInt(followerCountStr)));
        } else {
            Long followerCount = userFollowMapper.countFollowers(userId);
            vo.setFollowerCount(followerCount.intValue());
            int ttl = RedisKeyConstants.ttlWithJitter(
                    RedisKeyConstants.FOLLOW_COUNT_TTL_BASE, RedisKeyConstants.FOLLOW_COUNT_TTL_JITTER);
            stringRedisTemplate.opsForValue().setIfAbsent(followerCountKey, followerCount.toString(), ttl, TimeUnit.SECONDS);
        }

        return vo;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        String statusKey = String.format(RedisKeyConstants.FOLLOW_STATUS_KEY, followerId, followeeId);
        String status = stringRedisTemplate.opsForValue().get(statusKey);

        if (status != null) {
            return "1".equals(status);
        }

        // 缓存未命中，查数据库
        Long count = userFollowMapper.countByFollowerAndFollowee(followerId, followeeId);
        boolean following = count != null && count > 0;

        // 回填 Redis（"1" 或 "0"）
        int ttl = RedisKeyConstants.ttlWithJitter(
                RedisKeyConstants.FOLLOW_STATUS_TTL_BASE, RedisKeyConstants.FOLLOW_STATUS_TTL_JITTER);
        stringRedisTemplate.opsForValue().set(statusKey, following ? "1" : "0", ttl, TimeUnit.SECONDS);

        return following;
    }

    /**
     * 安全递减：key 不存在时跳过，防止 DECR 不存在的 key 导致 -1
     */
    private void safeDecrement(String key) {
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val != null) {
            stringRedisTemplate.opsForValue().decrement(key);
        }
    }

    /**
     * 发布关注通知事件（与 CachedSocialServiceImpl.publishNotification 保持一致的防御性处理）
     */
    private void publishFollowEvent(Long followerId, Long followeeId) {
        try {
            User follower = userMapper.selectById(followerId);
            if (follower == null) {
                return;
            }
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    followeeId,
                    followerId,
                    follower.getUserName(),
                    follower.getUserAvatar(),
                    NotificationTypeEnum.FOLLOW,
                    "新增粉丝",
                    "用户 " + follower.getUserName() + " 关注了你",
                    null,
                    "/user/" + followerId
            ));
        } catch (Exception e) {
            log.warn("发布关注通知事件失败：followerId={}, followeeId={}", followerId, followeeId, e);
        }
    }
}
