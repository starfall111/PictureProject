package org.example.picture.moderation.domain.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈状态枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    REJECTED("REJECTED", "已拒绝"),
    REOPENED("REOPENED", "已重新打开"),
    CLOSED("CLOSED", "已关闭");

    private final String type;
    private final String desc;

    FeedbackStatusEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackStatusEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackStatusEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
