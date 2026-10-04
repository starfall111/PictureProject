package org.example.marketing.interfaces.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.constants.RedisKeyConstants;
import org.example.marketing.domain.enums.VipTypeEnum;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.identity.api.VipUtil;
import org.example.marketing.interfaces.dto.VipGrantDTO;
import org.example.identity.api.model.User;
import org.example.marketing.interfaces.vo.VipStatsVO;
import org.example.identity.application.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * 管理后台 — 会员管理
 */
// todo 撤销会员状态没有发送通知给用户
@Slf4j
@RestController
@RequestMapping("/admin/vip")
public class AdminVipController {

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 会员列表
     * GET /api/admin/vip/list
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<User>> listVipUsers(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer vipType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (vipType != null) {
            wrapper.eq(User::getVipType, vipType);
        } else {
            wrapper.gt(User::getVipType, 0); // 默认只看会员
        }
        wrapper.orderByDesc(User::getVipExpireTime);
        Page<User> page = userService.page(new Page<>(current, pageSize), wrapper);
        return ResultUtils.success(page);
    }

    /**
     * 会员统计
     * GET /api/admin/vip/stats
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<VipStatsVO> getVipStats() {
        VipStatsVO vo = new VipStatsVO();

        // 总 VIP 用户数（含已过期）
        vo.setTotalVipUsers((int) userService.count(
                new LambdaQueryWrapper<User>().gt(User::getVipType, 0)
        ));

        // 当前有效 VIP
        vo.setActiveVipUsers((int) userService.count(
                new LambdaQueryWrapper<User>()
                        .eq(User::getVipType, VipTypeEnum.VIP.getValue())
                        .gt(User::getVipExpireTime, new Date())
        ));

        // 即将到期（3天内）
        Date threeDaysLater = new Date(System.currentTimeMillis() + 3L * 24 * 3600 * 1000);
        vo.setExpiringVipUsers((int) userService.count(
                new LambdaQueryWrapper<User>()
                        .eq(User::getVipType, VipTypeEnum.VIP.getValue())
                        .between(User::getVipExpireTime, new Date(), threeDaysLater)
        ));

        // 今日新增（简化：今日激活过的）
        // TODO: 如需精确统计需要日志表或增加字段

        return ResultUtils.success(vo);
    }

    /**
     * 手动赠送 VIP
     * POST /api/admin/vip/{userId}/grant
     */
    @PostMapping("/{userId}/grant")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<?> grantVip(@PathVariable Long userId,
                                           @RequestBody VipGrantDTO dto) {
        User user = userService.getById(userId);
        if (user == null) {
            return ResultUtils.error(40000, "用户不存在");
        }

        Date newExpireTime = VipUtil.calculateNewExpireTime(user.getVipExpireTime(), dto.getDays());

        user.setVipType(VipTypeEnum.VIP.getValue());
        user.setVipExpireTime(newExpireTime);
        if (user.getVipActivatedAt() == null) {
            user.setVipActivatedAt(new Date());
        }
        user.setVipTotalDays(
                (user.getVipTotalDays() != null ? user.getVipTotalDays() : 0) + dto.getDays()
        );
        userService.updateById(user);

        // 失效缓存
        String userInfoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
        stringRedisTemplate.delete(userInfoKey);

        log.info("管理员赠送VIP, userId={}, days={}, reason={}", userId, dto.getDays(), dto.getReason());
        return ResultUtils.success(true);
    }

    /**
     * 撤销 VIP（立即过期）
     * POST /api/admin/vip/{userId}/revoke
     */
    @PostMapping("/{userId}/revoke")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<?> revokeVip(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return ResultUtils.error(40000, "用户不存在");
        }

        user.setVipType(VipTypeEnum.NORMAL.getValue());
        user.setVipExpireTime(new Date()); // 立即过期
        userService.updateById(user);

        // 失效缓存
        String userInfoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
        stringRedisTemplate.delete(userInfoKey);

        log.info("管理员撤销VIP, userId={}", userId);
        return ResultUtils.success(true);
    }

    /**
     * 设置降级等级
     * POST /api/admin/vip/degrade?level=0
     */
    @PostMapping("/degrade")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> setDegradeLevel(@RequestParam int level) {
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.SECKILL_DEGRADE, String.valueOf(level));
        log.info("降级等级设置为: {}", level);
        return ResultUtils.success(true);
    }
}
