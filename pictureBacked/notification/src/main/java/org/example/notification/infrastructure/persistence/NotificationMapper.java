package org.example.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.notification.domain.model.Notification;

/**
 * 针对表【notification(站内通知)】的数据库操作Mapper
 *
 * @author Zou
 */
public interface NotificationMapper extends BaseMapper<Notification> {
}
