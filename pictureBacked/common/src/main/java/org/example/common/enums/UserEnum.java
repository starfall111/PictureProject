package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserEnum {

    USER("普通用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    UserEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    //根据value 获取枚举值
    public static UserEnum getByValue(String value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (UserEnum userEnum : UserEnum.values()){
            if (userEnum.value.equals(value)){
                return userEnum;
            }
        }

        return null;
    }
}
