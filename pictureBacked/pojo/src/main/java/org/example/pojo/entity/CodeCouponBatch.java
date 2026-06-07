package org.example.pojo.entity;

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
 * 编码券发放批次
 *
 * @author Zou
 * @TableName code_coupon_batch
 */
@TableName(value = "code_coupon_batch")
@Data
public class CodeCouponBatch implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 批次名称
     */
    private String name;

    /**
     * 券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP
     */
    private Integer type;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 当前剩余库存
     */
    private Integer currentStock;

    /**
     * 秒杀开始时间
     */
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    private Date endTime;

    /**
     * 0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

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
