package org.example.pojo.dto.notification;

import lombok.Data;
import org.example.pojo.PageRequest;

/**
 * 通知查询 DTO
 *
 * @author Zou
 */
@Data
public class NotificationQueryDTO extends PageRequest {

    /**
     * 通知类型筛选（可选）
     */
    private String type;

    /**
     * 已读状态筛选（可选）：0=未读 1=已读
     */
    private Integer isRead;
}
