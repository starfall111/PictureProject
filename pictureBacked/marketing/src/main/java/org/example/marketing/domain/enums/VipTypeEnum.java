package org.example.marketing.domain.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum VipTypeEnum {
    NORMAL("普通用户", 0),
    VIP("VIP会员", 1);

    private final String text;
    private final int value;

    VipTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static VipTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) return null;
        for (VipTypeEnum e : VipTypeEnum.values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
