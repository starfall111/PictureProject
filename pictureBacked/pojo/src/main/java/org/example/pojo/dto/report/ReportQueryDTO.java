package org.example.pojo.dto.report;

import lombok.Data;
import org.example.pojo.PageRequest;

/**
 * 举报查询 DTO
 *
 * @author Zou
 */
@Data
public class ReportQueryDTO extends PageRequest {

    /**
     * 状态筛选
     */
    private String status;

    /**
     * 目标类型筛选
     */
    private String targetType;

    /**
     * 举报原因类型筛选
     */
    private String reasonType;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 最小举报次数阈值
     */
    private Integer minReportCount;
}
