package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片统计
 *
 * @TableName picture_statistics
 */
@TableName(value = "picture_statistics")
@Data
public class PictureStatistics implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long pictureId;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer shareCount;

    private Integer viewCount;

    private Integer downloadCount;

    @Serial
    private static final long serialVersionUID = 1L;
}