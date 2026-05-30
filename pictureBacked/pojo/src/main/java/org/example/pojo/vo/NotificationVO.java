package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知 VO
 *
 * @author Zou
 */
@Data
public class NotificationVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 触发者用户ID
     */
    private Long senderId;

    /**
     * 触发者昵称
     */
    private String senderName;

    /**
     * 触发者头像
     */
    private String senderAvatar;

    /**
     * 通知类型
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

    @Serial
    private static final long serialVersionUID = 1L;
}
