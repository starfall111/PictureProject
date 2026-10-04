package org.example.picture.moderation.interfaces.dto;

import lombok.Data;
import org.example.shared.contract.PageRequest;

/**
 * 反馈查询 DTO
 *
 * @author Zou
 */
@Data
public class FeedbackQueryDTO extends PageRequest {

    /**
     * 状态筛选
     */
    private String status;

    /**
     * 类型筛选
     */
    private String type;

    /**
     * 优先级筛选
     */
    private String priority;

    /**
     * 关键词搜索（标题/内容模糊匹配）
     */
    private String keyword;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
