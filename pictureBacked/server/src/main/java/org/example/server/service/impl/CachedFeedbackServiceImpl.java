package org.example.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.feedback.*;
import org.example.pojo.entity.Feedback;
import org.example.pojo.vo.feedback.*;
import org.example.server.mapper.FeedbackMapper;
import org.example.server.service.FeedbackService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存版反馈服务实现
 * - 反馈详情 → Redis 缓存，TTL 15-30min
 * - 反馈统计 → Redis 缓存，TTL 5min
 * - 写操作 → 委托 dbFeedbackService + 主动失效缓存
 *
 * @author Zou
 */
@Slf4j
@Service("cachedFeedbackService")
public class CachedFeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback>
        implements FeedbackService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    @Qualifier("dbFeedbackService")
    private FeedbackService dbFeedbackService;

    // ==================== 缓存读方法 ====================

    @Override
    public FeedbackVO getFeedbackDetail(Long feedbackId) {
        String cacheKey = "feedback:detail:" + feedbackId;
        int ttlSeconds = 900 + RandomUtil.randomInt(0, 900);

        String json = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            FeedbackVO vo = dbFeedbackService.getFeedbackDetail(feedbackId);
            return vo != null ? JSONUtil.toJsonStr(vo) : null;
        });

        return json != null ? JSONUtil.toBean(json, FeedbackVO.class) : null;
    }

    @Override
    public FeedbackVO getAdminFeedbackDetail(Long feedbackId) {
        // 管理端详情不走缓存
        return dbFeedbackService.getAdminFeedbackDetail(feedbackId);
    }

    @Override
    public FeedbackStatsVO getMyFeedbackStats() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return dbFeedbackService.getMyFeedbackStats();
        }

        String cacheKey = String.format(RedisKeyConstants.FEEDBACK_USER_STATS_KEY, userId);
        return getStatsWithCache(cacheKey, () -> dbFeedbackService.getMyFeedbackStats());
    }

    @Override
    public FeedbackStatsVO getAdminFeedbackStats() {
        String cacheKey = RedisKeyConstants.FEEDBACK_ADMIN_STATS_KEY;
        return getStatsWithCache(cacheKey, () -> dbFeedbackService.getAdminFeedbackStats());
    }

    // ==================== 写方法（委托 + 缓存失效）====================

    @Override
    public FeedbackAttachmentVO uploadAttachment(MultipartFile file) {
        return dbFeedbackService.uploadAttachment(file);
    }

    @Override
    public long submitFeedback(FeedbackSubmitDTO dto) {
        long feedbackId = dbFeedbackService.submitFeedback(dto);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
        return feedbackId;
    }

    @Override
    public void withdrawFeedback(Long feedbackId) {
        dbFeedbackService.withdrawFeedback(feedbackId);
        invalidateFeedbackCache(feedbackId);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
    }

    @Override
    public Page<FeedbackListItemVO> getMyFeedbackList(FeedbackQueryDTO dto) {
        return dbFeedbackService.getMyFeedbackList(dto);
    }

    @Override
    public void replyFeedback(Long feedbackId, FeedbackReplyDTO dto) {
        dbFeedbackService.replyFeedback(feedbackId, dto);
        invalidateFeedbackCache(feedbackId);
        invalidateUserStatsCache();
    }

    @Override
    public void reopenFeedback(Long feedbackId) {
        dbFeedbackService.reopenFeedback(feedbackId);
        invalidateFeedbackCache(feedbackId);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
    }

    @Override
    public void confirmFeedback(Long feedbackId) {
        dbFeedbackService.confirmFeedback(feedbackId);
        invalidateFeedbackCache(feedbackId);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
    }

    @Override
    public Page<FeedbackListItemVO> getAdminFeedbackList(FeedbackQueryDTO dto) {
        return dbFeedbackService.getAdminFeedbackList(dto);
    }

    @Override
    public void claimFeedback(Long feedbackId) {
        dbFeedbackService.claimFeedback(feedbackId);
        invalidateFeedbackCache(feedbackId);
        invalidateAdminStatsCache();
    }

    @Override
    public void transferFeedback(Long feedbackId, Long targetHandlerId) {
        dbFeedbackService.transferFeedback(feedbackId, targetHandlerId);
        invalidateFeedbackCache(feedbackId);
    }

    @Override
    public void adminReplyFeedback(Long feedbackId, FeedbackReplyDTO dto) {
        dbFeedbackService.adminReplyFeedback(feedbackId, dto);
        invalidateFeedbackCache(feedbackId);
        invalidateAdminStatsCache();
    }

    @Override
    public void addInternalNote(Long feedbackId, FeedbackReplyDTO dto) {
        dbFeedbackService.addInternalNote(feedbackId, dto);
        invalidateFeedbackCache(feedbackId);
    }

    @Override
    public void rejectFeedback(Long feedbackId, String reason) {
        dbFeedbackService.rejectFeedback(feedbackId, reason);
        invalidateFeedbackCache(feedbackId);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
    }

    @Override
    public void updatePriority(Long feedbackId, String priority) {
        dbFeedbackService.updatePriority(feedbackId, priority);
        invalidateFeedbackCache(feedbackId);
        invalidateAdminStatsCache();
    }

    @Override
    public long convertToReport(Long feedbackId, FeedbackConvertDTO dto) {
        long reportId = dbFeedbackService.convertToReport(feedbackId, dto);
        invalidateFeedbackCache(feedbackId);
        invalidateAdminStatsCache();
        return reportId;
    }

    @Override
    public void batchCloseFeedback(List<Long> feedbackIds, String reason) {
        dbFeedbackService.batchCloseFeedback(feedbackIds, reason);
        feedbackIds.forEach(this::invalidateFeedbackCache);
        invalidateUserStatsCache();
        invalidateAdminStatsCache();
    }

    // ==================== 缓存失效方法 ====================

    private void invalidateFeedbackCache(Long feedbackId) {
        if (feedbackId == null) {
            return;
        }
        String cacheKey = "feedback:detail:" + feedbackId;
        stringRedisTemplate.delete(cacheKey);
        log.debug("Invalidated feedback cache for feedbackId: {}", feedbackId);
    }

    private void invalidateUserStatsCache() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return;
        }
        String cacheKey = String.format(RedisKeyConstants.FEEDBACK_USER_STATS_KEY, userId);
        stringRedisTemplate.delete(cacheKey);
        log.debug("Invalidated user feedback stats cache for userId: {}", userId);
    }

    private void invalidateAdminStatsCache() {
        stringRedisTemplate.delete(RedisKeyConstants.FEEDBACK_ADMIN_STATS_KEY);
        log.debug("Invalidated admin feedback stats cache");
    }

    // ==================== 私有辅助方法 ====================

    @FunctionalInterface
    private interface StatsSupplier {
        FeedbackStatsVO get();
    }

    private FeedbackStatsVO getStatsWithCache(String cacheKey, StatsSupplier supplier) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSONUtil.toBean(cached, FeedbackStatsVO.class);
            }
        } catch (Exception e) {
            log.warn("读取反馈统计缓存失败, key={}", cacheKey, e);
        }

        FeedbackStatsVO stats = supplier.get();

        try {
            int ttlSeconds = RedisKeyConstants.FEEDBACK_STATS_TTL + RandomUtil.randomInt(0, 60);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(stats), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入反馈统计缓存失败, key={}", cacheKey, e);
        }

        return stats;
    }

    private Long getCurrentUserId() {
        try {
            org.example.pojo.entity.User currentUser = org.example.common.context.UserContext.get();
            return currentUser != null ? currentUser.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
