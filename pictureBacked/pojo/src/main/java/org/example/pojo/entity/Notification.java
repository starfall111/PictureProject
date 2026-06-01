package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 站内通知
 *
 * @author Zou
 * @TableName notification
 */
@TableName(value = "notification")
@Data
public class Notification implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收者用户ID
     */
    private Long receiverId;

    /**
     * 触发者用户ID
     */
    private Long senderId;

    /**
     * 触发者昵称（冗余）
     */
    private String senderName;

    /**
     * 触发者头像（冗余）
     */
    private String senderAvatar;

    /**
     * 通知类型：LIKE/FAVORITE/COMMENT/FOLLOW/SYSTEM
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联资源ID
     */
    private Long resourceId;

    /**
     * 关联资源链接
     */
    private String resourceUrl;

    /**
     * 是否已读：0=未读 1=已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
