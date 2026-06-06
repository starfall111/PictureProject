package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 举报状态枚举
 *
 * @author Zou
 */
@Getter
public enum ReportStatusEnum {

    PENDING("PENDING", "待审核"),
    PROCESSING("PROCESSING", "处理中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    WARN_ONLY("WARN_ONLY", "仅警告"),
    REMOVE_ONLY("REMOVE_ONLY", "仅下架");

    private final String type;
    private final String desc;

    ReportStatusEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static ReportStatusEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (ReportStatusEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
