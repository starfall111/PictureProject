package org.example.pojo.vo.report;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 举报目标信息 VO
 *
 * @author Zou
 */
@Data
public class ReportTargetVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标 ID
     */
    private Long id;

    /**
     * 目标类型
     */
    private String type;

    /**
     * 目标标题
     */
    private String title;

    /**
     * 目标缩略图 URL
     */
    private String thumbnailUrl;

    /**
     * 目标作者 ID
     */
    private Long authorId;

    /**
     * 目标作者用户名
     */
    private String authorName;
}
