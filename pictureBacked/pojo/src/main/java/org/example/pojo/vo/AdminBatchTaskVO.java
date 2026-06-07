package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 管理端批量任务详情 VO
 *
 * @author Zou
 */
@Data
public class AdminBatchTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long userId;

    private Long spaceId;

    private String searchText;

    private String searchSource;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String status;

    private Long categoryId;

    private String namePrefix;

    private String tags;

    private String errorMessage;

    private Date createTime;

    private Date updateTime;

    private Date finishTime;
}
