package org.example.pojo.vo.report;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 封禁记录 VO
 *
 * @author Zou
 */
@Data
public class BanRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 封禁记录 ID
     */
    private Long id;

    /**
     * 被封禁用户 ID
     */
    private Long userId;

    /**
     * 被封禁用户名
     */
    private String userName;

    /**
     * 封禁类型
     */
    private String banType;

    /**
     * 封禁类型描述
     */
    private String banTypeDesc;

    /**
     * 封禁时长（天）
     */
    private Integer banDuration;

    /**
     * 封禁开始时间
     */
    private Date banStartTime;

    /**
     * 封禁结束时间
     */
    private Date banEndTime;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 违规次数
     */
    private Integer violationCount;

    /**
     * 是否已解封
     */
    private Boolean unbanned;

    /**
     * 解封时间
     */
    private Date unbanTime;

    /**
     * 解封原因
     */
    private String unbanReason;

    /**
     * 封禁操作人用户名
     */
    private String banOperatorName;

    /**
     * 关联举报记录 ID
     */
    private Long reportId;

    /**
     * 创建时间
     */
    private Date createTime;
}
