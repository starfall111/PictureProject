package org.example.picture.moderation.application.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 举报统计同步定时任务
 * - 每 5 分钟将今日新增脏集合大小同步至计数器
 * - 每天凌晨清空昨日脏集合和计数器
 *
 * @author Zou
 */
@Slf4j
@Component
public class ReportStatsSyncScheduled {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 同步今日新增举报计数 - 每5分钟
     */
    @Scheduled(fixedRate = 300000)
    public void syncTodayNewCount() {
        String lockKey = "lock:report:stats:sync";
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 240, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        try {
            Long dirtySize = stringRedisTemplate.opsForSet().size(RedisKeyConstants.REPORT_TODAY_DIRTY_KEY);
            long count = dirtySize != null ? dirtySize : 0;
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.REPORT_TODAY_COUNT_KEY, String.valueOf(count),
                    25 * 3600, TimeUnit.SECONDS); // 保留到次日
            log.debug("同步今日新增举报计数: {}", count);
        } catch (Exception e) {
            log.error("同步今日新增举报计数失败", e);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 清空昨日脏集合 - 每天凌晨3点
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearYesterdayDirtySet() {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.REPORT_TODAY_DIRTY_KEY);
            stringRedisTemplate.delete(RedisKeyConstants.REPORT_TODAY_COUNT_KEY);
            log.info("清空举报昨日脏集合完成");
        } catch (Exception e) {
            log.error("清空举报昨日脏集合失败", e);
        }
    }
}
