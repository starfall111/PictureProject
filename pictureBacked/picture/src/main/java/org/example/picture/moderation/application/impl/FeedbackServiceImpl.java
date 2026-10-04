package org.example.picture.moderation.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserConstant;
import org.example.identity.api.UserContext;
import org.example.picture.moderation.domain.enums.FeedbackCloseReasonEnum;
import org.example.picture.moderation.domain.enums.FeedbackPriorityEnum;
import org.example.picture.moderation.domain.enums.FeedbackReplyTypeEnum;
import org.example.picture.moderation.domain.enums.FeedbackSourceEnum;
import org.example.picture.moderation.domain.enums.FeedbackStatusEnum;
import org.example.picture.moderation.domain.enums.FeedbackTypeEnum;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.AliOssUtil;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.moderation.interfaces.dto.*;
import org.example.picture.moderation.interfaces.dto.ReportSubmitDTO;
import org.example.picture.moderation.domain.model.Feedback;
import org.example.picture.moderation.domain.model.FeedbackAttachment;
import org.example.picture.moderation.domain.model.FeedbackReply;
import org.example.picture.moderation.domain.model.FeedbackStatusLog;
import org.example.picture.moderation.domain.model.Report;
import org.example.identity.api.model.User;
import org.example.picture.moderation.interfaces.vo.*;
import org.example.picture.moderation.infrastructure.persistence.FeedbackAttachmentMapper;
import org.example.picture.moderation.infrastructure.persistence.FeedbackMapper;
import org.example.picture.moderation.infrastructure.persistence.FeedbackReplyMapper;
import org.example.picture.moderation.infrastructure.persistence.FeedbackStatusLogMapper;
import org.example.picture.moderation.application.FeedbackService;
import org.example.picture.moderation.application.ReportService;
import org.example.identity.application.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("dbFeedbackService")
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    @Qualifier("dbUserService")
    private UserService userService;

    @Resource
    @Qualifier("dbReportService")
    private ReportService reportService;

    @Resource
    private FeedbackReplyMapper feedbackReplyMapper;

    @Resource
    private FeedbackStatusLogMapper feedbackStatusLogMapper;

    @Resource
    private FeedbackAttachmentMapper feedbackAttachmentMapper;

    // ==================== 用户端接口 ====================

    /**
     * 反馈附件上传
     * @param file
     * @return
     */
    @Override
    public FeedbackAttachmentVO uploadAttachment(MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请选择文件");
        ThrowUtils.throwIf(file.getSize() > 5 * 1024 * 1024, ErrorCode.FEEDBACK_ATTACHMENT_TOO_LARGE);

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase() : "";
        Set<String> allowedTypes = Set.of("PNG", "JPG", "JPEG", "GIF");
        ThrowUtils.throwIf(!allowedTypes.contains(ext), ErrorCode.PARAMS_ERROR, "仅支持 PNG/JPG/JPEG/GIF 格式");

        try {
            String url = aliOssUtil.upload(file.getBytes(), "feedback/" + originalFilename + "/");

            FeedbackAttachment attachment = new FeedbackAttachment();
            attachment.setFeedbackId(0L); // 占位，提交时关联
            attachment.setFileUrl(url);
            attachment.setFileName(originalFilename);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(ext);
            attachment.setSortOrder(0);
            attachment.setIsDelete(0);
            feedbackAttachmentMapper.insert(attachment);

            FeedbackAttachmentVO vo = new FeedbackAttachmentVO();
            vo.setId(attachment.getId());
            vo.setFileUrl(url);
            vo.setFileName(originalFilename);
            vo.setFileSize(file.getSize());
            vo.setFileType(ext);
            return vo;
        } catch (Exception e) {
            log.error("上传反馈附件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    /**
     * 提交反馈
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long submitFeedback(FeedbackSubmitDTO dto) {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getContent()), ErrorCode.PARAMS_ERROR, "内容不能为空");
        ThrowUtils.throwIf(FeedbackTypeEnum.getByType(dto.getType()) == null, ErrorCode.PARAMS_ERROR, "无效的反馈类型");

        Long userId = currentUser.getId();

        // 限流：每天5条
        String countKey = RedisKeyConstants.FEEDBACK_SUBMIT_COUNT + userId;
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        ThrowUtils.throwIf(count >= 5, ErrorCode.FEEDBACK_RATE_LIMIT);

        // 防重锁（RLock 看门狗续期）
        String lockKey = RedisKeyConstants.LOCK_FEEDBACK_SUBMIT + userId;
        return redisCacheUtil.executeWithLock(lockKey, () -> {
            Feedback feedback = new Feedback();
            feedback.setTitle(dto.getTitle());
            feedback.setContent(dto.getContent());
            feedback.setType(dto.getType());
            feedback.setPriority(FeedbackPriorityEnum.P3.getType());
            feedback.setStatus(FeedbackStatusEnum.PENDING.getType());
            feedback.setSource(FeedbackSourceEnum.APP.getType());
            feedback.setRelatedPictureId(dto.getRelatedPictureId());
            feedback.setRelatedUserId(dto.getRelatedUserId());
            feedback.setIsAnonymous(Boolean.TRUE.equals(dto.getIsAnonymous()) ? 1 : 0);
            feedback.setReopenCount(0);
            feedback.setIsDelete(0);

            if (Boolean.TRUE.equals(dto.getIsAnonymous())) {
                feedback.setUserId(null);
            } else {
                feedback.setUserId(userId);
            }

            this.save(feedback);

            // 关联附件
            if (dto.getAttachmentIds() != null && !dto.getAttachmentIds().isEmpty()) {
                ThrowUtils.throwIf(dto.getAttachmentIds().size() > 3, ErrorCode.FEEDBACK_ATTACHMENT_LIMIT);
                for (Long attachmentId : dto.getAttachmentIds()) {
                    FeedbackAttachment att = feedbackAttachmentMapper.selectById(attachmentId);
                    if (att != null && att.getFeedbackId().equals(0L)) {
                        att.setFeedbackId(feedback.getId());
                        feedbackAttachmentMapper.updateById(att);
                    }
                }
            }

            // 状态日志
            saveStatusLog(feedback.getId(), null, FeedbackStatusEnum.PENDING.getType(), userId, "USER", null);

            // 递增限流计数
            Long newCount = stringRedisTemplate.opsForValue().increment(countKey);
            if (newCount != null && newCount == 1L) {
                stringRedisTemplate.expire(countKey, RedisKeyConstants.FEEDBACK_SUBMIT_COUNT_TTL, TimeUnit.SECONDS);
            }

            log.info("用户 {} 提交反馈 {}", userId, feedback.getId());
            return feedback.getId();
        }, ErrorCode.FEEDBACK_DUPLICATE, "请勿重复提交");
    }

    /**
     * 撤回反馈
     * @param feedbackId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawFeedback(Long feedbackId) {
        User currentUser = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!currentUser.getId().equals(feedback.getUserId()), ErrorCode.FEEDBACK_NO_PERMISSION);
        ThrowUtils.throwIf(!FeedbackStatusEnum.PENDING.getType().equals(feedback.getStatus()), ErrorCode.FEEDBACK_STATUS_ERROR);

        feedback.setStatus(FeedbackStatusEnum.CLOSED.getType());
        feedback.setCloseReason(FeedbackCloseReasonEnum.USER_WITHDRAW.getType());
        this.updateById(feedback);

        saveStatusLog(feedbackId, FeedbackStatusEnum.PENDING.getType(), FeedbackStatusEnum.CLOSED.getType(), currentUser.getId(), "USER", null);
    }

    /**
     * 普通用户查看反馈列表
     * @param dto
     * @return
     */
    @Override
    public Page<FeedbackListItemVO> getMyFeedbackList(FeedbackQueryDTO dto) {
        User currentUser = UserContext.get();
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUserId, currentUser.getId())
                .eq(Feedback::getIsDelete, 0);
        if (ObjUtil.isNotEmpty(dto.getStatus())) {
            wrapper.eq(Feedback::getStatus, dto.getStatus());
        }
        if (ObjUtil.isNotEmpty(dto.getType())) {
            wrapper.eq(Feedback::getType, dto.getType());
        }
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = this.page(new Page<>(dto.getCurrent(), dto.getPageSize()), wrapper);
        return convertToListItemPage(page);
    }

    /**
     * 获取反馈详情（含回复、状态日志、附件）
     * @param feedbackId
     * @return
     */
    @Override
    public FeedbackVO getFeedbackDetail(Long feedbackId) {
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);

        User currentUser = UserContext.get();
        boolean isAdmin = UserConstant.ADMIN_AUTH_ROLE.equals(currentUser.getUserRole());
        boolean isOwner = currentUser.getId().equals(feedback.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isOwner, ErrorCode.FEEDBACK_NO_PERMISSION);

        return buildFeedbackVO(feedback);
    }

    /**
     * 管理员与用户回复反馈
     * @param feedbackId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyFeedback(Long feedbackId, FeedbackReplyDTO dto) {
        User currentUser = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!currentUser.getId().equals(feedback.getUserId()), ErrorCode.FEEDBACK_NO_PERMISSION);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getContent()), ErrorCode.PARAMS_ERROR, "回复内容不能为空");

        FeedbackReply reply = new FeedbackReply();
        reply.setFeedbackId(feedbackId);
        reply.setUserId(currentUser.getId());
        reply.setReplyType(FeedbackReplyTypeEnum.USER_REPLY.getType());
        reply.setContent(dto.getContent());
        reply.setIsDelete(0);
        feedbackReplyMapper.insert(reply);

        if (FeedbackStatusEnum.PENDING.getType().equals(feedback.getStatus())) {
            feedback.setStatus(FeedbackStatusEnum.PROCESSING.getType());
            this.updateById(feedback);
            saveStatusLog(feedbackId, FeedbackStatusEnum.PENDING.getType(), FeedbackStatusEnum.PROCESSING.getType(), currentUser.getId(), "USER", null);
        }
    }

    /**
     * 重新打开反馈
     * @param feedbackId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenFeedback(Long feedbackId) {
        User currentUser = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!currentUser.getId().equals(feedback.getUserId()), ErrorCode.FEEDBACK_NO_PERMISSION);

        String status = feedback.getStatus();
        // 当反馈状态为已解决或已拒绝时，才能重新打开
        boolean canReopen = FeedbackStatusEnum.RESOLVED.getType().equals(status)
                || FeedbackStatusEnum.REJECTED.getType().equals(status);
        ThrowUtils.throwIf(!canReopen, ErrorCode.FEEDBACK_STATUS_ERROR);
        ThrowUtils.throwIf(feedback.getReopenCount() >= 2, ErrorCode.FEEDBACK_REOPEN_LIMIT);

        feedback.setStatus(FeedbackStatusEnum.REOPENED.getType());
        feedback.setReopenCount(feedback.getReopenCount() + 1);
        this.updateById(feedback);

        saveStatusLog(feedbackId, status, FeedbackStatusEnum.REOPENED.getType(), currentUser.getId(), "USER", null);
    }

    /**
     * 关闭反馈
     * @param feedbackId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmFeedback(Long feedbackId) {
        User currentUser = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!currentUser.getId().equals(feedback.getUserId()), ErrorCode.FEEDBACK_NO_PERMISSION);
        ThrowUtils.throwIf(!FeedbackStatusEnum.RESOLVED.getType().equals(feedback.getStatus()), ErrorCode.FEEDBACK_STATUS_ERROR);

        feedback.setStatus(FeedbackStatusEnum.CLOSED.getType());
        feedback.setCloseReason(FeedbackCloseReasonEnum.USER_CONFIRM.getType());
        this.updateById(feedback);

        saveStatusLog(feedbackId, FeedbackStatusEnum.RESOLVED.getType(), FeedbackStatusEnum.CLOSED.getType(), currentUser.getId(), "USER", null);
    }

    /**
     * 获取用户反馈操作统计
     * @return
     */
    @Override
    public FeedbackStatsVO getMyFeedbackStats() {
        User currentUser = UserContext.get();
        return buildStatsVO(currentUser.getId());
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理员获取反馈列表
     * @param dto
     * @return
     */
    @Override
    public Page<FeedbackListItemVO> getAdminFeedbackList(FeedbackQueryDTO dto) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getIsDelete, 0);
        if (ObjUtil.isNotEmpty(dto.getStatus())) {
            wrapper.eq(Feedback::getStatus, dto.getStatus());
        }
        if (ObjUtil.isNotEmpty(dto.getType())) {
            wrapper.eq(Feedback::getType, dto.getType());
        }
        if (ObjUtil.isNotEmpty(dto.getPriority())) {
            wrapper.eq(Feedback::getPriority, dto.getPriority());
        }
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w.like(Feedback::getTitle, dto.getKeyword())
                    .or().like(Feedback::getContent, dto.getKeyword()));
        }
        if (StrUtil.isNotBlank(dto.getStartTime())) {
            wrapper.ge(Feedback::getCreateTime, dto.getStartTime());
        }
        if (StrUtil.isNotBlank(dto.getEndTime())) {
            wrapper.le(Feedback::getCreateTime, dto.getEndTime());
        }
        // 按优先级排序
        if (ObjUtil.isNotEmpty(dto.getPriority())) {
            wrapper.orderByAsc(Feedback::getPriority);
        }
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = this.page(new Page<>(dto.getCurrent(), dto.getPageSize()), wrapper);
        return convertToListItemPage(page);
    }

    /**
     * 管理员查看反馈详情
     * @param feedbackId
     * @return
     */
    @Override
    public FeedbackVO getAdminFeedbackDetail(Long feedbackId) {
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        return buildFeedbackVO(feedback);
    }

    /**
     * 管理员认领反馈
     * @param feedbackId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimFeedback(Long feedbackId) {
        User admin = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!FeedbackStatusEnum.PENDING.getType().equals(feedback.getStatus()), ErrorCode.FEEDBACK_STATUS_ERROR);

        feedback.setHandlerId(admin.getId());
        feedback.setStatus(FeedbackStatusEnum.PROCESSING.getType());
        this.updateById(feedback);

        saveStatusLog(feedbackId, FeedbackStatusEnum.PENDING.getType(), FeedbackStatusEnum.PROCESSING.getType(), admin.getId(), "ADMIN", "管理员认领");
    }

    /**
     * 管理转派反馈为举报
     * @param feedbackId
     * @param targetHandlerId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferFeedback(Long feedbackId, Long targetHandlerId) {
        User admin = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);

        feedback.setHandlerId(targetHandlerId);
        this.updateById(feedback);

        saveStatusLog(feedbackId, feedback.getStatus(), feedback.getStatus(), admin.getId(), "ADMIN", "转派给管理员ID:" + targetHandlerId);
    }

    /**
     * 管理员回复反馈
     * @param feedbackId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminReplyFeedback(Long feedbackId, FeedbackReplyDTO dto) {
        User admin = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getContent()), ErrorCode.PARAMS_ERROR);

        FeedbackReply reply = new FeedbackReply();
        reply.setFeedbackId(feedbackId);
        reply.setUserId(admin.getId());
        reply.setReplyType(FeedbackReplyTypeEnum.ADMIN_REPLY.getType());
        reply.setContent(dto.getContent());
        reply.setIsDelete(0);
        feedbackReplyMapper.insert(reply);

        if (FeedbackStatusEnum.PENDING.getType().equals(feedback.getStatus())) {
            feedback.setHandlerId(admin.getId());
            feedback.setStatus(FeedbackStatusEnum.PROCESSING.getType());
            this.updateById(feedback);
            saveStatusLog(feedbackId, FeedbackStatusEnum.PENDING.getType(), FeedbackStatusEnum.PROCESSING.getType(), admin.getId(), "ADMIN", null);
        }
    }

    /**
     * 没看懂
     * @param feedbackId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addInternalNote(Long feedbackId, FeedbackReplyDTO dto) {
        User admin = UserContext.get();
        FeedbackReply reply = new FeedbackReply();
        reply.setFeedbackId(feedbackId);
        reply.setUserId(admin.getId());
        reply.setReplyType(FeedbackReplyTypeEnum.INTERNAL_NOTE.getType());
        reply.setContent(dto.getContent());
        reply.setIsDelete(0);
        feedbackReplyMapper.insert(reply);
    }

    /**
     * 反馈内容无营养 拒绝处理——这个应该不会使用
     * @param feedbackId
     * @param reason
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFeedback(Long feedbackId, String reason) {
        User admin = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);

        String currentStatus = feedback.getStatus();
        if (FeedbackStatusEnum.REJECTED.getType().equals(currentStatus)
                || FeedbackStatusEnum.CLOSED.getType().equals(currentStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前状态为" + currentStatus + "，不能重复操作");
        }

        feedback.setStatus(FeedbackStatusEnum.REJECTED.getType());
        this.updateById(feedback);

        saveStatusLog(feedbackId, currentStatus, FeedbackStatusEnum.REJECTED.getType(), admin.getId(), "ADMIN", reason);
    }

    /**
     * 调整反馈优先级
     * @param feedbackId
     * @param priority
     */
    @Override
    public void updatePriority(Long feedbackId, String priority) {
        ThrowUtils.throwIf(FeedbackPriorityEnum.getByType(priority) == null, ErrorCode.PARAMS_ERROR);
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        feedback.setPriority(priority);
        this.updateById(feedback);
    }

    /**
     * 转派至举报模块
     * @param feedbackId
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long convertToReport(Long feedbackId, FeedbackConvertDTO dto) {
        User admin = UserContext.get();
        Feedback feedback = this.getById(feedbackId);
        ThrowUtils.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);
        ThrowUtils.throwIf(!FeedbackStatusEnum.PENDING.getType().equals(feedback.getStatus())
                && !FeedbackStatusEnum.PROCESSING.getType().equals(feedback.getStatus()), ErrorCode.FEEDBACK_STATUS_ERROR);

        ReportSubmitDTO reportDTO = new ReportSubmitDTO();
        reportDTO.setTargetType(dto.getTargetType());
        reportDTO.setTargetId(dto.getTargetId());
        reportDTO.setReasonType(dto.getReasonType());
        reportDTO.setDescription(dto.getDescription());

        long reportId = reportService.submitReport(reportDTO);

        // 补充关联信息：更新 report 的 sourceFeedbackId
        Report report = reportService.getById(reportId);
        if (report != null) {
            report.setSourceFeedbackId(feedbackId);
            reportService.updateById(report);
        }

        feedback.setConvertedReportId(reportId);
        feedback.setStatus(FeedbackStatusEnum.CLOSED.getType());
        feedback.setCloseReason(FeedbackCloseReasonEnum.ADMIN_CLOSE.getType());
        this.updateById(feedback);

        saveStatusLog(feedbackId, feedback.getStatus(), FeedbackStatusEnum.CLOSED.getType(), admin.getId(), "ADMIN", "转为举报工单 #" + reportId);

        return reportId;
    }

    /**
     * 批量关闭反馈
     * @param feedbackIds
     * @param reason
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCloseFeedback(List<Long> feedbackIds, String reason) {
        User admin = UserContext.get();
        for (Long feedbackId : feedbackIds) {
            Feedback feedback = this.getById(feedbackId);
            if (feedback != null && !FeedbackStatusEnum.CLOSED.getType().equals(feedback.getStatus())) {
                String fromStatus = feedback.getStatus();
                feedback.setStatus(FeedbackStatusEnum.CLOSED.getType());
                feedback.setCloseReason(FeedbackCloseReasonEnum.ADMIN_CLOSE.getType());
                this.updateById(feedback);
                saveStatusLog(feedbackId, fromStatus, FeedbackStatusEnum.CLOSED.getType(), admin.getId(), "ADMIN", reason);
            }
        }
    }

    /**
     * 获取管理员未处理反馈数据
     * @return
     */
    @Override
    public FeedbackStatsVO getAdminFeedbackStats() {
        return buildStatsVO(null);
    }

    // ==================== 私有方法 ====================

    /**
     * 反馈日志记录
     * @param feedbackId
     * @param fromStatus
     * @param toStatus
     * @param operatorId
     * @param operatorType
     * @param remark
     */
    private void saveStatusLog(Long feedbackId, String fromStatus, String toStatus, Long operatorId, String operatorType, String remark) {
        FeedbackStatusLog statusLog = new FeedbackStatusLog();
        statusLog.setFeedbackId(feedbackId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorType(operatorType);
        statusLog.setRemark(remark);
        feedbackStatusLogMapper.insert(statusLog);
    }

    /**
     * 封装反馈数据详情
     * @param feedback
     * @return
     */
    private FeedbackVO buildFeedbackVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtil.copyProperties(feedback, vo);

        // 枚举描述
        FeedbackTypeEnum typeEnum = FeedbackTypeEnum.getByType(feedback.getType());
        vo.setTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        FeedbackStatusEnum statusEnum = FeedbackStatusEnum.getByType(feedback.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");

        // 用户信息（非匿名）
        if (!Integer.valueOf(1).equals(feedback.getIsAnonymous()) && feedback.getUserId() != null) {
            User user = userService.getById(feedback.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
        }

        // 回复列表
        List<FeedbackReply> replies = feedbackReplyMapper.selectList(
                new LambdaQueryWrapper<FeedbackReply>()
                        .eq(FeedbackReply::getFeedbackId, feedback.getId())
                        .eq(FeedbackReply::getIsDelete, 0)
                        .orderByAsc(FeedbackReply::getCreateTime));
        vo.setReplies(replies.stream().map(this::convertReplyToVO).collect(Collectors.toList()));

        // 状态日志
        List<FeedbackStatusLog> logs = feedbackStatusLogMapper.selectList(
                new LambdaQueryWrapper<FeedbackStatusLog>()
                        .eq(FeedbackStatusLog::getFeedbackId, feedback.getId())
                        .orderByAsc(FeedbackStatusLog::getCreateTime));
        vo.setStatusLogs(logs.stream().map(log -> {
            FeedbackStatusLogVO logVO = new FeedbackStatusLogVO();
            BeanUtil.copyProperties(log, logVO);
            return logVO;
        }).collect(Collectors.toList()));

        // 附件列表
        List<FeedbackAttachment> attachments = feedbackAttachmentMapper.selectList(
                new LambdaQueryWrapper<FeedbackAttachment>()
                        .eq(FeedbackAttachment::getFeedbackId, feedback.getId())
                        .eq(FeedbackAttachment::getIsDelete, 0)
                        .orderByAsc(FeedbackAttachment::getSortOrder));
        vo.setAttachments(attachments.stream().map(att -> {
            FeedbackAttachmentVO attVO = new FeedbackAttachmentVO();
            BeanUtil.copyProperties(att, attVO);
            return attVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 封装反馈回复数据
     * @param reply
     * @return
     */
    private FeedbackReplyVO convertReplyToVO(FeedbackReply reply) {
        FeedbackReplyVO vo = new FeedbackReplyVO();
        BeanUtil.copyProperties(reply, vo);
        FeedbackReplyTypeEnum replyType = FeedbackReplyTypeEnum.getByType(reply.getReplyType());
        vo.setReplyTypeDesc(replyType != null ? replyType.getDesc() : "");
        if (reply.getUserId() != null) {
            User user = userService.getById(reply.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
        }
        return vo;
    }

    /**
     *  封装反馈数据列表
     * @param page
     * @return
     */
    private Page<FeedbackListItemVO> convertToListItemPage(Page<Feedback> page) {
        Page<FeedbackListItemVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(fb -> {
            FeedbackListItemVO vo = new FeedbackListItemVO();
            BeanUtil.copyProperties(fb, vo);
            FeedbackTypeEnum typeEnum = FeedbackTypeEnum.getByType(fb.getType());
            vo.setTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
            FeedbackStatusEnum statusEnum = FeedbackStatusEnum.getByType(fb.getStatus());
            vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 封装反馈数据统计
     * @param userId
     * @return
     */
    private FeedbackStatsVO buildStatsVO(Long userId) {
        LambdaQueryWrapper<Feedback> baseWrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0);
        if (userId != null) {
            baseWrapper.eq(Feedback::getUserId, userId);
        }

        FeedbackStatsVO stats = new FeedbackStatsVO();
        stats.setTotalCount(this.count(baseWrapper));
        stats.setPendingCount(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getStatus, FeedbackStatusEnum.PENDING.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        stats.setProcessingCount(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getStatus, FeedbackStatusEnum.PROCESSING.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        stats.setResolvedCount(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getStatus, FeedbackStatusEnum.RESOLVED.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        stats.setClosedCount(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getStatus, FeedbackStatusEnum.CLOSED.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        stats.setRejectedCount(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getStatus, FeedbackStatusEnum.REJECTED.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        stats.setP0Count(this.count(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getIsDelete, 0).eq(Feedback::getPriority, FeedbackPriorityEnum.P0.getType())
                .eq(userId != null, Feedback::getUserId, userId)));
        return stats;
    }
}
