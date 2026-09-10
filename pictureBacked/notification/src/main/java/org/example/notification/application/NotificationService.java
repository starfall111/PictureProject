package org.example.notification.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.notification.interfaces.dto.NotificationQueryDTO;
import org.example.notification.domain.model.Notification;
import org.example.notification.interfaces.vo.NotificationVO;

import java.io.Serializable;
import java.util.List;

/**
 * 通知服务接口
 *
 * @author Zou
 */
public interface NotificationService {

    /**
     * 保存通知
     */
    boolean save(Notification entity);

    /**
     * 批量保存通知
     */
    boolean saveBatch(List<Notification> entities);

    /**
     * 根据 ID 获取通知
     */
    Notification getById(Serializable id);

    /**
     * 获取未读通知数量
     *
     * @param receiverId 接收者用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long receiverId);

    /**
     * 获取分页通知列表
     *
     * @param receiverId 接收者用户ID
     * @param queryDTO   查询条件
     * @return 通知分页
     */
    Page<NotificationVO> listNotifications(Long receiverId, NotificationQueryDTO queryDTO);

    /**
     * 标记单条通知为已读
     *
     * @param id         通知ID
     * @param receiverId 接收者用户ID（防越权）
     * @return 是否成功
     */
    boolean markAsRead(Long id, Long receiverId);

    /**
     * 标记所有通知为已读
     *
     * @param receiverId 接收者用户ID
     * @return 是否成功
     */
    boolean markAllAsRead(Long receiverId);

    /**
     * 删除单条通知（软删除）
     *
     * @param id         通知ID
     * @param receiverId 接收者用户ID（防越权）
     * @return 是否成功
     */
    boolean deleteNotification(Long id, Long receiverId);

    /**
     * 清空已读通知（软删除）
     *
     * @param receiverId 接收者用户ID
     * @return 清除的数量
     */
    int cleanReadNotifications(Long receiverId);
}
