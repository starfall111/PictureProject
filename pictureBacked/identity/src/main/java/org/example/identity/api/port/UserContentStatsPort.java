package org.example.identity.api.port;

import org.example.identity.api.port.model.UserContentStats;

/**
 * 用户内容统计端口 — 由图片模块提供实现（防腐层）
 * <p>
 * 身份上下文需要展示用户画像统计（上传/获赞/收藏等），
 * 但这些数据的所有权在图片模块，通过该端口反向获取，
 * 避免身份模块直接依赖图片模块的持久层。
 *
 * @author Zou
 */
public interface UserContentStatsPort {

    /**
     * 查询用户的公共图库内容统计
     *
     * @param userId 用户 ID
     * @return 内容统计（点赞/收藏/上传/总互动量/涉及分类）
     */
    UserContentStats getUserContentStats(Long userId);
}
