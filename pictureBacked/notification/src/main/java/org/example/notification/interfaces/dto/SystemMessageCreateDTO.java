package org.example.notification.interfaces.dto;

import lombok.Data;

import java.util.Date;

/**
 * 系统消息创建/编辑 DTO
 *
 * @author Zou
 */
@Data
public class SystemMessageCreateDTO {

    /**
     * ID（编辑时必传）
     */
    private Long id;

    /**
     * 标题（必填）
     */
    private String title;

    /**
     * 内容（必填）
     */
    private String content;

    /**
     * 发送模式: DIRECT / RABBITMQ（默认 DIRECT）
     */
    private String sendMode;

    /**
     * 目标类型: ALL / FILTER（默认 ALL）
     */
    private String targetType;

    /**
     * 按角色筛选（targetType=FILTER 时使用）
     */
    private String filterRole;

    /**
     * 按空间等级筛选（targetType=FILTER 时使用）
     */
    private Integer filterSpaceLevel;

    /**
     * 注册时间起始（targetType=FILTER 时使用）
     */
    private Date filterRegisterStart;

    /**
     * 注册时间截止（targetType=FILTER 时使用）
     */
    private Date filterRegisterEnd;
}
