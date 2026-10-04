package org.example.picture.moderation.interfaces;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.moderation.interfaces.dto.FeedbackQueryDTO;
import org.example.picture.moderation.interfaces.dto.FeedbackReplyDTO;
import org.example.picture.moderation.interfaces.dto.FeedbackSubmitDTO;
import org.example.identity.api.model.User;
import org.example.picture.moderation.interfaces.vo.FeedbackAttachmentVO;
import org.example.picture.moderation.interfaces.vo.FeedbackListItemVO;
import org.example.picture.moderation.interfaces.vo.FeedbackStatsVO;
import org.example.picture.moderation.interfaces.vo.FeedbackVO;
import org.example.picture.moderation.application.FeedbackService;
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
    @RateLimit(resource = "feedback.upload", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.submit", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.withdraw", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.list", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.detail", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.reply", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.reopen", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.confirm", dimensions = {RateLimitDimension.USER})
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
    @RateLimit(resource = "feedback.stats", dimensions = {RateLimitDimension.USER})
    public BaseResponse<FeedbackStatsVO> getMyFeedbackStats() {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        FeedbackStatsVO result = feedbackService.getMyFeedbackStats();
        return ResultUtils.success(result);
    }
}
