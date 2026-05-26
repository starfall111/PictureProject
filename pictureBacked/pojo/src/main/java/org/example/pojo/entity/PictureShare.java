package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图片分享记录
 *
 * @TableName picture_share
 */
@TableName(value = "picture_share")
@Data
public class PictureShare implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long pictureId;

    private Long userId;

    /**
     * 分享平台：wechat/weibo/qq
     */
    private String platform;

    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}