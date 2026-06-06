package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 举报原因枚举
 *
 * @author Zou
 */
@Getter
public enum ReportReasonEnum {

    PORNOGRAPHY("PORNOGRAPHY", "色情内容", "HIGH"),
    VIOLENCE("VIOLENCE", "暴力内容", "HIGH"),
    SCAM("SCAM", "诈骗信息", "HIGH"),
    COPYRIGHT("COPYRIGHT", "侵权内容", "MEDIUM"),
    SPAM("SPAM", "垃圾信息", "MEDIUM"),
    HARASSMENT("HARASSMENT", "恶意行为", "MEDIUM"),
    OTHER("OTHER", "其他", "LOW");

    private final String type;
    private final String desc;
    private final String level;

    ReportReasonEnum(String type, String desc, String level) {
        this.type = type;
        this.desc = desc;
        this.level = level;
    }

    public static ReportReasonEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (ReportReasonEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
