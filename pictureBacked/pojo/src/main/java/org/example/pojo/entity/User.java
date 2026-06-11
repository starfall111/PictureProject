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

    /** VIP会员类型: 0-普通用户, 1-VIP会员 */
    private Integer vipType;

    /** VIP到期时间 */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private Date vipExpireTime;

    /** 最近一次激活VIP时间 */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private Date vipActivatedAt;

    /** VIP累计总天数 */
    private Integer vipTotalDays;

    /**
     * 封禁状态：NONE/TEMP/PERMANENT
     */
    private String banStatus;

    /**
     * 封禁结束时间
     */
    private Date banEndTime;

    /**
     * 累计违规次数（6个月滚动窗口）
     */
    private Integer violationCount;

    /**
     * 最近一次违规时间
     */
    private Date lastViolationTime;

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