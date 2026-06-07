package org.example.pojo.dto.seckill;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 秒杀抢购 MQ 消息体
 *
 * @author Zou
 */
@Data
public class SeckillMessage implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 订单号
     */
    private String orderNo;

    @Serial
    private static final long serialVersionUID = 1L;
}
