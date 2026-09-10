package org.example.marketing.interfaces.dto;

import lombok.Data;

/**
 * 管理员赠送 VIP DTO
 */
@Data
public class VipGrantDTO {

    private Long userId;

    /**
     * 赠送天数
     */
    private Integer days;

    private String reason;
}
