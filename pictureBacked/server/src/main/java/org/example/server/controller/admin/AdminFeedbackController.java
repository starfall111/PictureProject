package org.example.server.controller.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.feedback.FeedbackConvertDTO;
import org.example.pojo.dto.feedback.FeedbackQueryDTO;
import org.example.pojo.dto.feedback.FeedbackReplyDTO;
import org.example.pojo.vo.feedback.FeedbackListItemVO;
import org.example.pojo.vo.feedback.FeedbackStatsVO;
import org.example.pojo.vo.feedback.FeedbackVO;
import org.example.server.service.FeedbackService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 管理端 — 反馈管理 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    @Resource
    @Qualifier("dbFeedbackService")
    private FeedbackService feedbackService;

    /**
     * 获取全量反馈列表
     *
     * @param dto 分页查询参数（支持状态、类型、优先级筛选）
     * @return 反馈分页列表
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<FeedbackListItemVO>> getAdminFeedbackList(FeedbackQueryDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        Page<FeedbackListItemVO> result = feedbackService.getAdminFeedbackList(dto);
        return ResultUtils.success(result);
    }

    /**
     * 获取反馈详情（含提交者信息、回复、状态日志、附件）
     *
     * @param id 反馈 ID
     * @return 反馈详情
     */
    @GetMapping("/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<FeedbackVO> getAdminFeedbackDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        FeedbackVO result = feedbackService.getAdminFeedbackDetail(id);
        return ResultUtils.success(result);
    }

    /**
     * 认领反馈（将当前管理员设为处理人）
     *
     * @param id 反馈 ID
     * @return 操作结果
     */
    @PostMapping("/{id}/claim")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> claimFeedback(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        feedbackService.claimFeedback(id);
        return ResultUtils.success(true);
    }

    /**
     * 转派反馈给其他管理员
     *
     * @param id              反馈 ID
     * @param targetHandlerId 目标处理人 ID
     * @return 操作结果
     */
    @PostMapping("/{id}/transfer")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> transferFeedback(@PathVariable Long id, @RequestParam Long targetHandlerId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(targetHandlerId), ErrorCode.PARAMS_ERROR);

        feedbackService.transferFeedback(id, targetHandlerId);
        return ResultUtils.success(true);
    }

    /**
     * 管理员回复反馈
     *
     * @param id  反馈 ID
     * @param dto 回复内容
     * @return 操作结果
     */
    @PostMapping("/{id}/reply")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> adminReplyFeedback(@PathVariable Long id, @RequestBody FeedbackReplyDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        feedbackService.adminReplyFeedback(id, dto);
        return ResultUtils.success(true);
    }

    /**
     * 添加内部备注（仅管理员可见）
     *
     * @param id  反馈 ID
     * @param dto 备注内容
     * @return 操作结果
     */
    @PostMapping("/{id}/note")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> addInternalNote(@PathVariable Long id, @RequestBody FeedbackReplyDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        feedbackService.addInternalNote(id, dto);
        return ResultUtils.success(true);
    }

    /**
     * 拒绝反馈
     *
     * @param id     反馈 ID
     * @param reason 拒绝原因
     * @return 操作结果
     */
    @PostMapping("/{id}/reject")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> rejectFeedback(@PathVariable Long id, @RequestParam String reason) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(reason), ErrorCode.PARAMS_ERROR);

        feedbackService.rejectFeedback(id, reason);
        return ResultUtils.success(true);
    }

    /**
     * 调整反馈优先级
     *
     * @param id       反馈 ID
     * @param priority 优先级（P0/P1/P2/P3）
     * @return 操作结果
     */
    @PostMapping("/{id}/priority")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updatePriority(@PathVariable Long id, @RequestParam String priority) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(priority), ErrorCode.PARAMS_ERROR);

        feedbackService.updatePriority(id, priority);
        return ResultUtils.success(true);
    }

    /**
     * 将反馈转为举报工单
     *
     * @param id  反馈 ID
     * @param dto 举报转换参数
     * @return 生成的举报记录 ID
     */
    @PostMapping("/{id}/convert")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Long> convertToReport(@PathVariable Long id, @RequestBody FeedbackConvertDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        long reportId = feedbackService.convertToReport(id, dto);
        return ResultUtils.success(reportId);
    }

    /**
     * 批量关闭反馈
     *
     * @param ids    反馈 ID 列表
     * @param reason 关闭原因
     * @return 操作结果
     */
    @PostMapping("/batch-close")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> batchCloseFeedback(@RequestBody List<Long> ids, @RequestParam String reason) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(reason), ErrorCode.PARAMS_ERROR);

        feedbackService.batchCloseFeedback(ids, reason);
        return ResultUtils.success(true);
    }

    /**
     * 获取管理端反馈统计数据
     *
     * @return 反馈统计看板数据
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<FeedbackStatsVO> getAdminFeedbackStats() {
        FeedbackStatsVO result = feedbackService.getAdminFeedbackStats();
        return ResultUtils.success(result);
    }
}
