package org.example.picture.moderation.domain.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈类型枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackTypeEnum {

    BUG("BUG", "Bug报告"),
    FEATURE("FEATURE", "功能建议"),
    ACCOUNT("ACCOUNT", "账号问题"),
    EXPERIENCE("EXPERIENCE", "体验反馈"),
    OTHER("OTHER", "其他");

    private final String type;
    private final String desc;

    FeedbackTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
