package org.example.marketing.interfaces.vo;

import lombok.Data;

import java.util.Date;

/**
 * 编码券视图对象
 *
 * @author Zou
 */
@Data
public class CouponVO {
    private Long id;
    private String code;
    private Integer type;
    private String typeName;
    private Integer status;
    private String statusName;
    private Date issuedAt;
    private Date activatedAt;
    private Date expireAt;
    private Long remainingDays;
}
