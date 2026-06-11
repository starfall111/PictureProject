package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.PageRequest;
import org.example.pojo.dto.BatchTaskQueryDTO;
import org.example.pojo.dto.BatchTaskUpdateDTO;
import org.example.pojo.dto.picture.PictureUploadByBatchDTO;
import org.example.pojo.entity.BatchTask;
import org.example.pojo.entity.User;
import org.example.pojo.vo.AdminBatchTaskVO;
import org.example.pojo.vo.BatchTaskStatsVO;
import org.example.pojo.vo.BatchTaskVO;
import org.example.server.config.RabbitMQConfig;
import org.example.server.mapper.BatchTaskMapper;
import org.example.server.service.BatchTaskService;
import org.example.server.strategy.imageSearch.ImageSearchStrategy;
import org.example.server.strategy.imageSearch.model.ImageSourceResult;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 批量获取图片任务服务实现
 *
 * @author Zou
 */
@Slf4j
@Service("dbBatchTaskService")
public class BatchTaskServiceImpl extends ServiceImpl<BatchTaskMapper, BatchTask>
        implements BatchTaskService {

    @Resource
    private List<ImageSearchStrategy> imageSearchStrategyList;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private Map<String, ImageSearchStrategy> imageSearchStrategyMap;

    @jakarta.annotation.PostConstruct
    private void initStrategyMap() {
        imageSearchStrategyMap = imageSearchStrategyList.stream()
                .collect(Collectors.toMap(ImageSearchStrategy::getSourceType, s -> s));
    }

    @Override
    public BatchTaskVO createAndSubmitTask(User user, PictureUploadByBatchDTO dto) {
        String searchText = dto.getSearchText();
        int count = dto.getCount();
        String namePrefix = StrUtil.blankToDefault(dto.getProfile(), searchText);
        String searchSource = dto.getSearchSource();

        // 根据 searchSource 选择策略并同步搜索图片
        ImageSearchStrategy strategy = imageSearchStrategyMap.get(searchSource);
        ThrowUtils.throwIf(strategy == null, ErrorCode.PARAMS_ERROR, "不支持的搜索来源: " + searchSource);
        List<ImageSourceResult> imageResults = strategy.searchImages(searchText, count);
        ThrowUtils.throwIf(imageResults == null || imageResults.isEmpty(), ErrorCode.OPERATION_ERROR, "未搜索到图片");

        // 创建任务记录
        BatchTask task = new BatchTask();
        task.setUserId(user.getId());
        task.setSpaceId(dto.getSpaceId());
        task.setSearchText(searchText);
        task.setSearchSource(searchSource);
        task.setTotalCount(imageResults.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setStatus("PENDING");
        task.setCategoryId(dto.getCategoryId());
        task.setNamePrefix(namePrefix);
        task.setTags(dto.getTags() != null ? JSONUtil.toJsonStr(dto.getTags()) : null);
        this.save(task);

        // 构建 MQ 消息
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", task.getId());
        message.put("userId", user.getId());
        message.put("spaceId", dto.getSpaceId());
        message.put("categoryId", dto.getCategoryId());
        message.put("namePrefix", namePrefix);
        message.put("tags", dto.getTags() != null ? JSONUtil.toJsonStr(dto.getTags()) : null);
        message.put("imageUrls", imageResults.stream().map(ImageSourceResult::getUrl).toList());
        message.put("imageNames", imageResults.stream().map(ImageSourceResult::getName).toList());
        message.put("imageIntros", imageResults.stream().map(ImageSourceResult::getIntroduction).toList());

        // 发送 MQ 消息
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BATCH_PIC_EXCHANGE,
                RabbitMQConfig.BATCH_PIC_ROUTING_KEY,
                JSONUtil.toJsonStr(message));

        log.info("批量任务已创建: taskId={}, userId={}, count={}", task.getId(), user.getId(), imageResults.size());

        return BatchTaskVO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .message("任务已提交，正在异步处理中")
                .createTime(task.getCreateTime())
                .searchText(searchText)
                .searchSource(searchSource)
                .categoryId(dto.getCategoryId())
                .namePrefix(namePrefix)
                .tags(dto.getTags())
                .spaceId(dto.getSpaceId())
                .build();
    }

    @Override
    public BatchTaskVO getTaskById(Long taskId, Long userId) {
        BatchTask task = this.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        ThrowUtils.throwIf(!task.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        return toVO(task);
    }

    @Override
    public List<BatchTaskVO> listUserTasks(Long userId) {
        List<BatchTask> tasks = this.lambdaQuery()
                .eq(BatchTask::getUserId, userId)
                .orderByDesc(BatchTask::getCreateTime)
                .last("LIMIT 50")
                .list();
        return tasks.stream().map(this::toVO).collect(Collectors.toList());
    }

    private BatchTaskVO toVO(BatchTask task) {
        return BatchTaskVO.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .createTime(task.getCreateTime())
                .finishTime(task.getFinishTime())
                .searchText(task.getSearchText())
                .searchSource(task.getSearchSource())
                .categoryId(task.getCategoryId())
                .namePrefix(task.getNamePrefix())
                .tags(task.getTags() != null ? JSONUtil.toList(task.getTags(), String.class) : null)
                .spaceId(task.getSpaceId())
                .build();
    }

    // ==================== 管理端方法 ====================

    @Override
    public BatchTaskStatsVO getAdminStats() {
        // 尝试从缓存读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.BATCH_TASK_STATS_KEY);
            if (cached != null) {
                return JSONUtil.toBean(cached, BatchTaskStatsVO.class);
            }
        } catch (Exception e) {
            log.warn("读取批量任务统计缓存失败", e);
        }

        BatchTaskStatsVO stats = new BatchTaskStatsVO();
        stats.setTotalCount(this.count(new LambdaQueryWrapper<BatchTask>().eq(BatchTask::getIsDelete, 0)));
        stats.setPendingCount(this.count(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0).eq(BatchTask::getStatus, "PENDING")));
        stats.setProcessingCount(this.count(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0).eq(BatchTask::getStatus, "PROCESSING")));
        stats.setCompletedCount(this.count(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0).eq(BatchTask::getStatus, "COMPLETED")));
        stats.setFailedCount(this.count(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0).eq(BatchTask::getStatus, "FAILED")));

        // 今日新增
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        stats.setTodayNewCount(this.count(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0).ge(BatchTask::getCreateTime, todayStart)));

        // 成功率：遍历已完成+失败的任务，统计总图片数和成功数
        List<BatchTask> finishedTasks = this.list(new LambdaQueryWrapper<BatchTask>()
                .eq(BatchTask::getIsDelete, 0)
                .in(BatchTask::getStatus, "COMPLETED", "FAILED")
                .select(BatchTask::getTotalCount, BatchTask::getSuccessCount));
        long totalImages = finishedTasks.stream().mapToLong(t -> t.getTotalCount() != null ? t.getTotalCount() : 0).sum();
        long successImages = finishedTasks.stream().mapToLong(t -> t.getSuccessCount() != null ? t.getSuccessCount() : 0).sum();
        stats.setSuccessRate(totalImages > 0 ? Math.round(successImages * 10000.0 / totalImages) / 100.0 : 0.0);

        // 写入缓存
        try {
            int ttl = RedisKeyConstants.BATCH_TASK_STATS_TTL + RandomUtil.randomInt(0, 60);
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.BATCH_TASK_STATS_KEY,
                    JSONUtil.toJsonStr(stats), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入批量任务统计缓存失败", e);
        }

        return stats;
    }

    @Override
    public Page<AdminBatchTaskVO> getAdminList(BatchTaskQueryDTO dto) {
        LambdaQueryWrapper<BatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchTask::getIsDelete, 0);

        if (StrUtil.isNotBlank(dto.getStatus())) {
            wrapper.eq(BatchTask::getStatus, dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getSearchSource())) {
            wrapper.eq(BatchTask::getSearchSource, dto.getSearchSource());
        }
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(BatchTask::getSearchText, dto.getKeyword());
        }
        if (ObjUtil.isNotEmpty(dto.getUserId())) {
            wrapper.eq(BatchTask::getUserId, dto.getUserId());
        }
        if (StrUtil.isNotBlank(dto.getStartTime())) {
            wrapper.ge(BatchTask::getCreateTime, dto.getStartTime());
        }
        if (StrUtil.isNotBlank(dto.getEndTime())) {
            wrapper.le(BatchTask::getCreateTime, dto.getEndTime());
        }

        // 排序
        wrapper.orderByDesc(BatchTask::getCreateTime);

        Page<BatchTask> page = this.page(new Page<>(dto.getCurrent(), dto.getPageSize()), wrapper);

        // 转换为 AdminBatchTaskVO 分页
        Page<AdminBatchTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toAdminVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public AdminBatchTaskVO getAdminDetail(Long taskId) {
        BatchTask task = this.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        return toAdminVO(task);
    }

    @Override
    public void adminDelete(Long taskId) {
        BatchTask task = this.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        this.removeById(taskId);
        invalidateStatsCache();
    }

    @Override
    public void adminUpdate(BatchTaskUpdateDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getId()), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        BatchTask task = this.getById(dto.getId());
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        LambdaUpdateWrapper<BatchTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BatchTask::getId, dto.getId());
        if (StrUtil.isNotBlank(dto.getStatus())) {
            wrapper.set(BatchTask::getStatus, dto.getStatus());
        }
        if (dto.getErrorMessage() != null) {
            wrapper.set(BatchTask::getErrorMessage, dto.getErrorMessage());
        }
        if (dto.getTags() != null) {
            wrapper.set(BatchTask::getTags, dto.getTags());
        }
        this.update(null, wrapper);
        invalidateStatsCache();
    }

    private AdminBatchTaskVO toAdminVO(BatchTask task) {
        AdminBatchTaskVO vo = new AdminBatchTaskVO();
        vo.setTaskId(task.getId());
        vo.setUserId(task.getUserId());
        vo.setSpaceId(task.getSpaceId());
        vo.setSearchText(task.getSearchText());
        vo.setSearchSource(task.getSearchSource());
        vo.setTotalCount(task.getTotalCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setStatus(task.getStatus());
        vo.setCategoryId(task.getCategoryId());
        vo.setNamePrefix(task.getNamePrefix());
        vo.setTags(task.getTags());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        vo.setFinishTime(task.getFinishTime());
        return vo;
    }

    private void invalidateStatsCache() {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.BATCH_TASK_STATS_KEY);
        } catch (Exception e) {
            log.warn("清除批量任务统计缓存失败", e);
        }
    }

    @Override
    public boolean hasRunningTask(Long userId) {
        return this.lambdaQuery()
                .eq(BatchTask::getUserId, userId)
                .eq(BatchTask::getIsDelete, 0)
                .in(BatchTask::getStatus, "PENDING", "PROCESSING")
                .exists();
    }
}
