package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈附件
 *
 * @author Zou
 * @TableName feedback_attachment
 */
@TableName(value = "feedback_attachment")
@Data
public class FeedbackAttachment implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联反馈ID */
    private Long feedbackId;

    /** OSS 文件 URL */
    private String fileUrl;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型：PNG/JPG/JPEG/GIF */
    private String fileType;

    /** 排序序号 */
    private Integer sortOrder;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
