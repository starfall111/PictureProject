package org.example.pojo.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.example.pojo.entity.Picture;

import java.util.List;

@Data
public class PictureVO {

    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

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
     * 分类名称
     */
    private String categoryName;

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
     * 创建用户信息
     * */
    private UserVO userVO;

    public static Picture voToObj(PictureVO pictureVO){
        if (pictureVO == null){
            return null;
        }

        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVO,picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    public static PictureVO objToVO(Picture picture){
        if (picture == null){
            return null;
        }
        PictureVO pictureVO = new PictureVO();

        BeanUtil.copyProperties(picture,pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }
}
