package org.example.marketing.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编码券激活请求 DTO
 *
 * @author Zou
 */
@Data
public class CouponActivateDTO implements Serializable {

    /**
     * 编码券 ID
     */
    private Long couponId;

    @Serial
    private static final long serialVersionUID = 1L;
}
