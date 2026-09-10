package org.example.notification.api.port;

import java.util.Date;
import java.util.List;

/**
 * 用户查询端口 — 由启动装配模块提供实现（防腐层）
 * <p>
 * 通知上下文发送系统消息时需要解析收件人，用户数据的所有权在身份模块，
 * 通过该端口获取，避免通知模块直接依赖身份模块的持久层。
 *
 * @author Zou
 */
public interface UserLookupPort {

    /**
     * 查询全部用户 ID（系统消息全量发送）
     */
    List<Long> listAllUserIds();

    /**
     * 按条件筛选用户 ID（系统消息定向发送）
     *
     * @param filterRole        角色过滤
     * @param filterSpaceLevel  空间等级过滤
     * @param filterRegisterStart 注册时间起
     * @param filterRegisterEnd   注册时间止
     */
    List<Long> listUserIdsByFilter(String filterRole, Integer filterSpaceLevel,
                                   Date filterRegisterStart, Date filterRegisterEnd);
}
