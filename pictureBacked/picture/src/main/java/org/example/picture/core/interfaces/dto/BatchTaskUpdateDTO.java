package org.example.picture.core.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端修改批量任务 DTO
 *
 * @author Zou
 */
@Data
public class BatchTaskUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID（必填）
     */
    private Long id;

    /**
     * 状态（可选）
     */
    private String status;

    /**
     * 错误信息（可选）
     */
    private String errorMessage;

    /**
     * 标签（可选）
     */
    private String tags;
}
