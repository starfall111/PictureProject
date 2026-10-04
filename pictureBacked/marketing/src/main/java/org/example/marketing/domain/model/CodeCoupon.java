package org.example.marketing.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 编码券
 *
 * @author Zou
 * @TableName code_coupon
 */
@TableName(value = "code_coupon")
@Data
public class CodeCoupon implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 唯一编码券码
     */
    private String code;

    /**
     * 3/7/30天
     */
    private Integer type;

    /**
     * 0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款
     */
    private Integer status;

    /**
     * 领取时间
     */
    private Date issuedAt;

    /**
     * 激活时间
     */
    private Date activatedAt;

    /**
     * 过期时间
     */
    private Date expireAt;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

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
