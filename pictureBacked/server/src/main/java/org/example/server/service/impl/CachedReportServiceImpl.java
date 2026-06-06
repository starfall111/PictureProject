package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.report.ReportHandleDTO;
import org.example.pojo.dto.report.ReportQueryDTO;
import org.example.pojo.dto.report.ReportSubmitDTO;
import org.example.pojo.entity.Report;
import org.example.pojo.vo.report.ReportStatsVO;
import org.example.pojo.vo.report.ReportVO;
import org.example.server.mapper.ReportMapper;
import org.example.server.service.ReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存版举报服务实现
 * - 举报详情 → Redis 缓存，TTL 15-30min
 * - 举报统计 → Redis 缓存，TTL 5min
 * - 写操作 → 委托 dbReportService + 主动失效缓存
 *
 * @author Zou
 */
@Slf4j
@Service("cachedReportService")
public class CachedReportServiceImpl extends ServiceImpl<ReportMapper, Report>
        implements ReportService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    @Qualifier("dbReportService")
    private ReportService dbReportService;

    // ==================== 缓存读方法 ====================

    @Override
    public ReportVO getReportDetail(Long reportId) {
        String cacheKey = "report:detail:" + reportId;
        int ttlSeconds = 900 + RandomUtil.randomInt(0, 900);

        String json = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            ReportVO vo = dbReportService.getReportDetail(reportId);
            return vo != null ? JSONUtil.toJsonStr(vo) : null;
        });

        return json != null ? JSONUtil.toBean(json, ReportVO.class) : null;
    }

    @Override
    public ReportVO getAdminReportDetail(Long reportId) {
        // 管理端详情不走缓存，实时性要求更高
        return dbReportService.getAdminReportDetail(reportId);
    }

    @Override
    public ReportStatsVO getAdminReportStats() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.REPORT_STATS_KEY);
            if (cached != null) {
                return JSONUtil.toBean(cached, ReportStatsVO.class);
            }
        } catch (Exception e) {
            log.warn("读取举报统计缓存失败", e);
        }

        ReportStatsVO stats = dbReportService.getAdminReportStats();

        try {
            int ttlSeconds = RedisKeyConstants.REPORT_STATS_TTL + RandomUtil.randomInt(0, 60);
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.REPORT_STATS_KEY,
                    JSONUtil.toJsonStr(stats), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入举报统计缓存失败", e);
        }

        return stats;
    }

    // ==================== 写方法（委托 + 缓存失效）====================

    @Override
    public long submitReport(ReportSubmitDTO dto) {
        return dbReportService.submitReport(dto);
    }

    @Override
    public Page<ReportVO> getMyReportList(ReportQueryDTO dto) {
        return dbReportService.getMyReportList(dto);
    }

    @Override
    public void cancelReport(Long reportId) {
        dbReportService.cancelReport(reportId);
        invalidateReportCache(reportId);
        invalidateReportStatsCache();
    }

    @Override
    public Page<ReportVO> getAdminReportList(ReportQueryDTO dto) {
        return dbReportService.getAdminReportList(dto);
    }

    @Override
    public void handleReport(ReportHandleDTO dto) {
        dbReportService.handleReport(dto);
        invalidateReportCache(dto.getReportId());
        invalidateReportStatsCache();
    }

    @Override
    public void batchHandleReports(List<ReportHandleDTO> dtoList) {
        dbReportService.batchHandleReports(dtoList);
        dtoList.forEach(dto -> invalidateReportCache(dto.getReportId()));
        invalidateReportStatsCache();
    }

    // ==================== 缓存失效方法 ====================

    private void invalidateReportCache(Long reportId) {
        if (reportId == null) {
            return;
        }
        String cacheKey = "report:detail:" + reportId;
        stringRedisTemplate.delete(cacheKey);
        log.debug("Invalidated report cache for reportId: {}", reportId);
    }

    private void invalidateReportStatsCache() {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.REPORT_STATS_KEY);
            log.debug("Invalidated report stats cache");
        } catch (Exception e) {
            log.warn("失效举报统计缓存失败", e);
        }
    }
}
