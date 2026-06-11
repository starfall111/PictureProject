package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 反馈关闭原因枚举
 *
 * @author Zou
 */
@Getter
public enum FeedbackCloseReasonEnum {

    USER_WITHDRAW("USER_WITHDRAW", "用户撤回"),
    AUTO_CLOSE("AUTO_CLOSE", "系统自动关闭"),
    ADMIN_CLOSE("ADMIN_CLOSE", "管理员关闭"),
    USER_CONFIRM("USER_CONFIRM", "用户确认解决");

    private final String type;
    private final String desc;

    FeedbackCloseReasonEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static FeedbackCloseReasonEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (FeedbackCloseReasonEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
