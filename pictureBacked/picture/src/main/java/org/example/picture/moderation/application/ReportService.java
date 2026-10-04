package org.example.picture.moderation.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.moderation.interfaces.dto.ReportHandleDTO;
import org.example.picture.moderation.interfaces.dto.ReportQueryDTO;
import org.example.picture.moderation.interfaces.dto.ReportSubmitDTO;
import org.example.picture.moderation.domain.model.Report;
import org.example.picture.moderation.interfaces.vo.ReportStatsVO;
import org.example.picture.moderation.interfaces.vo.ReportVO;

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
