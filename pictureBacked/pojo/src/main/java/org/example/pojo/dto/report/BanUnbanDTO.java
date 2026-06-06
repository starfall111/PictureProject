package org.example.pojo.dto.report;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 解封操作 DTO
 *
 * @author Zou
 */
@Data
public class BanUnbanDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 被解封用户 ID
     */
    private Long userId;

    /**
     * 解封原因
     */
    private String unbanReason;
}
