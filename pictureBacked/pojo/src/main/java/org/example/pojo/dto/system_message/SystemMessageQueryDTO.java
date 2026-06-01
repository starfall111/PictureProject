package org.example.pojo.dto.system_message;

import lombok.Data;
import org.example.pojo.PageRequest;

/**
 * 系统消息查询 DTO
 *
 * @author Zou
 */
@Data
public class SystemMessageQueryDTO extends PageRequest {

    /**
     * 标题模糊搜索（可选）
     */
    private String title;

    /**
     * 状态筛选（可选）: 0=草稿 1=已发布 2=已下架
     */
    private Integer status;

    /**
     * 发送模式筛选（可选）: DIRECT / RABBITMQ
     */
    private String sendMode;
}
