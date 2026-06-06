package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈回复类型枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackReplyTypeEnum {

    USER_REPLY("USER_REPLY", "用户回复"),
    ADMIN_REPLY("ADMIN_REPLY", "管理员回复"),
    INTERNAL_NOTE("INTERNAL_NOTE", "内部备注");

    private final String type;
    private final String desc;

    FeedbackReplyTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackReplyTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackReplyTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
