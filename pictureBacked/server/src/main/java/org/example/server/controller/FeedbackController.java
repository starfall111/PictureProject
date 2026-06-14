package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.feedback.FeedbackQueryDTO;
import org.example.pojo.dto.feedback.FeedbackReplyDTO;
import org.example.pojo.dto.feedback.FeedbackSubmitDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.feedback.FeedbackAttachmentVO;
import org.example.pojo.vo.feedback.FeedbackListItemVO;
import org.example.pojo.vo.feedback.FeedbackStatsVO;
import org.example.pojo.vo.feedback.FeedbackVO;
import org.example.server.service.FeedbackService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

/**
 * 用户端 — 反馈 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Resource
    @Qualifier("dbFeedbackService")
    private FeedbackService feedbackService;

    /**
     * 上传反馈附件
     *
     * @param file 附件文件
     * @return 附件信息（含 ID 和 URL）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<FeedbackAttachmentVO> uploadAttachment(@RequestPart("file") MultipartFile file) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR);

        FeedbackAttachmentVO result = feedbackService.uploadAttachment(file);
        return ResultUtils.success(result);
    }

    /**
     * 提交反馈
     *
     * @param dto 反馈提交参数
     * @return 反馈 ID
     */
    @PostMapping("/submit")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 3)
    public BaseResponse<Long> submitFeedback(@RequestBody FeedbackSubmitDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        long feedbackId = feedbackService.submitFeedback(dto);
        return ResultUtils.success(feedbackId);
    }

    /**
     * 撤回反馈（仅 PENDING 状态可撤回）
     *
     * @param id 反馈 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}/withdraw")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<Boolean> withdrawFeedback(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        feedbackService.withdrawFeedback(id);
        return ResultUtils.success(true);
    }

    /**
     * 获取个人反馈列表
     *
     * @param dto 分页查询参数
     * @return 反馈分页列表
     */
    @GetMapping("/list")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 30)
    public BaseResponse<Page<FeedbackListItemVO>> getMyFeedbackList(FeedbackQueryDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        Page<FeedbackListItemVO> result = feedbackService.getMyFeedbackList(dto);
        return ResultUtils.success(result);
    }

    /**
     * 获取反馈详情（含回复、状态日志、附件）
     *
     * @param id 反馈 ID
     * @return 反馈详情
     */
    @GetMapping("/{id}")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 30)
    public BaseResponse<FeedbackVO> getFeedbackDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        FeedbackVO result = feedbackService.getFeedbackDetail(id);
        return ResultUtils.success(result);
    }

    /**
     * 追加回复反馈
     *
     * @param id  反馈 ID
     * @param dto 回复内容
     * @return 操作结果
     */
    @PostMapping("/{id}/reply")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<Boolean> replyFeedback(@PathVariable Long id, @RequestBody FeedbackReplyDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        feedbackService.replyFeedback(id, dto);
        return ResultUtils.success(true);
    }

    /**
     * 重新打开反馈（限 2 次）
     *
     * @param id 反馈 ID
     * @return 操作结果
     */
    @PostMapping("/{id}/reopen")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 5)
    public BaseResponse<Boolean> reopenFeedback(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        feedbackService.reopenFeedback(id);
        return ResultUtils.success(true);
    }

    /**
     * 确认反馈已解决并关闭
     *
     * @param id 反馈 ID
     * @return 操作结果
     */
    @PostMapping("/{id}/confirm")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<Boolean> confirmFeedback(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        feedbackService.confirmFeedback(id);
        return ResultUtils.success(true);
    }

    /**
     * 获取个人反馈统计
     *
     * @return 反馈统计数据
     */
    @GetMapping("/stats")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 30)
    public BaseResponse<FeedbackStatsVO> getMyFeedbackStats() {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        FeedbackStatsVO result = feedbackService.getMyFeedbackStats();
        return ResultUtils.success(result);
    }
}
