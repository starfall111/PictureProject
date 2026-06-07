package org.example.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.SeckillOrder;

/**
 * 秒杀服务接口
 *
 * @author Zou
 */
public interface SeckillService extends IService<SeckillOrder> {

    /**
     * 执行秒杀抢券（Redis 预扣库存 + MQ 异步下单）
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @return 订单号
     */
    String doSeckill(Long userId, Long batchId);
}
