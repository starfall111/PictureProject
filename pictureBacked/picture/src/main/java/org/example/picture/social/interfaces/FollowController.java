package org.example.picture.social.interfaces;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.shared.annotation.CheckAuth;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.picture.social.interfaces.dto.FollowActionDTO;
import org.example.identity.api.model.User;
import org.example.picture.social.interfaces.vo.FollowCountVO;
import org.example.picture.social.interfaces.vo.FollowUserVO;
import org.example.picture.social.application.FollowService;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 用户关注控制器
 *
 * @author Zou
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource(name = "dbFollowService")
    private FollowService followService;

    /**
     * 关注/取关用户
     *
     * @param followActionDTO 关注请求体
     * @return true=关注，false=取关
     */
    @PostMapping("/action")
    @CheckAuth
    @RateLimit(resource = "follow.toggle", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Boolean> toggleFollow(@RequestBody FollowActionDTO followActionDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(followActionDTO), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(followActionDTO.getTargetUserId()), ErrorCode.PARAMS_ERROR, "目标用户ID不能为空");

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        boolean result = followService.toggleFollow(currentUser.getId(), followActionDTO.getTargetUserId());
        return ResultUtils.success(result);
    }

    /**
     * 获取关注列表
     *
     * @param userId   用户ID
     * @param current  当前页码
     * @param pageSize 每页数量
     * @return 关注列表
     */
    @GetMapping("/list/following")
    public BaseResponse<Page<FollowUserVO>> listFollowing(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userId), ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        Page<FollowUserVO> page = followService.listFollowing(userId, current, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 获取粉丝列表
     *
     * @param userId   用户ID
     * @param current  当前页码
     * @param pageSize 每页数量
     * @return 粉丝列表
     */
    @GetMapping("/list/followers")
    public BaseResponse<Page<FollowUserVO>> listFollowers(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userId), ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        User currentUser = UserContext.get();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        Page<FollowUserVO> page = followService.listFollowers(userId, currentUserId, current, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 获取关注数和粉丝数
     *
     * @param userId 用户ID
     * @return 关注统计
     */
    @GetMapping("/count/{userId}")
    public BaseResponse<FollowCountVO> getFollowCount(@PathVariable Long userId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userId), ErrorCode.PARAMS_ERROR);

        FollowCountVO count = followService.getFollowCount(userId);
        return ResultUtils.success(count);
    }

    /**
     * 查询是否关注某用户
     *
     * @param targetUserId 目标用户ID
     * @return 是否关注
     */
    @GetMapping("/status/{targetUserId}")
    @CheckAuth
    public BaseResponse<Boolean> isFollowing(@PathVariable Long targetUserId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(targetUserId), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        boolean result = followService.isFollowing(currentUser.getId(), targetUserId);
        return ResultUtils.success(result);
    }
}
