package org.example.pojo.dto.seckill;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 秒杀抢券请求 DTO
 *
 * @author Zou
 */
@Data
public class SeckillGrabDTO implements Serializable {

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * HMAC 令牌
     */
    private String token;

    @Serial
    private static final long serialVersionUID = 1L;
}
