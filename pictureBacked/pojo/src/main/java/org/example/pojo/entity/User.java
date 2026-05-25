package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.FileDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @author Zou
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 手机号
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userPhone;

    /**
     * 邮箱
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userEmail;

    /**
     * 密码
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userPassword;

    /**
     * 用户昵称
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userName;

    /**
     * 用户头像
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userAvatar;

    /**
     * 用户简介
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

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