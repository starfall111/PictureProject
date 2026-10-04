package org.example.picture.moderation.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈列表项 VO
 *
 * @author Zou
 */
@Data
public class FeedbackListItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 反馈 ID
     */
    private Long id;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈类型
     */
    private String type;

    /**
     * 反馈类型描述
     */
    private String typeDesc;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
