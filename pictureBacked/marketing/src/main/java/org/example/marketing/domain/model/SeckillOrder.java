package org.example.marketing.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 秒杀订单（防重）
 *
 * @author Zou
 * @TableName seckill_order
 */
@TableName(value = "seckill_order")
@Data
public class SeckillOrder implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 编码券ID（异步写入后回填）
     */
    private Long couponId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 0-处理中, 1-成功, 2-失败
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
