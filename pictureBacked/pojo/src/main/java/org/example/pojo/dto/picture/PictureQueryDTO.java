package org.example.pojo.dto.picture;

import lombok.Data;
import org.example.pojo.PageRequest;

import java.util.List;

/**
 * @author Zou
 */
@Data
public class PictureQueryDTO extends PageRequest {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类 id
     */
    private Long categoryId;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 所属空间 spaceId
     */
    private Long spaceId;

    /**
     * 空间分页查询和公共图库查询
     */
    private Boolean nullSpaceId;

    /**
    * 审核状态
    */
    private Integer reviewStatus;

    /**
     * 收索关键字（图片名或图片简介）
     * */
    private String searchText;
}
