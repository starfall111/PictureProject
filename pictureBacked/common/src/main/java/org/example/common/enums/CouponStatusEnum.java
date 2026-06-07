package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum CouponStatusEnum {
    UNSOLD("未发放", 0),
    CLAIMED("已领取未使用", 1),
    ACTIVATED("已激活使用中", 2),
    EXPIRED("已过期", 3);

    private final String text;
    private final int value;

    CouponStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static CouponStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) return null;
        for (CouponStatusEnum e : CouponStatusEnum.values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
