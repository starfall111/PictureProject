package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * VIP 状态视图对象
 *
 * @author Zou
 */
@Data
public class VipStatusVO {
    private Boolean isVip;
    private Integer vipType;
    private String vipTypeName;
    private Date expireTime;
    private Long remainingDays;
    private Date activatedAt;
    private Integer totalDays;
}
