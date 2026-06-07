package org.example.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.SeckillOrder;

import java.util.Map;

/**
 * 秒杀服务接口
 *
 * @author Zou
 */
public interface SeckillService extends IService<SeckillOrder> {

    /**
     * 获取秒杀 HMAC 令牌
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @return 令牌字符串
     */
    String getToken(Long userId, Long batchId);

    /**
     * 执行秒杀抢券（Redis 预扣库存 + MQ 异步下单）
     *
     * @param userId   用户ID
     * @param batchId  批次ID
     * @param token    HMAC 令牌
     * @param clientIP 客户端 IP
     * @return 订单号
     */
    String grab(Long userId, Long batchId, String token, String clientIP);

    /**
     * 查询秒杀结果
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 秒杀订单
     */
    SeckillOrder getResult(Long userId, String orderNo);

    /**
     * 预热批次库存到 Redis
     *
     * @param batchId 批次ID
     */
    void preheat(Long batchId);

    /**
     * 获取批次信息（含剩余库存）
     *
     * @param batchId 批次ID
     * @return 批次详情
     */
    Map<String, Object> getBatchInfo(Long batchId);
}
