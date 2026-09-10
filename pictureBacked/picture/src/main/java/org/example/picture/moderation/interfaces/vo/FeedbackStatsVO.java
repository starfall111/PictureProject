package org.example.picture.moderation.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈统计 VO
 *
 * @author Zou
 */
@Data
public class FeedbackStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总数
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
     * 已解决数
     */
    private long resolvedCount;

    /**
     * 已关闭数
     */
    private long closedCount;

    /**
     * 已拒绝数
     */
    private long rejectedCount;

    /**
     * P0 紧急反馈数
     */
    private long p0Count;
}
