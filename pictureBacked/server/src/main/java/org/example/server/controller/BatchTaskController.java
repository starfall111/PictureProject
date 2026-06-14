package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.entity.User;
import org.example.pojo.vo.BatchTaskVO;
import org.example.server.service.BatchTaskService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 批量获取图片任务接口
 *
 * @author Zou
 */
@RestController
@RequestMapping("/batch/task")
public class BatchTaskController {

    @Resource
    private BatchTaskService batchTaskService;

    /**
     * 查询单个任务状态
     */
    @GetMapping("/{taskId}")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 5, maxAttempts = 5)
    public BaseResponse<BatchTaskVO> getTask(@PathVariable Long taskId) {
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        BatchTaskVO taskVO = batchTaskService.getTaskById(taskId, user.getId());
        return ResultUtils.success(taskVO);
    }

    /**
     * 查询当前用户的批量任务列表
     */
    @GetMapping("/list")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 20)
    public BaseResponse<List<BatchTaskVO>> listTasks() {
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        List<BatchTaskVO> tasks = batchTaskService.listUserTasks(user.getId());
        return ResultUtils.success(tasks);
    }
}
