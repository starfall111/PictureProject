package org.example.common.exception;

import lombok.Getter;

/**
 * @author Zou
 */

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),

    // ==================== 限流相关 ====================
    /**
     * 请求过于频繁，触发限流。
     * 由 {@code RateLimitAop} 抛出 {@code RateLimitException}，
     * {@code RateLimitExceptionHandler} 统一转换为 HTTP 429 + Retry-After。
     */
    RATE_LIMIT_EXCEEDED(42900, "请求过于频繁，请稍后再试"),

    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    UPLOAD_NO_PERMISSION(40102, "请先绑定手机号和邮箱后再上传图片"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),

    // ==================== Feedback 反馈相关 ====================
    FEEDBACK_RATE_LIMIT(40010, "今日反馈次数已达上限"),
    FEEDBACK_DUPLICATE(40011, "请勿重复提交"),
    FEEDBACK_NOT_FOUND(40012, "反馈不存在"),
    FEEDBACK_NO_PERMISSION(40013, "无权操作此反馈"),
    FEEDBACK_STATUS_ERROR(40014, "当前状态不允许此操作"),
    FEEDBACK_REOPEN_LIMIT(40015, "重新打开次数已达上限"),
    FEEDBACK_ATTACHMENT_LIMIT(40016, "附件数量超过上限（最多3张）"),
    FEEDBACK_ATTACHMENT_TOO_LARGE(40017, "附件大小超过限制（最大5MB）"),

    // ==================== Report 举报相关 ====================
    REPORT_RATE_LIMIT_EXCEEDED(40020, "举报频率超限，请稍后再试"),
    REPORT_DUPLICATE(40021, "请勿重复举报"),
    REPORT_TARGET_NOT_FOUND(40022, "举报目标不存在"),
    REPORT_SELF(40023, "不能举报自己"),
    REPORT_RESTRICTED(40024, "举报权限已被暂停"),
    REPORT_CANCEL_TIMEOUT(40025, "举报已超过撤回时间"),

    // ==================== Ban 封禁相关 ====================
    USER_BANNED(40102, "账号已被封禁"),
    USER_BANNED_PERMANENT(40103, "账号已被永久封禁"),
    USER_BANNED_EXPIRED(40104, "封禁已过期，请重新登录");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
