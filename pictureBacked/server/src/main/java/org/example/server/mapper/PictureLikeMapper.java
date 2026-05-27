package org.example.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.entity.PictureLike;
import org.example.pojo.vo.PictureBriefVO;

import java.util.List;

/**
 * @author Zou
 * @description 针对表【picture_like(图片点赞)】的数据库操作Mapper
 * @createDate 2026-05-26 15:39:14
 * @Entity generator.domain.PictureLike
 */
public interface PictureLikeMapper extends BaseMapper<PictureLike> {

    /**
     * 查询用户点赞的图片列表（分页 + 筛选）
     *
     * @param userId    用户 id
     * @param queryDTO  查询条件
     * @param offset    偏移量
     * @param pageSize  每页数量
     * @return 图片列表
     */
    List<PictureBriefVO> selectUserLikedPictures(@Param("userId") Long userId,
                                                 @Param("query") UserPictureQueryDTO queryDTO,
                                                 @Param("offset") Integer offset,
                                                 @Param("pageSize") Integer pageSize);

    /**
     * 统计用户点赞的图片总数
     *
     * @param userId   用户 id
     * @param queryDTO 查询条件
     * @return 总数
     */
    Long countUserLikedPictures(@Param("userId") Long userId, @Param("query") UserPictureQueryDTO queryDTO);
}




