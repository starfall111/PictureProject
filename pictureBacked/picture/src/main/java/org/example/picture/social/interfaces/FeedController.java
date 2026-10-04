package org.example.picture.social.interfaces;

import cn.hutool.core.util.ObjUtil;
import org.example.shared.annotation.CheckAuth;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.social.interfaces.dto.FeedQueryDTO;
import org.example.identity.api.model.User;
import org.example.picture.social.interfaces.vo.FeedTimelineVO;
import org.example.picture.social.interfaces.vo.FeedUnreadVO;
import org.example.picture.social.application.FeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 动态 Feed 控制器
 * <p>
 * 提供关注用户的图片动态时间线、未读数统计和已读标记接口
 *
 * @author Zou
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

    @Resource(name = "cachedFeedService")
    private FeedService feedService;

    /**
     * 获取动态时间线
     *
     * @param queryDTO 分页查询参数（current, pageSize）
     * @return 动态分页结果（含 unreadCount）
     */
    @GetMapping("/timeline")
    @RateLimit(resource = "feed.timeline", dimensions = {RateLimitDimension.USER})
    public BaseResponse<FeedTimelineVO> getTimeline(FeedQueryDTO queryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(queryDTO), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        FeedTimelineVO result = feedService.getTimeline(currentUser.getId(), queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * 获取未读动态数
     *
     * @return 未读数信息
     */
    @GetMapping("/unread-count")
    public BaseResponse<FeedUnreadVO> getUnreadCount() {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        FeedUnreadVO result = feedService.getUnreadCount(currentUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 标记动态已读（更新水位线，清除缓存）
     *
     * @return 操作结果
     */
    @PostMapping("/mark-read")
    public BaseResponse<Boolean> markRead() {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        feedService.markRead(currentUser.getId());
        return ResultUtils.success(true);
    }
}
