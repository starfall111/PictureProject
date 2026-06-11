package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈优先级枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackPriorityEnum {

    P0("P0", "紧急-2h内响应"),
    P1("P1", "高-8h内响应"),
    P2("P2", "中-24h内响应"),
    P3("P3", "低-72h内响应");

    private final String type;
    private final String desc;

    FeedbackPriorityEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackPriorityEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackPriorityEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
