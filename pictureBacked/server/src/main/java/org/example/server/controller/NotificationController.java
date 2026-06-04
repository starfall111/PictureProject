package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.notification.NotificationQueryDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.NotificationVO;
import org.example.server.service.NotificationService;
import org.example.server.service.sse.SsePushService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.util.HashSet;

/**
 * 通知 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource(name = "cachedNotificationService")
    private NotificationService notificationService;

    @Resource
    private SsePushService ssePushService;

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadCount() {
        User user = UserContext.get();
        if (user == null) {
            return ResultUtils.success(0L);
        }
        long count = notificationService.getUnreadCount(user.getId());
        return ResultUtils.success(count);
    }

    /**
     * 获取通知列表（分页）
     */
    @GetMapping("/list")
    public BaseResponse<Page<NotificationVO>> listNotifications(NotificationQueryDTO queryDTO) {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR);

        Page<NotificationVO> page = notificationService.listNotifications(user.getId(), queryDTO);
        return ResultUtils.success(page);
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/read/{id}")
    public BaseResponse<Boolean> markAsRead(@PathVariable Long id) {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = notificationService.markAsRead(id, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read/all")
    public BaseResponse<Boolean> markAllAsRead() {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean result = notificationService.markAllAsRead(user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 删除单条通知
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteNotification(@PathVariable Long id) {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = notificationService.deleteNotification(id, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 清空已读通知
     */
    @DeleteMapping("/clean/read")
    public BaseResponse<Integer> cleanReadNotifications() {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        int count = notificationService.cleanReadNotifications(user.getId());
        return ResultUtils.success(count);
    }

    /**
     * SSE 实时推送连接
     */
    @GetMapping(value = "/sse", produces = "text/event-stream")
    public SseEmitter connectSse() {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        return ssePushService.createEmitter(user.getId());
    }
}
