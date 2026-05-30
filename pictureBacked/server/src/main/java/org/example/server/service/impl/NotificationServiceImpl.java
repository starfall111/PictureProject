package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.dto.notification.NotificationQueryDTO;
import org.example.pojo.entity.Notification;
import org.example.pojo.vo.NotificationVO;
import org.example.server.mapper.NotificationMapper;
import org.example.server.service.NotificationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

/**
 * 通知服务 DB 实现
 *
 * @author Zou
 */
@Slf4j
@Service("dbNotificationService")
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public boolean save(Notification entity) {
        return super.save(entity);
    }

    @Override
    public boolean saveBatch(List<Notification> entities) {
        return super.saveBatch(entities);
    }

    @Override
    public Notification getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public long getUnreadCount(Long receiverId) {
        QueryWrapper<Notification> qw = new QueryWrapper<>();
        qw.eq("receiverId", receiverId)
                .eq("isRead", 0);
        return notificationMapper.selectCount(qw);
    }

    @Override
    public Page<NotificationVO> listNotifications(Long receiverId, NotificationQueryDTO queryDTO) {
        int current = queryDTO.getCurrent();
        int pageSize = Math.min(queryDTO.getPageSize(), 50);

        QueryWrapper<Notification> qw = new QueryWrapper<>();
        qw.eq("receiverId", receiverId);
        if (ObjUtil.isNotEmpty(queryDTO.getType())) {
            qw.eq("type", queryDTO.getType());
        }
        if (queryDTO.getIsRead() != null) {
            qw.eq("isRead", queryDTO.getIsRead());
        }
        qw.orderByDesc("createTime");

        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(current, pageSize), qw);

        // 转换为 VO
        Page<NotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(n -> BeanUtil.copyProperties(n, NotificationVO.class))
                .toList());
        return voPage;
    }

    @Override
    public boolean markAsRead(Long id, Long receiverId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        return update(wrapper);
    }

    @Override
    public boolean markAllAsRead(Long receiverId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        return update(wrapper);
    }

    @Override
    public boolean deleteNotification(Long id, Long receiverId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
                .eq(Notification::getReceiverId, receiverId);
        return remove(wrapper);
    }

    @Override
    public int cleanReadNotifications(Long receiverId) {
        QueryWrapper<Notification> qw = new QueryWrapper<>();
        qw.eq("receiverId", receiverId)
                .eq("isRead", 1);
        // 利用 MyBatis-Plus 逻辑删除
        long count = count(new QueryWrapper<Notification>()
                .eq("receiverId", receiverId)
                .eq("isRead", 1));
        remove(qw);
        return (int) count;
    }
}
