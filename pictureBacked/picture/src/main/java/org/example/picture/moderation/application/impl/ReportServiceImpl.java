package org.example.picture.moderation.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserContext;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.picture.moderation.interfaces.dto.ReportHandleDTO;
import org.example.picture.moderation.interfaces.dto.ReportQueryDTO;
import org.example.picture.moderation.interfaces.dto.ReportSubmitDTO;
import org.example.picture.core.domain.model.Picture;
import org.example.picture.moderation.domain.model.Report;
import org.example.identity.api.model.User;
import org.example.picture.moderation.interfaces.vo.ReportStatsVO;
import org.example.picture.moderation.interfaces.vo.ReportTargetVO;
import org.example.picture.moderation.interfaces.vo.ReportVO;

import org.example.picture.moderation.infrastructure.persistence.ReportMapper;
import org.example.picture.moderation.application.BanService;
import org.example.picture.core.application.PictureService;
import org.example.picture.moderation.application.ReportService;
import org.example.identity.application.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 举报服务实现（DB 直写版本，被 CachedReportServiceImpl 委托调用）
 *
 * @author Zou
 */
@Slf4j
@Service("dbReportService")
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report>
        implements ReportService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Qualifier("dbUserService")
    private UserService userService;

    @Resource
    @Qualifier("dbPictureService")
    private PictureService pictureService;

    @Resource
    private BanService banService;

    @Resource
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ==================== 合法的枚举值集合 ====================

    private static final Set<String> VALID_TARGET_TYPES = Set.of("PICTURE", "USER");
    private static final Set<String> VALID_REASON_TYPES = Set.of(
            "PORNOGRAPHIC", "VIOLENCE", "POLITICAL", "FRAUD",
            "COPYRIGHT", "SPAM", "HARASSMENT", "OTHER"
    );
    private static final Set<String> VALID_HANDLE_RESULTS = Set.of(
            "APPROVED", "REJECTED", "WARN_ONLY", "REMOVE_ONLY",
            "BAN_TEMP_3", "BAN_TEMP_7", "BAN_TEMP_30", "BAN_PERMANENT"
    );

    // ==================== 用户端方法 ====================

    @Override
    public long submitReport(ReportSubmitDTO dto) {
        // 1. 获取当前用户
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);
        Long currentUserId = currentUser.getId();

        // 2. 参数校验
        String targetType = dto.getTargetType();
        Long targetId = dto.getTargetId();
        String reasonType = dto.getReasonType();
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "举报目标类型不能为空");
        ThrowUtils.throwIf(ObjUtil.isEmpty(targetId), ErrorCode.PARAMS_ERROR, "举报目标ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(reasonType), ErrorCode.PARAMS_ERROR, "举报原因不能为空");
        ThrowUtils.throwIf(!VALID_TARGET_TYPES.contains(targetType), ErrorCode.PARAMS_ERROR, "非法的举报目标类型");
        ThrowUtils.throwIf(!VALID_REASON_TYPES.contains(reasonType), ErrorCode.PARAMS_ERROR, "非法的举报原因类型");

        // 3. 不能举报自己
        if ("USER".equals(targetType) && targetId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.REPORT_SELF);
        }

        // 4. 限流检查
        checkReportRateLimit(currentUserId);

        // 5. 防重复检查
        String dupKey = String.format(RedisKeyConstants.REPORT_DUPLICATE_KEY, currentUserId, targetType, targetId);
        Boolean isFirst = stringRedisTemplate.opsForValue().setIfAbsent(dupKey, "1",
                RedisKeyConstants.REPORT_DUPLICATE_TTL, TimeUnit.SECONDS);
        if (ObjUtil.isNotNull(isFirst) && !isFirst) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
        }

        // 6. 创建举报记录
        Report report = new Report();
        report.setReporterId(currentUserId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReasonType(reasonType);
        report.setDescription(dto.getDescription());
        report.setStatus("PENDING");
        report.setReportCount(0);
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        boolean saved = this.save(report);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "举报提交失败");

        log.info("用户 {} 提交举报: targetType={}, targetId={}, reportId={}", currentUserId, targetType, targetId, report.getId());

        // 加入今日新增脏集合
        try {
            stringRedisTemplate.opsForSet().add(RedisKeyConstants.REPORT_TODAY_DIRTY_KEY, String.valueOf(report.getId()));
        } catch (Exception e) {
            log.warn("写入举报今日新增脏集合失败: reportId={}", report.getId(), e);
        }

        return report.getId();
    }

    @Override
    public Page<ReportVO> getMyReportList(ReportQueryDTO dto) {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reporterId", currentUser.getId());
        queryWrapper.eq("isDelete", 0);

        // 可选过滤条件
        if (StrUtil.isNotBlank(dto.getStatus())) {
            queryWrapper.eq("status", dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getTargetType())) {
            queryWrapper.eq("targetType", dto.getTargetType());
        }

        queryWrapper.orderByDesc("createTime");

        Page<Report> reportPage = this.page(new Page<>(dto.getCurrent(), dto.getPageSize()), queryWrapper);
        return convertToVOPage(reportPage);
    }

    @Override
    public ReportVO getReportDetail(Long reportId) {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        Report report = this.getById(reportId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(report), ErrorCode.REPORT_TARGET_NOT_FOUND, "举报记录不存在");

        // 权限校验：举报人本人才能查看
        if (!report.getReporterId().equals(currentUser.getId())) {
            // 管理员也能查看，交给上层 Controller 校验角色
            ThrowUtils.throwIf(!"admin".equals(currentUser.getUserRole()), ErrorCode.NO_AUTH_ERROR);
        }

        return convertToVO(report);
    }

    @Override
    public void cancelReport(Long reportId) {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        Report report = this.getById(reportId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(report), ErrorCode.REPORT_TARGET_NOT_FOUND, "举报记录不存在");

        // 权限校验
        ThrowUtils.throwIf(!report.getReporterId().equals(currentUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只能撤回自己的举报");

        // 状态校验：只有 PENDING 可以撤回
        ThrowUtils.throwIf(!"PENDING".equals(report.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不允许撤回");

        // 时间校验：10 分钟内可以撤回
        long elapsed = System.currentTimeMillis() - report.getCreateTime().getTime();
        if (elapsed > 10 * 60 * 1000) {
            throw new BusinessException(ErrorCode.REPORT_CANCEL_TIMEOUT);
        }

        // 更新状态
        Report updateReport = new Report();
        updateReport.setId(reportId);
        updateReport.setStatus("REJECTED");
        updateReport.setHandleResult("CANCELLED");
        updateReport.setUpdateTime(new Date());
        boolean updated = this.updateById(updateReport);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "撤回举报失败");

        // 删除防重复 Redis key
        String dupKey = String.format(RedisKeyConstants.REPORT_DUPLICATE_KEY,
                report.getReporterId(), report.getTargetType(), report.getTargetId());
        stringRedisTemplate.delete(dupKey);

        log.info("用户 {} 撤回举报 reportId={}", currentUser.getId(), reportId);
    }

    // ==================== 管理端方法 ====================

    @Override
    public Page<ReportVO> getAdminReportList(ReportQueryDTO dto) {
        QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);

        if (StrUtil.isNotBlank(dto.getStatus())) {
            queryWrapper.eq("status", dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getTargetType())) {
            queryWrapper.eq("targetType", dto.getTargetType());
        }
        if (StrUtil.isNotBlank(dto.getReasonType())) {
            queryWrapper.eq("reasonType", dto.getReasonType());
        }
        if (StrUtil.isNotBlank(dto.getStartTime())) {
            queryWrapper.ge("createTime", dto.getStartTime());
        }
        if (StrUtil.isNotBlank(dto.getEndTime())) {
            queryWrapper.le("createTime", dto.getEndTime());
        }
        if (dto.getMinReportCount() != null && dto.getMinReportCount() > 0) {
            queryWrapper.ge("reportCount", dto.getMinReportCount());
        }

        // 优先展示举报次数多的、时间最近的
        queryWrapper.orderByDesc("reportCount", "createTime");

        Page<Report> reportPage = this.page(new Page<>(dto.getCurrent(), dto.getPageSize()), queryWrapper);
        Page<ReportVO> voPage = convertToVOPage(reportPage);

        // 填充列表中每条举报的目标信息
        voPage.getRecords().forEach(vo -> {
            if (vo.getTargetType() != null && vo.getTargetId() != null) {
                vo.setTargetInfo(buildTargetInfo(vo.getTargetType(), vo.getTargetId()));
            }
        });

        return voPage;
    }

    @Override
    public ReportVO getAdminReportDetail(Long reportId) {
        Report report = this.getById(reportId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(report), ErrorCode.REPORT_TARGET_NOT_FOUND, "举报记录不存在");

        ReportVO vo = convertToVO(report);

        // 填充目标详情信息
        vo.setTargetInfo(buildTargetInfo(report.getTargetType(), report.getTargetId()));

        return vo;
    }

    /**
     * 处理举报记录
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(ReportHandleDTO dto) {
        User currentAdmin = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentAdmin), ErrorCode.NOT_LOGIN_ERROR);

        Long reportId = dto.getReportId();
        String handleResult = dto.getHandleResult();
        ThrowUtils.throwIf(ObjUtil.isEmpty(reportId), ErrorCode.PARAMS_ERROR, "举报记录ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(handleResult), ErrorCode.PARAMS_ERROR, "处理结果不能为空");
        ThrowUtils.throwIf(!VALID_HANDLE_RESULTS.contains(handleResult), ErrorCode.PARAMS_ERROR, "非法的处理结果");

        Report report = this.getById(reportId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(report), ErrorCode.REPORT_TARGET_NOT_FOUND, "举报记录不存在");

        // 状态校验：允许 PENDING / PROCESSING / REJECTED 状态处理（REJECTED 允许改判）
        String currentStatus = report.getStatus();
        if (!"PENDING".equals(currentStatus) && !"PROCESSING".equals(currentStatus) && !"REJECTED".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前状态不允许处理");
        }

        // 构造更新实体
        Report updateReport = new Report();
        updateReport.setId(reportId);
        updateReport.setHandlerId(currentAdmin.getId());
        updateReport.setHandleResult(handleResult);
        updateReport.setHandleReason(dto.getHandleReason());
        updateReport.setHandleTime(new Date());
        updateReport.setUpdateTime(new Date());

        // 根据 handleResult 决定最终状态
        switch (handleResult) {
            case "APPROVED", "REMOVE_ONLY", "WARN_ONLY",
                 "BAN_TEMP_3", "BAN_TEMP_7", "BAN_TEMP_30", "BAN_PERMANENT" -> updateReport.setStatus("APPROVED");
            case "REJECTED" -> updateReport.setStatus("REJECTED");
            default -> updateReport.setStatus("PROCESSING");
        }

        boolean updated = this.updateById(updateReport);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "处理举报失败");

        // 封禁逻辑
        switch (handleResult) {
            case "BAN_TEMP_3" -> banService.executeBan(report.getTargetId(), "BAN_TEMP_3", 3,
                    dto.getHandleReason(), reportId);
            case "BAN_TEMP_7" -> banService.executeBan(report.getTargetId(), "BAN_TEMP_7", 7,
                    dto.getHandleReason(), reportId);
            case "BAN_TEMP_30" -> banService.executeBan(report.getTargetId(), "BAN_TEMP_30", 30,
                    dto.getHandleReason(), reportId);
            case "BAN_PERMANENT" -> banService.executeBan(report.getTargetId(), "BAN_PERMANENT", null,
                    dto.getHandleReason(), reportId);
            default -> { /* 无封禁操作 */ }
        }

        // 下架图片逻辑
        if ("REMOVE_ONLY".equals(handleResult) && "PICTURE".equals(report.getTargetType())) {
            removePicture(report.getTargetId(), currentAdmin.getId(), "因举报被下架");
            sendPictureRemovedNotification(report.getTargetId(), "因举报被下架");
        }
        // 封禁场景下，若目标是图片也一并处理
        if (handleResult.startsWith("BAN_") && "PICTURE".equals(report.getTargetType())) {
            removePicture(report.getTargetId(), currentAdmin.getId(), "因举报违规被下架");
            sendPictureRemovedNotification(report.getTargetId(), "因举报违规被下架");
        }

        // 警告通知
        if ("WARN_ONLY".equals(handleResult)) {
            sendWarnNotification(report, dto.getHandleReason(), currentAdmin);
        }

        // 递增该目标其他待处理举报的 reportCount
        this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Report>()
                .eq(Report::getId, reportId)
                .setSql("reportCount = reportCount + 1"));

        log.info("管理员 {} 处理举报 reportId={}, handleResult={}", currentAdmin.getId(), reportId, handleResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchHandleReports(List<ReportHandleDTO> dtoList) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dtoList), ErrorCode.PARAMS_ERROR, "处理列表不能为空");
        for (ReportHandleDTO dto : dtoList) {
            handleReport(dto);
        }
    }

    @Override
    public ReportStatsVO getAdminReportStats() {
        ReportStatsVO stats = new ReportStatsVO();

        // 待审核数
        long pendingCount = this.count(new QueryWrapper<Report>()
                .eq("status", "PENDING").eq("isDelete", 0));
        stats.setPendingCount(pendingCount);

        // 累计处理数（APPROVED + REJECTED）
        long totalHandledCount = this.count(new QueryWrapper<Report>()
                .in("status", "APPROVED", "REJECTED").eq("isDelete", 0));
        stats.setTotalHandledCount(totalHandledCount);

        // 今日新增数
        long todayNewCount = 0;
        try {
            String countStr = stringRedisTemplate.opsForValue().get(RedisKeyConstants.REPORT_TODAY_COUNT_KEY);
            if (StrUtil.isNotBlank(countStr)) {
                todayNewCount = Long.parseLong(countStr);
            } else {
                // 缓存中没有，从脏集合大小估算
                Long dirtySize = stringRedisTemplate.opsForSet().size(RedisKeyConstants.REPORT_TODAY_DIRTY_KEY);
                todayNewCount = dirtySize != null ? dirtySize : 0;
            }
        } catch (Exception e) {
            log.warn("读取今日新增举报数失败", e);
        }
        stats.setTodayNewCount(todayNewCount);

        return stats;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 举报限流检查
     */
    private void checkReportRateLimit(Long userId) {
        String rateLimitKey = String.format(RedisKeyConstants.REPORT_RATE_LIMIT_KEY, userId);
        Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(rateLimitKey, RedisKeyConstants.REPORT_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        if (count != null && count > RedisKeyConstants.REPORT_RATE_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.REPORT_RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 实体转 VO
     */
    private ReportVO convertToVO(Report report) {
        if (ObjUtil.isEmpty(report)) {
            return null;
        }
        ReportVO vo = new ReportVO();
        BeanUtil.copyProperties(report, vo);

        // 填充举报人信息
        fillReporterInfo(vo, report.getReporterId());

        // 状态描述
        vo.setStatusDesc(getStatusDesc(report.getStatus()));
        vo.setHandleResultDesc(getHandleResultDesc(report.getHandleResult()));

        return vo;
    }

    /**
     * 填充举报人信息
     */
    private void fillReporterInfo(ReportVO vo, Long reporterId) {
        if (ObjUtil.isEmpty(reporterId)) {
            return;
        }
        try {
            User reporter = userService.getById(reporterId);
            if (ObjUtil.isNotEmpty(reporter)) {
                vo.setReporterId(reporter.getId());
                vo.setReporterName(reporter.getUserName());
                vo.setReporterAvatar(reporter.getUserAvatar());
            }
        } catch (Exception e) {
            log.warn("查询举报人信息失败: reporterId={}", reporterId, e);
        }
    }

    /**
     * 构建举报目标信息
     */
    private ReportTargetVO buildTargetInfo(String targetType, Long targetId) {
        ReportTargetVO targetVO = new ReportTargetVO();
        targetVO.setType(targetType);
        targetVO.setId(targetId);

        try {
            if ("PICTURE".equals(targetType)) {
                Picture picture = pictureService.getById(targetId);
                if (ObjUtil.isNotEmpty(picture)) {
                    targetVO.setTitle(picture.getName());
                    targetVO.setThumbnailUrl(picture.getThumbnailUrl());
                    targetVO.setAuthorId(picture.getUserId());
                    // 填充作者名
                    if (picture.getUserId() != null) {
                        User author = userService.getById(picture.getUserId());
                        if (ObjUtil.isNotEmpty(author)) {
                            targetVO.setAuthorName(author.getUserName());
                        }
                    }
                }
            } else if ("USER".equals(targetType)) {
                User targetUser = userService.getById(targetId);
                if (ObjUtil.isNotEmpty(targetUser)) {
                    targetVO.setTitle(targetUser.getUserName());
                    targetVO.setThumbnailUrl(targetUser.getUserAvatar());
                    targetVO.setAuthorId(targetUser.getId());
                    targetVO.setAuthorName(targetUser.getUserName());
                }
            }
        } catch (Exception e) {
            log.warn("查询举报目标信息失败: targetType={}, targetId={}", targetType, targetId, e);
        }

        return targetVO;
    }

    /**
     * 下架图片
     */
    private void removePicture(Long pictureId, Long reviewerId, String reviewMessage) {
        try {
            Picture updatePic = new Picture();
            updatePic.setId(pictureId);
            updatePic.setReviewStatus(2); // 2=拒绝/下架
            updatePic.setReviewMessage(reviewMessage);
            updatePic.setReviewerId(reviewerId);
            updatePic.setReviewTime(new Date());
            pictureService.updateById(updatePic);
            log.info("图片已下架: pictureId={}, reason={}", pictureId, reviewMessage);
        } catch (Exception e) {
            log.error("下架图片失败: pictureId={}", pictureId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下架图片失败");
        }
    }

    /**
     * 发送图片下架通知给作者
     */
    private void sendPictureRemovedNotification(Long pictureId, String reason) {
        try {
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture) || picture.getUserId() == null) {
                return;
            }
            String content = "您的图片「" + (picture.getName() != null ? picture.getName() : pictureId) + "」已被下架。原因：" + reason;
            eventPublisher.publishEvent(new org.example.shared.contract.NotificationEvent(
                    this, picture.getUserId(), 0L, "系统", null,
                    org.example.shared.contract.NotificationTypeEnum.PICTURE_REMOVED,
                    "图片下架通知", content, null, "/picture/" + pictureId
            ));
        } catch (Exception e) {
            log.warn("发送图片下架通知失败: pictureId={}", pictureId, e);
        }
    }

    /**
     * 发送警告通知给被举报用户
     */
    private void sendWarnNotification(Report report, String reason, User admin) {
        try {
            Long targetUserId;
            if ("USER".equals(report.getTargetType())) {
                targetUserId = report.getTargetId();
            } else if ("PICTURE".equals(report.getTargetType())) {
                Picture picture = pictureService.getById(report.getTargetId());
                targetUserId = picture != null ? picture.getUserId() : null;
            } else {
                return;
            }
            if (targetUserId == null) {
                return;
            }
            String content = "您收到一条系统警告。原因：" + (StrUtil.isNotBlank(reason) ? reason : "内容违规");
            eventPublisher.publishEvent(new org.example.shared.contract.NotificationEvent(
                    this, targetUserId, admin.getId(), admin.getUserName(), admin.getUserAvatar(),
                    org.example.shared.contract.NotificationTypeEnum.USER_WARNED,
                    "系统警告", content, report.getId(), null
            ));
        } catch (Exception e) {
            log.warn("发送警告通知失败: reportId={}", report.getId(), e);
        }
    }

    /**
     * 分页实体转分页 VO
     */
    private Page<ReportVO> convertToVOPage(Page<Report> reportPage) {
        Page<ReportVO> voPage = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        List<ReportVO> voList = reportPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(String status) {
        if (ObjUtil.isEmpty(status)) return "";
        return switch (status) {
            case "PENDING" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }

    /**
     * 获取处理结果描述
     */
    private String getHandleResultDesc(String handleResult) {
        if (ObjUtil.isEmpty(handleResult)) return "";
        return switch (handleResult) {
            case "APPROVED" -> "举报成立";
            case "REJECTED" -> "举报驳回";
            case "WARN_ONLY" -> "仅警告";
            case "REMOVE_ONLY" -> "仅下架";
            case "BAN_TEMP_3" -> "封禁3天";
            case "BAN_TEMP_7" -> "封禁7天";
            case "BAN_TEMP_30" -> "封禁30天";
            case "BAN_PERMANENT" -> "永久封禁";
            case "CANCELLED" -> "已撤回";
            default -> handleResult;
        };
    }
}
