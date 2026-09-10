package org.example.marketing.application;

import org.example.marketing.interfaces.dto.BatchCreateDTO;
import org.example.marketing.interfaces.dto.BatchUpdateDTO;

/**
 * 批次管理服务接口（管理端）
 * <p>
 * 负责批次的创建、修改、兑换码生成、状态流转、取消/恢复等生命周期管理
 * </p>
 *
 * @author Zou
 */
public interface BatchManageService {

    /**
     * 创建发放批次（草稿状态）
     *
     * @param dto 创建参数
     * @return 批次ID
     */
    Long createBatch(BatchCreateDTO dto);

    /**
     * 生成兑换码（草稿阶段调用，幂等）
     *
     * @param batchId 批次ID
     */
    void generateCodes(Long batchId);

    /**
     * 修改批次数据（仅草稿阶段）
     *
     * @param dto 修改参数（只更新非空字段）
     */
    void updateBatch(BatchUpdateDTO dto);

    /**
     * 统一状态流转
     * <p>
     * 合法转换：
     * 草稿(0) -> 预热中(1)：前置条件 — 兑换码已生成
     * 预热中(1) -> 进行中(2)：前置条件 — startTime <= now
     * 进行中(2) -> 已结束(3)：手动或定时任务触发
     * </p>
     *
     * @param batchId      批次ID
     * @param targetStatus 目标状态
     */
    void transitionBatch(Long batchId, int targetStatus);

    /**
     * 取消批次
     * <p>只有草稿(0)和预热中(1)可以取消</p>
     *
     * @param batchId 批次ID
     */
    void cancelBatch(Long batchId);

    /**
     * 恢复已取消批次为草稿
     *
     * @param batchId 批次ID
     */
    void restoreBatch(Long batchId);

    /**
     * 结束批次（进行中 -> 已结束）
     * <p>先更新状态阻止新请求，再异步将未发放券标记为过期</p>
     *
     * @param batchId 批次ID
     */
    void endBatch(Long batchId);
}
