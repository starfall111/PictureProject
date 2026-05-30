package org.example.server.mapper;

import org.example.pojo.entity.Picture;
import org.example.pojo.entity.PictureWithStats;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author Zou
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2026-05-14 21:47:55
* @Entity org.example.pojo.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 批量查询图片及其统计数据（LEFT JOIN picture_statistics）
     */
    List<PictureWithStats> selectWithStats(@Param("ids") List<Long> ids);

    /**
     * 查询所有已审核通过的公开图片 ID（用于全量热度重算）
     */
    List<Long> selectAllPublicPictureIds();

    /**
     * 批量更新图片热度分数
     */
    int batchUpdateHotScore(@Param("list") List<Map<String, Object>> list);

    /**
     * 查询已审核通过的热度图片（降级查询，按 hotScore 降序）
     */
    List<PictureWithStats> selectHotPictures(@Param("offset") int offset, @Param("limit") int limit);
}




