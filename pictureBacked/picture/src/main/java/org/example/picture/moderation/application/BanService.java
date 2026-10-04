package org.example.picture.moderation.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.moderation.interfaces.dto.BanUnbanDTO;
import org.example.picture.moderation.domain.model.BanRecord;
import org.example.identity.api.model.User;
import org.example.picture.moderation.interfaces.vo.BanRecordVO;

import java.util.Map;

/**
 * 封禁服务接口
 *
 * @author Zou
 */
public interface BanService extends IService<BanRecord> {

    /**
     * 执行封禁
     *
     * @param userId      被封禁用户 ID
     * @param banType     封禁类型
     * @param banDuration 封禁时长（天）
     * @param banReason   封禁原因
     * @param reportId    关联举报记录 ID
     */
    void executeBan(Long userId, String banType, Integer banDuration, String banReason, Long reportId);

    /**
     * 手动解封
     */
    void unbanUser(BanUnbanDTO dto);

    /**
     * 自动解封（定时任务调用）
     */
    void autoUnbanExpiredUsers();

    /**
     * 检查封禁状态（LoginInterceptor 调用）
     */
    void checkBanStatus(User user);

    /**
     * 封禁记录列表
     */
    Page<BanRecordVO> getBanRecordList(int current, int pageSize, String banStatus);

    /**
     * 封禁统计
     */
    Map<String, Object> getBanStats();
}
