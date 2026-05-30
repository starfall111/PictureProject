package org.example.pojo.vo;

import lombok.Data;

import java.util.Date;

/**
 * 系统消息 VO（管理端返回）
 *
 * @author Zou
 */
@Data
public class SystemMessageVO {

    private Long id;

    private String title;

    private String content;

    private String sendMode;

    private String targetType;

    private String filterRole;

    private Integer filterSpaceLevel;

    private Date filterRegisterStart;

    private Date filterRegisterEnd;

    private Integer status;

    private Long publisherId;

    private Date publishTime;

    private Date createTime;

    private Date updateTime;
}
