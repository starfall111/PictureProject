package org.example.picture.core.interfaces.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 批量获取图片任务 VO
 */
@Data
@Builder
public class BatchTaskVO {

    private Long taskId;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String message;

    private Date createTime;

    private Date finishTime;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 搜索来源
     */
    private String searchSource;

    /**
     * 图片分类 ID
     */
    private Long categoryId;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

    /**
     * 图片标签
     */
    private List<String> tags;

    /**
     * 所属空间 ID
     */
    private Long spaceId;
}
