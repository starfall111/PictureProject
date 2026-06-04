package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.feed.FeedQueryDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.FeedTimelineVO;
import org.example.pojo.vo.FeedUnreadVO;
import org.example.server.service.FeedService;
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
