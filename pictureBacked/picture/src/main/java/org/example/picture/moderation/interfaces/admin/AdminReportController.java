package org.example.picture.moderation.interfaces.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.moderation.interfaces.dto.ReportHandleDTO;
import org.example.picture.moderation.interfaces.dto.ReportQueryDTO;
import org.example.picture.moderation.interfaces.vo.ReportStatsVO;
import org.example.picture.moderation.interfaces.vo.ReportVO;
import org.example.picture.moderation.application.ReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 管理端 — 举报管理 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/report")
public class AdminReportController {

    @Resource
    @Qualifier("dbReportService")
    private ReportService reportService;

    /**
     * 获取举报列表（按被举报次数排序）
     *
     * @param dto 分页查询参数（支持状态、目标类型筛选）
     * @return 举报分页列表
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<ReportVO>> getAdminReportList(ReportQueryDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        Page<ReportVO> result = reportService.getAdminReportList(dto);
        return ResultUtils.success(result);
    }

    /**
     * 获取举报详情（含目标信息、关联举报）
     *
     * @param reportId 举报记录 ID
     * @return 举报详情
     */
    @GetMapping("/detail/{reportId}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<ReportVO> getAdminReportDetail(@PathVariable Long reportId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(reportId), ErrorCode.PARAMS_ERROR);

        ReportVO result = reportService.getAdminReportDetail(reportId);
        return ResultUtils.success(result);
    }

    /**
     * 审核处理举报（可触发封禁）
     *
     * @param dto 举报处理参数
     * @return 操作结果
     */
    @PostMapping("/handle")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> handleReport(@RequestBody ReportHandleDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        reportService.handleReport(dto);
        return ResultUtils.success(true);
    }

    /**
     * 批量处理举报
     *
     * @param dtoList 举报处理参数列表
     * @return 操作结果
     */
    @PostMapping("/batch-handle")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> batchHandleReports(@RequestBody List<ReportHandleDTO> dtoList) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dtoList), ErrorCode.PARAMS_ERROR);

        reportService.batchHandleReports(dtoList);
        return ResultUtils.success(true);
    }

    /**
     * 获取举报统计数据
     *
     * @return 举报统计信息（待审核数、今日新增数、累计处理数）
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<ReportStatsVO> getAdminReportStats() {
        ReportStatsVO result = reportService.getAdminReportStats();
        return ResultUtils.success(result);
    }
}
