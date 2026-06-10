package org.example.server.service.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.common.enums.NotificationTypeEnum;
import org.example.pojo.dto.social.SocialActionMessage;
import org.example.pojo.entity.PictureFavorite;
import org.example.pojo.entity.PictureLike;
import org.example.pojo.entity.User;
import org.example.server.mapper.PictureFavoriteMapper;
import org.example.server.mapper.PictureLikeMapper;
import org.example.server.mapper.UserMapper;
import org.example.server.service.event.NotificationEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 社交操作（点赞/收藏）MQ 消费者
 * <p>
 * 核心流程:
 * 1. 反序列化消息，校验必要字段
 * 2. 根据 actionType + operation 执行对应的 DB 写操作
 * 3. 新增操作（insert）时发布通知事件
 * 4. 捕获 DuplicateKeyException 静默忽略（快速重复点击场景）
 * 5. 其他异常 NACK 进入死信队列
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class SocialActionConsumer {

    @Resource
    private PictureLikeMapper pictureLikeMapper;

    @Resource
    private PictureFavoriteMapper pictureFavoriteMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 消息中 actionType 的值常量
     */
    private static final String ACTION_TYPE_LIKE = "like";
    private static final String ACTION_TYPE_FAVORITE = "favorite";

    /**
     * 消息中 operation 的值常量
     */
    private static final String OPERATION_INSERT = "insert";
    private static final String OPERATION_DELETE = "delete";

    @RabbitListener(queues = "social.action.queue")
    public void handleSocialAction(String message, Channel channel,
                                   org.springframework.messaging.Message<String> msg) {
        long deliveryTag = 0;
        SocialActionMessage socialMsg = null;
        try {
            deliveryTag = (long) msg.getHeaders().get(AmqpHeaders.DELIVERY_TAG);

            socialMsg = JSONUtil.toBean(message, SocialActionMessage.class);
            log.info("社交操作消费开始 | actionType={}, operation={}, userId={}, pictureId={}",
                    socialMsg.getActionType(), socialMsg.getOperation(),
                    socialMsg.getUserId(), socialMsg.getPictureId());

            processAction(socialMsg);

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("社交操作消费成功 | actionType={}, operation={}, userId={}, pictureId={}",
                    socialMsg.getActionType(), socialMsg.getOperation(),
                    socialMsg.getUserId(), socialMsg.getPictureId());
        } catch (DuplicateKeyException e) {
            // 快速重复点击导致唯一索引冲突，Redis 已正确反映最新状态，DB 侧静默忽略
            log.warn("社交操作唯一索引冲突（忽略） | actionType={}, userId={}, pictureId={}",
                    socialMsg != null ? socialMsg.getActionType() : "unknown",
                    socialMsg != null ? socialMsg.getUserId() : "unknown",
                    socialMsg != null ? socialMsg.getPictureId() : "unknown");
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("重复点击场景 ACK 失败", ex);
            }
        } catch (Exception e) {
            log.error("社交操作消费失败 | payload={}", message, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("NACK 失败", ex);
            }
        }
    }

    /**
     * 处理社交操作消息
     */
    private void processAction(SocialActionMessage msg) {
        String actionType = msg.getActionType();
        String operation = msg.getOperation();

        if (ACTION_TYPE_LIKE.equals(actionType)) {
            processLikeAction(msg, operation);
        } else if (ACTION_TYPE_FAVORITE.equals(actionType)) {
            processFavoriteAction(msg, operation);
        } else {
            log.warn("未知的社交操作类型 | actionType={}", actionType);
        }
    }

    /**
     * 处理点赞操作
     */
    private void processLikeAction(SocialActionMessage msg, String operation) {
        Long pictureId = msg.getPictureId();
        Long userId = msg.getUserId();

        if (OPERATION_INSERT.equals(operation)) {
            PictureLike pictureLike = new PictureLike();
            pictureLike.setPictureId(pictureId);
            pictureLike.setUserId(userId);
            pictureLikeMapper.insert(pictureLike);

            // 发布点赞通知
            publishNotification(msg, NotificationTypeEnum.LIKE, "赞了你的图片");
        } else if (OPERATION_DELETE.equals(operation)) {
            QueryWrapper<PictureLike> qw = new QueryWrapper<>();
            qw.eq("pictureId", pictureId).eq("userId", userId);
            pictureLikeMapper.delete(qw);
        }
    }

    /**
     * 处理收藏操作
     */
    private void processFavoriteAction(SocialActionMessage msg, String operation) {
        Long pictureId = msg.getPictureId();
        Long userId = msg.getUserId();

        if (OPERATION_INSERT.equals(operation)) {
            PictureFavorite pictureFavorite = new PictureFavorite();
            pictureFavorite.setPictureId(pictureId);
            pictureFavorite.setUserId(userId);
            pictureFavoriteMapper.insert(pictureFavorite);

            // 发布收藏通知
            publishNotification(msg, NotificationTypeEnum.FAVORITE, "收藏了你的图片");
        } else if (OPERATION_DELETE.equals(operation)) {
            QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
            qw.eq("pictureId", pictureId).eq("userId", userId);
            pictureFavoriteMapper.delete(qw);
        }
    }

    /**
     * 发布通知事件
     */
    private void publishNotification(SocialActionMessage msg, NotificationTypeEnum type, String action) {
        try {
            User sender = userMapper.selectById(msg.getUserId());
            String senderName = sender != null ? sender.getUserName() : "匿名用户";
            String senderAvatar = sender != null ? sender.getUserAvatar() : null;

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    msg.getPictureOwnerId(),
                    msg.getUserId(),
                    senderName,
                    senderAvatar,
                    type,
                    action,
                    null,
                    msg.getPictureId(),
                    "/picture/" + msg.getPictureId()
            ));
        } catch (Exception e) {
            log.warn("发布通知事件失败：pictureId={}, senderId={}, type={}",
                    msg.getPictureId(), msg.getUserId(), type, e);
        }
    }
}
