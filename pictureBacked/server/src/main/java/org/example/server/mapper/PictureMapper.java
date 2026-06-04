package org.example.server.mapper;

import org.example.pojo.entity.Picture;
import org.example.pojo.entity.PictureWithStats;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
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

    /**
     * 查询已审核通过的分类热度图片（降级查询，按 hotScore 降序）
     */
    List<PictureWithStats> selectHotPicturesByCategory(@Param("categoryId") Long categoryId,
                                                        @Param("offset") int offset,
                                                        @Param("limit") int limit);

    /**
     * 分页查询关注用户的动态图片
     *
     * @param page       分页参数
     * @param followerId 关注者ID
     * @return 动态图片分页
     */
    Page<Picture> selectFeedPictures(Page<Picture> page,
                                      @Param("followerId") Long followerId);

    /**
     * 统计关注用户的新动态数量（createTime > watermark）
     *
     * @param followerId 关注者ID
     * @param watermark  水位线（毫秒时间戳）
     * @return 未读动态数
     */
    Long countFeedUnread(@Param("followerId") Long followerId,
                         @Param("watermark") Date watermark);
}




