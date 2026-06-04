package org.example.server.config;

import lombok.extern.slf4j.Slf4j;
import org.example.server.service.RecommendService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 推荐缓存预热 Runner
 * 应用启动后检查 Redis ZSET 是否为空，若空则从 DB hotScore 恢复
 *
 * @author Zou
 */
@Slf4j
@Component
public class RecommendPreheatRunner implements CommandLineRunner {

    @Resource
    private RecommendService recommendService;

    @Override
    public void run(String... args) {
        try {
            log.info("开始推荐缓存预热...");
            int count = recommendService.preheatFromDb();
            log.info("推荐缓存预热完成，加载 {} 张图片", count);
        } catch (Exception e) {
            log.error("推荐缓存预热失败，将由推荐接口降级 DB 查询兜底", e);
        }
    }
}
