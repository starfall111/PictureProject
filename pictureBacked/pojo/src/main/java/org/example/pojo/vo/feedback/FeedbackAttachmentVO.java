package org.example.pojo.vo.feedback;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈附件 VO
 *
 * @author Zou
 */
@Data
public class FeedbackAttachmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 附件 ID
     */
    private Long id;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;
}
