package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 举报处理结果枚举
 *
 * @author Zou
 */
@Getter
public enum HandleResultEnum {

    REJECT("REJECT", "驳回"),
    WARN("WARN", "警告"),
    BAN_TEMP_3("BAN_TEMP_3", "封禁3天"),
    BAN_TEMP_7("BAN_TEMP_7", "封禁7天"),
    BAN_TEMP_30("BAN_TEMP_30", "封禁30天"),
    BAN_PERMANENT("BAN_PERMANENT", "永久封禁"),
    REMOVE_PICTURE("REMOVE_PICTURE", "下架图片"),
    RESTORE_PICTURE("RESTORE_PICTURE", "恢复图片");

    private final String type;
    private final String desc;

    HandleResultEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static HandleResultEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (HandleResultEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 是否涉及封禁操作
     */
    public boolean isBanAction() {
        return this == BAN_TEMP_3 || this == BAN_TEMP_7 || this == BAN_TEMP_30 || this == BAN_PERMANENT;
    }
}
