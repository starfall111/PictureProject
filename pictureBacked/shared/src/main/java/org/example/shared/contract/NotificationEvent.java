package org.example.shared.contract;

import lombok.Getter;
import org.example.shared.contract.NotificationTypeEnum;
import org.springframework.context.ApplicationEvent;

/**
 * 通知事件
 *
 * @author Zou
 */
@Getter
public class NotificationEvent extends ApplicationEvent {

    /**
     * 接收者用户ID
     */
    private final Long receiverId;

    /**
     * 触发者用户ID
     */
    private final Long senderId;

    /**
     * 触发者昵称
     */
    private final String senderName;

    /**
     * 触发者头像
     */
    private final String senderAvatar;

    /**
     * 通知类型
     */
    private final NotificationTypeEnum type;

    /**
     * 通知标题
     */
    private final String title;

    /**
     * 通知内容
     */
    private final String content;

    /**
     * 关联资源ID
     */
    private final Long resourceId;

    /**
     * 关联资源链接
     */
    private final String resourceUrl;

    public NotificationEvent(Object source, Long receiverId, Long senderId,
                             String senderName, String senderAvatar,
                             NotificationTypeEnum type, String title, String content,
                             Long resourceId, String resourceUrl) {
        super(source);
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.type = type;
        this.title = title;
        this.content = content;
        this.resourceId = resourceId;
        this.resourceUrl = resourceUrl;
    }
}
