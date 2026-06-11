package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 封禁时长枚举
 *
 * @author Zou
 */
@Getter
public enum BanDurationEnum {

    DAYS_3(3, "3天"),
    DAYS_7(7, "7天"),
    DAYS_30(30, "30天");

    private final int days;
    private final String desc;

    BanDurationEnum(int days, String desc) {
        this.days = days;
        this.desc = desc;
    }

    public static BanDurationEnum getByDays(int days) {
        for (BanDurationEnum e : values()) {
            if (e.days == days) {
                return e;
            }
        }
        return null;
    }
}
