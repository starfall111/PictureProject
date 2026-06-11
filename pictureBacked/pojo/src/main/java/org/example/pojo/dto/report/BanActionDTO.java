package org.example.pojo.dto.report;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 封禁操作 DTO
 *
 * @author Zou
 */
@Data
public class BanActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 被封禁用户 ID
     */
    private Long userId;

    /**
     * 封禁类型
     */
    private String banType;

    /**
     * 封禁时长（天）
     */
    private Integer banDuration;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 关联举报记录 ID
     */
    private Long reportId;
}
