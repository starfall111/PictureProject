package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author Zou
 */

@Getter
public enum ReviewStatusEnum {
    REVIEWING("待审核",0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;
    private final int value;

    ReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    //根据value 获取枚举值
    public static ReviewStatusEnum getByValue(int value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (ReviewStatusEnum reviewStatusEnum : ReviewStatusEnum.values()){
            if (reviewStatusEnum.value == value){
                return reviewStatusEnum;
            }
        }

        return null;
    }
}
