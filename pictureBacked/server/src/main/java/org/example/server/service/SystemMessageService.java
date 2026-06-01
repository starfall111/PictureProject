package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.dto.system_message.SystemMessageCreateDTO;
import org.example.pojo.dto.system_message.SystemMessageQueryDTO;
import org.example.pojo.vo.SystemMessageVO;

/**
 * 系统消息服务接口
 *
 * @author Zou
 */
public interface SystemMessageService {

    /**
     * 创建系统消息（草稿）
     */
    Long createSystemMessage(SystemMessageCreateDTO dto, Long publisherId);

    /**
     * 编辑系统消息
     */
    Boolean updateSystemMessage(Long id, SystemMessageCreateDTO dto);

    /**
     * 删除系统消息
     */
    Boolean deleteSystemMessage(Long id);

    /**
     * 分页查询系统消息列表
     */
    Page<SystemMessageVO> listSystemMessages(SystemMessageQueryDTO dto);

    /**
     * 发布系统消息
     */
    Boolean publishSystemMessage(Long id);

    /**
     * 下架系统消息
     */
    Boolean revokeSystemMessage(Long id);
}
