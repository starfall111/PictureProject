package org.example.marketing.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.marketing.interfaces.dto.BatchQueryDTO;
import org.example.marketing.domain.model.SeckillOrder;
import org.example.marketing.interfaces.vo.PublicBatchVO;

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

    /**
     * 获取公开批次列表（分页 + 状态筛选）
     * <p>
     * 只返回对用户可见的批次（预热中、进行中、已结束），
     * 不返回草稿和已取消的批次
     * </p>
     *
     * @param dto 查询参数（status / current / pageSize）
     * @return 分页结果
     */
    Page<PublicBatchVO> listBatches(BatchQueryDTO dto);
}
