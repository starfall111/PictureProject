package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图片点赞
 *
 * @TableName picture_like
 */
@TableName(value = "picture_like")
@Data
public class PictureLike implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long pictureId;

    private Long userId;

    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}