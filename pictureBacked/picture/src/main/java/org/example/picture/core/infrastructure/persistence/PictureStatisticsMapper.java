package org.example.picture.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.picture.core.domain.model.PictureStatistics;

/**
 * @author Zou
 * @description 针对表【picture_statistics(图片统计)】的数据库操作Mapper
 * @createDate 2026-05-26 15:39:32
 * @Entity generator.domain.PictureStatistics
 */
public interface PictureStatisticsMapper extends BaseMapper<PictureStatistics> {

    /**
     * 原子 UPSERT：存在则更新，不存在则插入
     */
    @Override
    boolean insertOrUpdate(PictureStatistics stat);
}




