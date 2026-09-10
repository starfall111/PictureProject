package org.example.picture.social.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.picture.social.interfaces.dto.UserPictureQueryDTO;
import org.example.picture.core.domain.model.PictureBrief;
import org.example.picture.social.domain.model.PictureFavorite;

import java.util.List;

/**
 * @author Zou
 * @description 针对表【picture_favorite(图片收藏)】的数据库操作Mapper
 * @createDate 2026-05-26 15:39:27
 * @Entity generator.domain.PictureFavorite
 */
public interface PictureFavoriteMapper extends BaseMapper<PictureFavorite> {

    /**
     * 查询用户收藏的图片列表（分页 + 筛选）
     *
     * @param userId   用户 id
     * @param queryDTO 查询条件
     * @param offset   偏移量
     * @param pageSize 每页数量
     * @return 图片列表
     */
    List<PictureBrief> selectUserFavoritedPictures(@Param("userId") Long userId,
                                                   @Param("query") UserPictureQueryDTO queryDTO,
                                                   @Param("offset") Integer offset,
                                                   @Param("pageSize") Integer pageSize);

    /**
     * 统计用户收藏的图片总数
     *
     * @param userId   用户 id
     * @param queryDTO 查询条件
     * @return 总数
     */
    Long countUserFavoritedPictures(@Param("userId") Long userId, @Param("query") UserPictureQueryDTO queryDTO);
}




