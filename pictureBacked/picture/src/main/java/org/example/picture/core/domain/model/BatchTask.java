package org.example.picture.core.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 批量获取图片任务
 *
 * @TableName batch_task
 */
@TableName(value = "batch_task")
@Data
public class BatchTask implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long spaceId;

    private String searchText;

    private String searchSource;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    /**
     * PENDING/PROCESSING/COMPLETED/FAILED
     */
    private String status;

    private Long categoryId;

    private String namePrefix;

    private String tags;

    private String errorMessage;

    private Date createTime;

    private Date updateTime;

    private Date finishTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
