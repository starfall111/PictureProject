package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 举报目标类型枚举
 *
 * @author Zou
 */
@Getter
public enum ReportTargetTypeEnum {

    PICTURE("PICTURE", "图片"),
    USER("USER", "用户");

    private final String type;
    private final String desc;

    ReportTargetTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static ReportTargetTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (ReportTargetTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
