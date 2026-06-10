package org.example.pojo.dto.social;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 社交操作（点赞/收藏）MQ 消息体
 * <p>
 * 用于将 DB 写入操作从同步改为异步：
 * Service 层完成 Redis 操作后，构造此消息发送到 RabbitMQ，
 * 由 Consumer 异步完成 DB 写入和通知发布。
 * </p>
 *
 * @author Zou
 */
@Data
public class SocialActionMessage implements Serializable {

    /**
     * 操作类型：like（点赞）、favorite（收藏）
     */
    private String actionType;

    /**
     * 操作行为：insert（新增）、delete（取消）
     */
    private String operation;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图片作者ID（用于通知接收者）
     */
    private Long pictureOwnerId;

    @Serial
    private static final long serialVersionUID = 1L;
}
