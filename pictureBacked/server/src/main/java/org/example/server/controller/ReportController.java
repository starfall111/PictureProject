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
import org.example.pojo.dto.report.ReportQueryDTO;
import org.example.pojo.dto.report.ReportSubmitDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.report.ReportVO;
import org.example.server.service.ReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 用户端 — 举报 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    @Qualifier("dbReportService")
    private ReportService reportService;

    /**
     * 提交举报
     *
     * @param dto 举报提交参数
     * @return 举报记录 ID
     */
    @PostMapping("/submit")
    @RateLimit(resource = "report.submit", dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 3)
    public BaseResponse<Long> submitReport(@RequestBody ReportSubmitDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        long reportId = reportService.submitReport(dto);
        return ResultUtils.success(reportId);
    }

    /**
     * 获取个人举报列表
     *
     * @param dto 分页查询参数
     * @return 举报分页列表
     */
    @GetMapping("/my-list")
    @RateLimit(resource = "report.myList", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Page<ReportVO>> getMyReportList(ReportQueryDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        Page<ReportVO> result = reportService.getMyReportList(dto);
        return ResultUtils.success(result);
    }

    /**
     * 获取举报详情
     *
     * @param reportId 举报记录 ID
     * @return 举报详情
     */
    @GetMapping("/detail/{reportId}")
    @RateLimit(resource = "report.detail", dimensions = {RateLimitDimension.USER})
    public BaseResponse<ReportVO> getReportDetail(@PathVariable Long reportId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(reportId), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        ReportVO result = reportService.getReportDetail(reportId);
        return ResultUtils.success(result);
    }

    /**
     * 撤回举报（10 分钟内 PENDING 状态可撤回）
     *
     * @param reportId 举报记录 ID
     * @return 操作结果
     */
    @PostMapping("/cancel")
    @RateLimit(resource = "report.cancel", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Boolean> cancelReport(@RequestParam Long reportId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(reportId), ErrorCode.PARAMS_ERROR);

        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        reportService.cancelReport(reportId);
        return ResultUtils.success(true);
    }
}
