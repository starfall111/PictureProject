package org.example.notification.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.notification.interfaces.dto.SystemMessageCreateDTO;
import org.example.notification.interfaces.dto.SystemMessageQueryDTO;
import org.example.notification.domain.model.SystemMessage;
import org.example.notification.interfaces.vo.SystemMessageVO;
import org.example.notification.infrastructure.mq.RabbitMQConfig;
import org.example.notification.infrastructure.persistence.SystemMessageMapper;
import org.example.notification.api.port.UserLookupPort;
import org.example.notification.application.SystemMessageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统消息服务实现
 *
 * @author Zou
 */
@Slf4j
@Service
public class SystemMessageServiceImpl extends ServiceImpl<SystemMessageMapper, SystemMessage>
        implements SystemMessageService {

    @Resource
    private UserLookupPort userLookupPort;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int BATCH_SIZE = 500;

    /**
     * 创建系统消息草稿
     * @param dto
     * @param publisherId
     * @return
     */
    @Override
    public Long createSystemMessage(SystemMessageCreateDTO dto, Long publisherId) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getContent()), ErrorCode.PARAMS_ERROR, "内容不能为空");

        SystemMessage entity = new SystemMessage();
        BeanUtil.copyProperties(dto, entity);
        entity.setPublisherId(publisherId);
        entity.setStatus(0);
        if (ObjUtil.isEmpty(entity.getSendMode())) {
            entity.setSendMode("DIRECT");
        }
        if (ObjUtil.isEmpty(entity.getTargetType())) {
            entity.setTargetType("ALL");
        }

        save(entity);
        return entity.getId();
    }

    /**
     * 修改系统消息
     * @param id
     * @param dto
     * @return
     */
    @Override
    public Boolean updateSystemMessage(Long id, SystemMessageCreateDTO dto) {
        SystemMessage entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.PARAMS_ERROR, "系统消息不存在");
        ThrowUtils.throwIf(entity.getStatus() != 0, ErrorCode.PARAMS_ERROR, "只能编辑草稿状态的消息");

        if (ObjUtil.isNotEmpty(dto.getTitle())) {
            entity.setTitle(dto.getTitle());
        }
        if (ObjUtil.isNotEmpty(dto.getContent())) {
            entity.setContent(dto.getContent());
        }
        if (ObjUtil.isNotEmpty(dto.getSendMode())) {
            entity.setSendMode(dto.getSendMode());
        }
        if (ObjUtil.isNotEmpty(dto.getTargetType())) {
            entity.setTargetType(dto.getTargetType());
        }
        entity.setFilterRole(dto.getFilterRole());
        entity.setFilterSpaceLevel(dto.getFilterSpaceLevel());
        entity.setFilterRegisterStart(dto.getFilterRegisterStart());
        entity.setFilterRegisterEnd(dto.getFilterRegisterEnd());

        return updateById(entity);
    }

    /**
     * 删除系统消息
     * @param id
     * @return
     */
    @Override
    public Boolean deleteSystemMessage(Long id) {
        SystemMessage entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.PARAMS_ERROR, "系统消息不存在");
        return removeById(id);
    }

    @Override
    public Page<SystemMessageVO> listSystemMessages(SystemMessageQueryDTO dto) {
        int current = dto.getCurrent();
        int pageSize = Math.min(dto.getPageSize(), 50);

        QueryWrapper<SystemMessage> qw = new QueryWrapper<>();
        if (ObjUtil.isNotEmpty(dto.getTitle())) {
            qw.like("title", dto.getTitle());
        }
        if (dto.getStatus() != null) {
            qw.eq("status", dto.getStatus());
        }
        if (ObjUtil.isNotEmpty(dto.getSendMode())) {
            qw.eq("sendMode", dto.getSendMode());
        }
        qw.orderByDesc("createTime");

        Page<SystemMessage> page = page(new Page<>(current, pageSize), qw);

        Page<SystemMessageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(msg -> BeanUtil.copyProperties(msg, SystemMessageVO.class))
                .toList());
        return voPage;
    }

    /**
     * 发布通知
     * @param id
     * @return
     */
    @Override
    public Boolean publishSystemMessage(Long id) {
        SystemMessage msg = getById(id);
        ThrowUtils.throwIf(msg == null, ErrorCode.PARAMS_ERROR, "系统消息不存在");
//        ThrowUtils.throwIf(msg.getStatus() != 0, ErrorCode.PARAMS_ERROR, "只能发布草稿状态的消息");

        // 1. 解析目标用户
        List<Long> targetUserIds = resolveTargetUsers(msg);
        ThrowUtils.throwIf(targetUserIds.isEmpty(), ErrorCode.PARAMS_ERROR, "没有符合条件的目标用户");

        // 2. 统一走 MQ 异步发送
        doMqSend(targetUserIds, msg);

        // 3. 更新状态为已发布
        msg.setStatus(1);
        msg.setPublishTime(new Date());
        updateById(msg);

        log.info("系统消息已发布：id={}, sendMode={}, targetType={}, 目标用户数={}",
                id, msg.getSendMode(), msg.getTargetType(), targetUserIds.size());
        return true;
    }

    /**
     * 下架系统消息
     * @param id
     * @return
     */
    @Override
    public Boolean revokeSystemMessage(Long id) {
        SystemMessage msg = getById(id);
        ThrowUtils.throwIf(msg == null, ErrorCode.PARAMS_ERROR, "系统消息不存在");
        ThrowUtils.throwIf(msg.getStatus() != 1, ErrorCode.PARAMS_ERROR, "只能下架已发布的消息");

        msg.setStatus(2);
        return updateById(msg);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析目标用户ID列表
     */
    private List<Long> resolveTargetUsers(SystemMessage msg) {
        if ("ALL".equals(msg.getTargetType())) {
            return userLookupPort.listAllUserIds();
        }
        return userLookupPort.listUserIdsByFilter(
                msg.getFilterRole(),
                msg.getFilterSpaceLevel(),
                msg.getFilterRegisterStart(),
                msg.getFilterRegisterEnd()
        );
    }

    /**
     * RabbitMQ 模式
     */
    private void doMqSend(List<Long> userIds, SystemMessage msg) {
        List<List<Long>> partitions = partition(userIds, BATCH_SIZE);
        for (List<Long> batch : partitions) {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("messageId", msg.getId());
                message.put("title", msg.getTitle());
                message.put("content", msg.getContent());
                message.put("userIds", batch);

                String json = objectMapper.writeValueAsString(message);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SYSTEM_MSG_EXCHANGE,
                        RabbitMQConfig.SYSTEM_MSG_ROUTING_KEY,
                        json
                );
            } catch (Exception e) {
                log.error("发送系统消息到 RabbitMQ 失败：messageId={}", msg.getId(), e);
                throw new RuntimeException("消息发送失败", e);
            }
        }
    }

    /**
     * 分片工具
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }


}
