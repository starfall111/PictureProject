package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.pojo.dto.notification.NotificationQueryDTO;
import org.example.pojo.entity.Notification;
import org.example.pojo.vo.NotificationVO;
import org.example.server.service.NotificationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存版通知服务实现
 * - 未读数：Redis 缓存（30min TTL）
 * - 列表查询：直委托 DB（不做列表缓存）
 *
 * @author Zou
 */
@Slf4j
@Service("cachedNotificationService")
public class CachedNotificationServiceImpl implements NotificationService {

    @Resource(name = "dbNotificationService")
    private NotificationService dbNotificationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean save(Notification entity) {
        return dbNotificationService.save(entity);
    }

    @Override
    public boolean saveBatch(List<Notification> entities) {
        return dbNotificationService.saveBatch(entities);
    }

    @Override
    public Notification getById(Serializable id) {
        return dbNotificationService.getById(id);
    }

    /**
     * 获取未读数
     * @param receiverId 接收者用户ID
     * @return
     */
    @Override
    public long getUnreadCount(Long receiverId) {
        String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, receiverId);
        String cached = stringRedisTemplate.opsForValue().get(unreadKey);

        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                // 缓存值异常，继续走 DB
                // todo redis挂了 需要兜底 但是不能直接打到db上
            }
        }

        // 查询 DB
        long count = dbNotificationService.getUnreadCount(receiverId);

        // 回填 Redis
        stringRedisTemplate.opsForValue().set(unreadKey, String.valueOf(count),
                RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);

        return count;
    }

    @Override
    public Page<NotificationVO> listNotifications(Long receiverId, NotificationQueryDTO queryDTO) {
        // 列表不做缓存，直委托 DB
        return dbNotificationService.listNotifications(receiverId, queryDTO);
    }

    /**
     * 标记已读
     * @param id         通知ID
     * @param receiverId 接收者用户ID（防越权）
     * @return
     */
    @Override
    public boolean markAsRead(Long id, Long receiverId) {
        boolean result = dbNotificationService.markAsRead(id, receiverId);
        if (result) {
            // Redis 未读数 -1（最小为 0）
            String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, receiverId);
            Long newCount = stringRedisTemplate.opsForValue().decrement(unreadKey);
            if (newCount != null && newCount < 0) {
                stringRedisTemplate.opsForValue().set(unreadKey, "0",
                        RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);
            }
        }
        return result;
    }

    /**
     * 全部已读
     * @param receiverId 接收者用户ID
     * @return
     */
    @Override
    public boolean markAllAsRead(Long receiverId) {
        boolean result = dbNotificationService.markAllAsRead(receiverId);
        if (result) {
            // 全量失效 Redis 未读数
            String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, receiverId);
            stringRedisTemplate.delete(unreadKey);
        }
        return result;
    }

    /**
     * 删除单条通知（软删除）
     * @param id         通知ID
     * @param receiverId 接收者用户ID（防越权）
     * @return
     */
    @Override
    public boolean deleteNotification(Long id, Long receiverId) {
        // 先查询通知状态（判断是否需要减少未读计数）
        Notification notification = dbNotificationService.getById(id);
        boolean result = dbNotificationService.deleteNotification(id, receiverId);
        if (result && notification != null
                && ObjUtil.equal(notification.getReceiverId(), receiverId)
                && notification.getIsRead() != null && notification.getIsRead() == 0) {
            // 删除的是未读通知，未读数 -1
            String unreadKey = String.format(RedisKeyConstants.NOTIFICATION_UNREAD_KEY, receiverId);
            Long newCount = stringRedisTemplate.opsForValue().decrement(unreadKey);
            if (newCount != null && newCount < 0) {
                stringRedisTemplate.opsForValue().set(unreadKey, "0",
                        RedisKeyConstants.NOTIFICATION_UNREAD_TTL, TimeUnit.SECONDS);
            }
        }
        return result;
    }

    /**
     * 清空已读通知（软删除）
     * @param receiverId 接收者用户ID
     * @return 清除的数量
     */
    @Override
    public int cleanReadNotifications(Long receiverId) {
        return dbNotificationService.cleanReadNotifications(receiverId);
    }
}
