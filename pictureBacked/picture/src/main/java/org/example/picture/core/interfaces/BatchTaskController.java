package org.example.picture.core.interfaces;

import cn.hutool.core.util.ObjUtil;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.identity.api.model.User;
import org.example.picture.core.interfaces.vo.BatchTaskVO;
import org.example.picture.core.application.BatchTaskService;
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
    @RateLimit(resource = "batchtask.progress", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "batchtask.list", dimensions = {RateLimitDimension.USER})
    public BaseResponse<List<BatchTaskVO>> listTasks() {
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        List<BatchTaskVO> tasks = batchTaskService.listUserTasks(user.getId());
        return ResultUtils.success(tasks);
    }
}
