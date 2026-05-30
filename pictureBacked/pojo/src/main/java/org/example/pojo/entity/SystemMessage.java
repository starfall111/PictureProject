package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 系统消息
 *
 * @author Zou
 * @TableName systemMessage
 */
@TableName(value = "systemMessage")
@Data
public class SystemMessage implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 发送模式: DIRECT / RABBITMQ
     */
    private String sendMode;

    /**
     * 目标类型: ALL / FILTER
     */
    private String targetType;

    /**
     * 按角色筛选: user / admin
     */
    private String filterRole;

    /**
     * 按空间等级筛选: 0=普通 1=专业 2=旗舰
     */
    private Integer filterSpaceLevel;

    /**
     * 注册时间起始
     */
    private Date filterRegisterStart;

    /**
     * 注册时间截止
     */
    private Date filterRegisterEnd;

    /**
     * 状态: 0=草稿 1=已发布 2=已下架
     */
    private Integer status;

    /**
     * 发布管理员ID
     */
    private Long publisherId;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
