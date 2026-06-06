package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 *
 * @author Zou
 */
@Getter
public enum NotificationTypeEnum {

    LIKE("LIKE", "点赞"),
    FAVORITE("FAVORITE", "收藏"),
    COMMENT("COMMENT", "评论"),
    FOLLOW("FOLLOW", "关注"),
    SYSTEM("SYSTEM", "系统通知"),

    // === 反馈相关 ===
    FEEDBACK_REPLIED("FEEDBACK_REPLIED", "反馈已回复"),
    FEEDBACK_RESOLVED("FEEDBACK_RESOLVED", "反馈已解决"),
    FEEDBACK_REJECTED("FEEDBACK_REJECTED", "反馈已拒绝"),

    // === 举报相关 ===
    REPORT_ACCEPTED("REPORT_ACCEPTED", "举报已受理"),
    REPORT_REJECTED("REPORT_REJECTED", "举报已驳回"),

    // === 被举报人相关 ===
    USER_WARNED("USER_WARNED", "违规警告"),
    USER_BANNED("USER_BANNED", "账号封禁"),
    USER_UNBANNED("USER_UNBANNED", "账号解封"),

    // === 图片相关 ===
    PICTURE_REMOVED("PICTURE_REMOVED", "图片下架"),
    PICTURE_RESTORED("PICTURE_RESTORED", "图片恢复"),

    // === 封禁相关 ===
    BAN_EXPIRING("BAN_EXPIRING", "封禁即将到期"),
    REPORT_RESTRICTED("REPORT_RESTRICTED", "举报权限暂停");

    private final String type;
    private final String desc;

    NotificationTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据 type 获取枚举值
     */
    public static NotificationTypeEnum getByType(String type) {
        if (ObjUtil.isEmpty(type)) {
            return null;
        }
        for (NotificationTypeEnum e : NotificationTypeEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
