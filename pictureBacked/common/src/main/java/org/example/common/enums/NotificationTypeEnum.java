package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 *
 * @author Zou
 */
@Getter
public enum NotificationTypeEnum {

    LIKE("LIKE", "点赞"),
    FAVORITE("FAVORITE", "收藏"),
    COMMENT("COMMENT", "评论"),
    FOLLOW("FOLLOW", "关注"),
    SYSTEM("SYSTEM", "系统通知"),
    REVIEW("REVIEW", "审批通知");

    private final String type;
    private final String desc;

    NotificationTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据 type 获取枚举值
     */
    public static NotificationTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (NotificationTypeEnum e : NotificationTypeEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
