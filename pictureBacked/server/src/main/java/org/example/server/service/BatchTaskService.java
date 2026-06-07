package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.dto.BatchTaskQueryDTO;
import org.example.pojo.dto.BatchTaskUpdateDTO;
import org.example.pojo.dto.picture.PictureUploadByBatchDTO;
import org.example.pojo.entity.BatchTask;
import org.example.pojo.entity.User;
import org.example.pojo.vo.AdminBatchTaskVO;
import org.example.pojo.vo.BatchTaskStatsVO;
import org.example.pojo.vo.BatchTaskVO;

import java.util.List;

/**
 * 批量获取图片任务服务
 *
 * @author Zou
 */
public interface BatchTaskService extends IService<BatchTask> {

    /**
     * 创建并提交异步批量任务
     *
     * @param user 当前用户
     * @param dto  批量上传参数
     * @return 任务信息 VO
     */
    BatchTaskVO createAndSubmitTask(User user, PictureUploadByBatchDTO dto);

    /**
     * 查询单个任务状态
     */
    BatchTaskVO getTaskById(Long taskId, Long userId);

    /**
     * 查询用户的批量任务列表
     */
    List<BatchTaskVO> listUserTasks(Long userId);

    /**
     * 管理端统计
     */
    BatchTaskStatsVO getAdminStats();

    /**
     * 管理端分页查询
     */
    Page<AdminBatchTaskVO> getAdminList(BatchTaskQueryDTO dto);

    /**
     * 管理端详情（无需归属校验）
     */
    AdminBatchTaskVO getAdminDetail(Long taskId);

    /**
     * 管理端逻辑删除
     */
    void adminDelete(Long taskId);

    /**
     * 管理端修改
     */
    void adminUpdate(BatchTaskUpdateDTO dto);

    /**
     * 判断用户是否有正在运行的任务（PENDING / PROCESSING）
     *
     * @param userId 用户ID
     * @return true 表示存在进行中的任务
     */
    boolean hasRunningTask(Long userId);
}
