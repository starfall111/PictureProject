package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量任务统计 VO
 *
 * @author Zou
 */
@Data
public class BatchTaskStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总任务数
     */
    private long totalCount;

    /**
     * 待处理数
     */
    private long pendingCount;

    /**
     * 处理中数
     */
    private long processingCount;

    /**
     * 已完成数
     */
    private long completedCount;

    /**
     * 失败数
     */
    private long failedCount;

    /**
     * 今日新增数
     */
    private long todayNewCount;

    /**
     * 总体成功率(%)
     */
    private double successRate;
}
