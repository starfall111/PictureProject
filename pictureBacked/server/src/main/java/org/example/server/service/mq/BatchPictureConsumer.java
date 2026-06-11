package org.example.server.service.mq;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.pojo.dto.picture.FileDTO;
import org.example.pojo.entity.BatchTask;
import org.example.pojo.entity.Picture;
import org.example.server.mapper.BatchTaskMapper;
import org.example.server.service.PictureService;
import org.example.server.websocket.BatchWebSocketHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 批量获取图片 MQ 消费者
 * <p>
 * 消费 batch.picture.queue，逐张下载图片并上传 OSS，通过 WebSocket 推送进度。
 *
 * @author Zou
 */
@Slf4j
@Component
public class BatchPictureConsumer {

    @Resource
    private BatchTaskMapper batchTaskMapper;

    @Resource(name = "cachedPictureService")
    private PictureService pictureService;

    @Resource
    private BatchWebSocketHandler batchWebSocketHandler;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "batch.picture.queue")
    public void consume(Message<String> message, Channel channel) throws Exception {
        long deliveryTag = 0;
        try {
            MessageHeaders headers = message.getHeaders();
            deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

            String payload = message.getPayload();
            JsonNode root = objectMapper.readTree(payload);

            Long taskId = root.get("taskId").asLong();
            Long userId = root.get("userId").asLong();
            Long spaceId = root.has("spaceId") && !root.get("spaceId").isNull() ? root.get("spaceId").asLong() : null;
            Long categoryId = root.has("categoryId") && !root.get("categoryId").isNull() ? root.get("categoryId").asLong() : null;
            String namePrefix = root.has("namePrefix") ? root.get("namePrefix").asText() : "";
            String tagsStr = root.has("tags") && !root.get("tags").isNull() ? root.get("tags").asText() : null;

            List<String> imageUrls = new ArrayList<>();
            List<String> imageNames = new ArrayList<>();
            List<String> imageIntros = new ArrayList<>();
            for (JsonNode node : root.get("imageUrls")) imageUrls.add(node.asText());
            for (JsonNode node : root.get("imageNames")) imageNames.add(node.isNull() ? null : node.asText());
            for (JsonNode node : root.get("imageIntros")) imageIntros.add(node.isNull() ? null : node.asText());

            // 更新任务状态为处理中
            updateTaskStatus(taskId, "PROCESSING");

            int total = imageUrls.size();
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < total; i++) {
                String url = imageUrls.get(i);
                try {
                    // 构建 FileDTO
                    FileDTO fileDTO = new FileDTO();
                    String name = i < imageNames.size() ? imageNames.get(i) : null;
                    if (StrUtil.isNotBlank(name)) {
                        fileDTO.setName(name);
                    } else if (StrUtil.isNotBlank(namePrefix)) {
                        fileDTO.setName(namePrefix + (i + 1));
                    }
                    String intro = i < imageIntros.size() ? imageIntros.get(i) : null;
                    if (StrUtil.isNotBlank(intro)) {
                        fileDTO.setIntroduction(intro);
                    }
                    if (ObjUtil.isNotEmpty(categoryId)) {
                        fileDTO.setCategoryId(categoryId);
                    }
                    if (StrUtil.isNotBlank(tagsStr)) {
                        fileDTO.setTags(tagsStr);
                    }
                    if (ObjUtil.isNotEmpty(spaceId)) {
                        fileDTO.setSpaceId(spaceId);
                    }
                    fileDTO.setUserId(userId);

                    // 执行上传（复用现有 upload 逻辑）
                    Picture picture = pictureService.upload(url, fileDTO);
                    log.info("批量任务图片上传成功: taskId={}, pictureId={}", taskId, picture.getId());
                    successCount++;

                } catch (Exception e) {
                    log.error("批量任务图片处理失败: taskId={}, url={}", taskId, url, e);
                    failCount++;
                }

                // 更新进度
                updateTaskProgress(taskId, successCount, failCount);

                // 缓存进度到 Redis
                cacheProgress(taskId, successCount + failCount, total, successCount, failCount);

                // WebSocket 推送进度
                Map<String, Object> progressMsg = new HashMap<>();
                progressMsg.put("type", "batch_progress");
                progressMsg.put("taskId", taskId);
                progressMsg.put("finished", successCount + failCount);
                progressMsg.put("total", total);
                progressMsg.put("success", successCount);
                progressMsg.put("fail", failCount);
                batchWebSocketHandler.sendToUser(userId, progressMsg);

            }

            // 更新任务完成
            LambdaUpdateWrapper<BatchTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BatchTask::getId, taskId)
                    .set(BatchTask::getStatus, "COMPLETED")
                    .set(BatchTask::getSuccessCount, successCount)
                    .set(BatchTask::getFailCount, failCount)
                    .set(BatchTask::getFinishTime, new Date());
            batchTaskMapper.update(null, updateWrapper);

            // WebSocket 推送完成通知
            Map<String, Object> completeMsg = new HashMap<>();
            completeMsg.put("type", "batch_complete");
            completeMsg.put("taskId", taskId);
            completeMsg.put("total", total);
            completeMsg.put("success", successCount);
            completeMsg.put("fail", failCount);
            batchWebSocketHandler.sendToUser(userId, completeMsg);

            // 手动 ACK
            if (deliveryTag > 0) {
                channel.basicAck(deliveryTag, false);
            }

            log.info("批量任务完成: taskId={}, success={}, fail={}", taskId, successCount, failCount);

        } catch (Exception e) {
            log.error("批量任务消费失败", e);
            if (deliveryTag > 0) {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private void updateTaskStatus(Long taskId, String status) {
        LambdaUpdateWrapper<BatchTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BatchTask::getId, taskId).set(BatchTask::getStatus, status);
        batchTaskMapper.update(null, wrapper);
    }

    private void updateTaskProgress(Long taskId, int successCount, int failCount) {
        LambdaUpdateWrapper<BatchTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BatchTask::getId, taskId)
                .set(BatchTask::getSuccessCount, successCount)
                .set(BatchTask::getFailCount, failCount);
        batchTaskMapper.update(null, wrapper);
    }

    private void cacheProgress(Long taskId, int finished, int total, int success, int fail) {
        try {
            String key = String.format(RedisKeyConstants.BATCH_TASK_PROGRESS_KEY, taskId);
            String value = String.format("{\"finished\":%d,\"total\":%d,\"success\":%d,\"fail\":%d}",
                    finished, total, success, fail);
            stringRedisTemplate.opsForValue().set(key, value,
                    RedisKeyConstants.BATCH_TASK_PROGRESS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("批量任务进度缓存失败: taskId={}", taskId, e);
        }
    }
}
