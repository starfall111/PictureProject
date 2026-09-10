package org.example.picture.moderation.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 举报处理 DTO
 *
 * @author Zou
 */
@Data
public class ReportHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报记录 ID
     */
    private Long reportId;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 处理原因
     */
    private String handleReason;

    /**
     * 封禁时长（天），仅在 handleResult 为 BAN_TEMP_* 时使用
     */
    private Integer banDuration;
}
