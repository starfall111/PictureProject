package org.example.pojo.dto.seckill;

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
