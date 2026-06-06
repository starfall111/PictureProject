package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.dto.report.ReportHandleDTO;
import org.example.pojo.dto.report.ReportQueryDTO;
import org.example.pojo.dto.report.ReportSubmitDTO;
import org.example.pojo.entity.Report;
import org.example.pojo.vo.report.ReportStatsVO;
import org.example.pojo.vo.report.ReportVO;

import java.util.List;

/**
 * 举报服务接口
 *
 * @author Zou
 */
public interface ReportService extends IService<Report> {

    /**
     * 用户提交举报
     */
    long submitReport(ReportSubmitDTO dto);

    /**
     * 用户举报列表
     */
    Page<ReportVO> getMyReportList(ReportQueryDTO dto);

    /**
     * 举报详情（用户端）
     */
    ReportVO getReportDetail(Long reportId);

    /**
     * 撤回举报
     */
    void cancelReport(Long reportId);

    /**
     * 管理端举报列表
     */
    Page<ReportVO> getAdminReportList(ReportQueryDTO dto);

    /**
     * 管理端举报详情
     */
    ReportVO getAdminReportDetail(Long reportId);

    /**
     * 审核处理举报
     */
    void handleReport(ReportHandleDTO dto);

    /**
     * 批量处理举报
     */
    void batchHandleReports(List<ReportHandleDTO> dtoList);

    /**
     * 获取管理端举报统计
     */
    ReportStatsVO getAdminReportStats();
}
