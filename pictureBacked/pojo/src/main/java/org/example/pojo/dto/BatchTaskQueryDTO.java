package org.example.pojo.dto;

import lombok.Data;
import org.example.pojo.PageRequest;

/**
 * 批量任务管理端分页查询 DTO
 *
 * @author Zou
 */
@Data
public class BatchTaskQueryDTO extends PageRequest {

    /**
     * 状态筛选
     */
    private String status;

    /**
     * 搜索来源筛选 (pexels/bing)
     */
    private String searchSource;

    /**
     * 关键词模糊搜索 (searchText)
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

    /**
     * 用户ID筛选
     */
    private Long userId;
}
