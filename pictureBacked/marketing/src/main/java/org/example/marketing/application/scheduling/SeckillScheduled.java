package org.example.marketing.application.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.example.marketing.application.SeckillService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 秒杀定时任务
 * <p>
 * 每分钟检查即将开始的批次，触发预热。
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class SeckillScheduled {

    @Resource
    private SeckillService seckillService;

    /**
     * 自动预热检查 — 每分钟执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoPreheat() {
        log.debug("自动预热检查...");
        // TODO: 查询 DB 中 status=0 且 startTime 在未来 5 分钟内的批次，调用 seckillService.preheat(batchId)
    }
}
