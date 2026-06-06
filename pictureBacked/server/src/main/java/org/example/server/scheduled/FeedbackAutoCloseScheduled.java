package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.FeedbackCloseReasonEnum;
import org.example.common.enums.FeedbackStatusEnum;
import org.example.pojo.entity.Feedback;
import org.example.pojo.entity.FeedbackStatusLog;
import org.example.server.mapper.FeedbackMapper;
import org.example.server.mapper.FeedbackStatusLogMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 反馈自动关闭定时任务
 *
 * @author Zou
 */
@Slf4j
@Component
public class FeedbackAutoCloseScheduled {

    @Resource
    private FeedbackMapper feedbackMapper;

    @Resource
    private FeedbackStatusLogMapper feedbackStatusLogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 自动关闭已解决超过7天的反馈 - 每1小时
     */
    @Scheduled(fixedRate = 3600000)
    public void autoCloseResolvedFeedback() {
        String lockKey = "lock:feedback:auto-close";
        if (!tryLock(lockKey, 3000)) {
            log.info("反馈自动关闭任务：另一个实例正在执行");
            return;
        }
        try {
            Date threshold = new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);

            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Feedback::getStatus, FeedbackStatusEnum.RESOLVED.getType())
                    .lt(Feedback::getUpdateTime, threshold)
                    .eq(Feedback::getIsDelete, 0);

            List<Feedback> feedbacks = feedbackMapper.selectList(wrapper);
            for (Feedback feedback : feedbacks) {
                feedback.setStatus(FeedbackStatusEnum.CLOSED.getType());
                feedback.setCloseReason(FeedbackCloseReasonEnum.AUTO_CLOSE.getType());
                feedbackMapper.updateById(feedback);

                FeedbackStatusLog statusLog = new FeedbackStatusLog();
                statusLog.setFeedbackId(feedback.getId());
                statusLog.setFromStatus(FeedbackStatusEnum.RESOLVED.getType());
                statusLog.setToStatus(FeedbackStatusEnum.CLOSED.getType());
                statusLog.setOperatorId(0L);
                statusLog.setOperatorType("SYSTEM");
                statusLog.setRemark("已解决超过7天，自动关闭");
                feedbackStatusLogMapper.insert(statusLog);
            }
            log.info("反馈自动关闭完成，关闭 {} 条", feedbacks.size());
        } catch (Exception e) {
            log.error("反馈自动关闭任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 刷新管理端待处理反馈计数 - 每5分钟
     */
    @Scheduled(fixedRate = 300000)
    public void refreshPendingCount() {
        try {
            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Feedback::getStatus, FeedbackStatusEnum.PENDING.getType())
                    .eq(Feedback::getIsDelete, 0);
            long count = feedbackMapper.selectCount(wrapper);

            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.FEEDBACK_ADMIN_PENDING_COUNT,
                    String.valueOf(count),
                    RedisKeyConstants.FEEDBACK_ADMIN_PENDING_TTL,
                    TimeUnit.SECONDS
            );
            log.debug("管理端待处理反馈计数刷新：{}", count);
        } catch (Exception e) {
            log.error("刷新待处理反馈计数失败", e);
        }
    }

    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
