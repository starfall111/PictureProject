package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 封禁类型枚举
 *
 * @author Zou
 */
@Getter
public enum BanTypeEnum {

    NONE("NONE", "正常"),
    TEMP("TEMP", "临时封禁"),
    PERMANENT("PERMANENT", "永久封禁");

    private final String type;
    private final String desc;

    BanTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static BanTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (BanTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
