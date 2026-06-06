package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈来源枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackSourceEnum {

    APP("APP", "应用内"),
    SYSTEM("SYSTEM", "系统自动"),
    ADMIN("ADMIN", "管理员创建");

    private final String type;
    private final String desc;

    FeedbackSourceEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackSourceEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackSourceEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
