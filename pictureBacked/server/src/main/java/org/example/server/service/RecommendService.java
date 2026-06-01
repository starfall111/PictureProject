package org.example.server.service;

import org.example.pojo.dto.recommend.RecommendQueryDTO;
import org.example.pojo.entity.PictureWithStats;
import org.example.pojo.vo.RecommendVO;

/**
 * 推荐服务接口
 *
 * @author Zou
 */
public interface RecommendService {

    /**
     * 推荐查询
     *
     * @param queryDTO 查询条件
     * @param userId   用户 ID（guess 场景需要，可为 null）
     * @return 推荐结果
     */
    RecommendVO recommend(RecommendQueryDTO queryDTO, Long userId);

    /**
     * 全量重算热度分数并写入 Redis ZSET
     *
     * @return 已重算的图片数量
     */
    int rebuildHotScores();

    /**
     * 计算单张图片的热度分数
     *
     * @param stats 图片统计数据
     * @return 热度分数
     */
    double calculateHotScore(PictureWithStats stats);

    /**
     * 从 DB 加载 hotScore 预热到 Redis ZSET（启动时 / 缓存恢复）
     *
     * @return 加载的图片数量
     */
    int preheatFromDb();
}
