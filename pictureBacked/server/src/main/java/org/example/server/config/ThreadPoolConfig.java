package org.example.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * <p>
 * 替代 Spring 默认的 SimpleAsyncTaskExecutor（无界创建线程）和单线程调度器，
 * 为 @Async 异步任务和 @Scheduled 定时任务分别配置独立线程池。
 *
 * @author Zou
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 异步任务线程池 — 供 @Async 使用
     * <p>
     * 当前场景：通知事件处理、绑定手机号自动过审等异步任务
     */
    @Bean("asyncTaskExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        // 队列满时由调用线程执行，不丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步任务线程池已初始化: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 100);
        return executor;
    }

    /**
     * 定时任务线程池 — 供 @Scheduled 使用
     * <p>
     * 当前有 13 个定时任务（缓存同步、统计数据、自动解封等），
     * 大部分使用分布式锁互斥，实际并发不高
     */
    @Bean("schedulingTaskScheduler")
    public ThreadPoolTaskScheduler schedulingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduled-");
        // 定时任务异常不影响其他任务
        scheduler.setErrorHandler(t ->
                log.error("定时任务执行异常", t)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        log.info("定时任务线程池已初始化: poolSize={}", scheduler.getPoolSize());
        return scheduler;
    }
}
