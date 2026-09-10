package org.example.marketing.interfaces.vo;

import lombok.Data;

import java.util.Date;

/**
 * 编码券激活结果视图对象
 *
 * @author Zou
 */
@Data
public class CouponActivateVO {
    private Boolean activated;
    private Integer vipType;
    private String vipTypeName;
    private Date vipExpireTime;
    private Integer couponType;
    private String couponTypeName;
    private String message;
}
