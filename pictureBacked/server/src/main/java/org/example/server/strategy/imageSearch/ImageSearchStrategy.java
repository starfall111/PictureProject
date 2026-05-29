package org.example.server.strategy.imageSearch;

import org.example.server.strategy.imageSearch.model.ImageSourceResult;

import java.util.List;

/**
 * 图片搜索策略接口
 */
public interface ImageSearchStrategy {

    /**
     * 获取策略标识
     */
    String getSourceType();

    /**
     * 搜索图片
     *
     * @param searchText 搜索关键词
     * @param count      最大数量
     * @return 图片搜索结果列表
     */
    List<ImageSourceResult> searchImages(String searchText, int count);
}
