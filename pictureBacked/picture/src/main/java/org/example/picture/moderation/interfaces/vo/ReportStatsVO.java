package org.example.picture.moderation.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 举报统计 VO
 *
 * @author Zou
 */
@Data
public class ReportStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 待审核数
     */
    private long pendingCount;

    /**
     * 今日新增数
     */
    private long todayNewCount;

    /**
     * 累计处理数
     */
    private long totalHandledCount;
}
