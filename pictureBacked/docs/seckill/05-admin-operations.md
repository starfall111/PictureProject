# Phase 5 — 管理后台与运营

> 优先级：P1 | 依赖：Phase 4 | 预估工时：3d

## 目标

实现管理后台的批次管理、编码券管理、会员管理、数据看板等功能，为运营人员提供完整的秒杀活动管理能力。

---

## 1. DTO 层

### 1.1 BatchCreateDTO

创建 `pojo/src/main/java/org/example/pojo/dto/seckill/BatchCreateDTO.java`：

```java
package org.example.pojo.dto.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 创建发放批次 DTO
 */
@Data
public class BatchCreateDTO {

    /**
     * 批次名称
     */
    private String name;

    /**
     * 券类型: 3/7/30 天
     */
    private Integer type;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 秒杀开始时间
     */
    private Date startTime;

    /**
     * 秒杀结束时间（可选，NULL表示发完即止）
     */
    private Date endTime;
}
```

### 1.2 BatchQueryDTO

创建 `pojo/src/main/java/org/example/pojo/dto/seckill/BatchQueryDTO.java`：

```java
package org.example.pojo.dto.seckill;

import lombok.Data;

/**
 * 批次查询 DTO
 */
@Data
public class BatchQueryDTO {

    /**
     * 状态筛选: 0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消
     */
    private Integer status;

    private int current = 1;

    private int pageSize = 10;
}
```

### 1.3 VipGrantDTO

创建 `pojo/src/main/java/org/example/pojo/dto/seckill/VipGrantDTO.java`：

```java
package org.example.pojo.dto.seckill;

import lombok.Data;

/**
 * 管理员赠送 VIP DTO
 */
@Data
public class VipGrantDTO {

    private Long userId;

    /**
     * 赠送天数
     */
    private Integer days;

    private String reason;
}
```

---

## 2. VO 层

### 2.1 BatchVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/BatchVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

import java.util.Date;

/**
 * 批次管理 VO
 */
@Data
public class BatchVO {

    private Long id;

    private String batchNo;

    private String name;

    private Integer type;

    private String typeName;

    private Integer totalStock;

    private Integer currentStock;

    private Date startTime;

    private Date endTime;

    private Integer status;

    private String statusName;

    private Date createTime;
}
```

### 2.2 SeckillStatsVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/SeckillStatsVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

/**
 * 秒杀数据统计 VO
 */
@Data
public class SeckillStatsVO {

    /**
     * 总发放批次数
     */
    private Integer totalBatches;

    /**
     * 总编码券数
     */
    private Integer totalCoupons;

    /**
     * 已领取数
     */
    private Integer claimedCount;

    /**
     * 已激活数
     */
    private Integer activatedCount;

    /**
     * 已过期数
     */
    private Integer expiredCount;

    /**
     * 领取率
     */
    private Double claimRate;

    /**
     * 激活率
     */
    private Double activationRate;
}
```

### 2.3 VipStatsVO

创建 `pojo/src/main/java/org/example/pojo/vo/seckill/VipStatsVO.java`：

```java
package org.example.pojo.vo.seckill;

import lombok.Data;

/**
 * 会员统计 VO
 */
@Data
public class VipStatsVO {

    /**
     * VIP 总人数（含已过期）
     */
    private Integer totalVipUsers;

    /**
     * 当前有效 VIP 人数
     */
    private Integer activeVipUsers;

    /**
     * 今日新增 VIP
     */
    private Integer todayNewVip;

    /**
     * 即将到期（3天内）VIP 人数
     */
    private Integer expiringVipUsers;
}
```

---

## 3. AdminBatchController

创建 `server/src/main/java/org/example/server/controller/admin/AdminBatchController.java`：

```java
package org.example.server.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.enums.CouponStatusEnum;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.BatchCreateDTO;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.service.SeckillService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理后台 — 批次管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/batch")
@CheckAuth(mustRole = "admin")
public class AdminBatchController {

    @Resource
    private org.example.server.mapper.CodeCouponBatchMapper batchMapper;

    @Resource
    private CodeCouponMapper couponMapper;

    @Resource
    private SeckillService seckillService;

    /**
     * 创建发放批次
     * POST /api/admin/batch/create
     */
    @PostMapping("/create")
    public BaseResponse<Long> createBatch(@RequestBody BatchCreateDTO dto) {
        CodeCouponBatch batch = new CodeCouponBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setBatchNo(generateBatchNo());
        batch.setCurrentStock(dto.getTotalStock());
        batch.setStatus(0); // 草稿

        batchMapper.insert(batch);
        return ResultUtils.success(batch.getId());
    }

    /**
     * 批次列表
     * GET /api/admin/batch/list
     */
    @GetMapping("/list")
    public BaseResponse<Page<CodeCouponBatch>> listBatches(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CodeCouponBatch::getStatus, status);
        }
        wrapper.orderByDesc(CodeCouponBatch::getCreateTime);
        Page<CodeCouponBatch> page = batchMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return ResultUtils.success(page);
    }

    /**
     * 手动触发预热
     * POST /api/admin/batch/{id}/preheat
     */
    @PostMapping("/{id}/preheat")
    public BaseResponse<Boolean> preheat(@PathVariable Long id) {
        seckillService.preheat(id);
        return ResultUtils.success(true);
    }

    /**
     * 取消批次
     * POST /api/admin/batch/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public BaseResponse<Boolean> cancel(@PathVariable Long id) {
        CodeCouponBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            return ResultUtils.error(40000, "批次不存在");
        }
        batch.setStatus(4); // 已取消
        batchMapper.updateById(batch);
        return ResultUtils.success(true);
    }

    private String generateBatchNo() {
        return "BATCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", new java.util.Random().nextInt(1000));
    }
}
```

---

## 4. AdminCouponController

创建 `server/src/main/java/org/example/server/controller/admin/AdminCouponController.java`：

```java
package org.example.server.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.enums.CouponStatusEnum;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.entity.CodeCoupon;
import org.example.server.mapper.CodeCouponMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 管理后台 — 编码券管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/coupon")
@CheckAuth(mustRole = "admin")
public class AdminCouponController {

    @Resource
    private CodeCouponMapper couponMapper;

    /**
     * 编码券列表（支持按状态、用户、批次筛选）
     * GET /api/admin/coupon/list
     */
    @GetMapping("/list")
    public BaseResponse<Page<CodeCoupon>> listCoupons(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long batchId) {
        LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CodeCoupon::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(CodeCoupon::getUserId, userId);
        }
        if (batchId != null) {
            wrapper.eq(CodeCoupon::getBatchId, batchId);
        }
        wrapper.orderByDesc(CodeCoupon::getCreateTime);
        Page<CodeCoupon> page = couponMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return ResultUtils.success(page);
    }

    /**
     * 吊销编码券
     * POST /api/admin/coupon/{id}/revoke
     */
    @PostMapping("/{id}/revoke")
    public BaseResponse<Boolean> revoke(@PathVariable Long id) {
        CodeCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            return ResultUtils.error(40000, "编码券不存在");
        }
        // 仅已领取未使用的券可吊销
        if (coupon.getStatus() != CouponStatusEnum.CLAIMED.getValue()) {
            return ResultUtils.error(40000, "券状态不支持吊销");
        }
        coupon.setStatus(CouponStatusEnum.EXPIRED.getValue());
        couponMapper.updateById(coupon);
        return ResultUtils.success(true);
    }
}
```

---

## 5. AdminVipController

创建 `server/src/main/java/org/example/server/controller/admin/AdminVipController.java`：

```java
package org.example.server.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.VipTypeEnum;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.common.util.VipUtil;
import org.example.pojo.dto.seckill.VipGrantDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.seckill.VipStatsVO;
import org.example.server.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * 管理后台 — 会员管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/vip")
@CheckAuth(mustRole = "admin")
public class AdminVipController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 会员列表
     * GET /api/admin/vip/list
     */
    @GetMapping("/list")
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
    public BaseResponse<Boolean> grantVip(@PathVariable Long userId,
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
    public BaseResponse<Boolean> revokeVip(@PathVariable Long userId) {
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
     * POST /api/admin/seckill/degrade?level=0
     */
    @PostMapping("/degrade")
    public BaseResponse<Boolean> setDegradeLevel(@RequestParam int level) {
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.SECKILL_DEGRADE, String.valueOf(level));
        log.info("降级等级设置为: {}", level);
        return ResultUtils.success(true);
    }
}
```

---

## 6. 秒杀数据看板

在 `AdminBatchController` 中追加统计接口：

```java
/**
 * 秒杀数据统计看板
 * GET /api/admin/seckill/stats
 */
@GetMapping("/stats")
public BaseResponse<SeckillStatsVO> getSeckillStats() {
    SeckillStatsVO vo = new SeckillStatsVO();

    // 总批次数
    vo.setTotalBatches((int) batchMapper.selectCount(null));

    // 总券数、各状态券数
    vo.setTotalCoupons((int) couponMapper.selectCount(null));
    vo.setClaimedCount((int) couponMapper.selectCount(
            new LambdaQueryWrapper<CodeCoupon>()
                    .gt(CodeCoupon::getStatus, CouponStatusEnum.UNSOLD.getValue())
    ));
    vo.setActivatedCount((int) couponMapper.selectCount(
            new LambdaQueryWrapper<CodeCoupon>()
                    .eq(CodeCoupon::getStatus, CouponStatusEnum.ACTIVATED.getValue())
    ));
    vo.setExpiredCount((int) couponMapper.selectCount(
            new LambdaQueryWrapper<CodeCoupon>()
                    .eq(CodeCoupon::getStatus, CouponStatusEnum.EXPIRED.getValue())
    ));

    // 领取率 = 已领取(含使用/过期) / 总券数
    if (vo.getTotalCoupons() > 0) {
        vo.setClaimRate((double) vo.getClaimedCount() / vo.getTotalCoupons());
    }
    // 激活率 = 已激活 / 已领取
    if (vo.getClaimedCount() > 0) {
        vo.setActivationRate((double) vo.getActivatedCount() / vo.getClaimedCount());
    }

    return ResultUtils.success(vo);
}
```

---

## 验证清单

- [ ] `POST /api/admin/batch/create` 成功创建批次，生成批次号
- [ ] `GET /api/admin/batch/list` 返回批次列表，支持状态筛选
- [ ] `POST /api/admin/batch/{id}/preheat` 成功预热批次，Redis 写入库存
- [ ] `POST /api/admin/batch/{id}/cancel` 取消批次
- [ ] `GET /api/admin/coupon/list` 返回券列表，支持多维度筛选
- [ ] `POST /api/admin/coupon/{id}/revoke` 吊销已领取的券
- [ ] `GET /api/admin/vip/list` 返回会员列表
- [ ] `GET /api/admin/vip/stats` 返回统计数据（总人数、活跃数等）
- [ ] `POST /api/admin/vip/{userId}/grant` 赠送 VIP，到期时间正确计算
- [ ] `POST /api/admin/vip/{userId}/revoke` 撤销 VIP，立即过期
- [ ] `POST /api/admin/seckill/degrade?level=3` 设置降级后抢购被暂停
- [ ] `GET /api/admin/seckill/stats` 返回领取率、激活率等统计数据
- [ ] 所有管理接口仅管理员可访问
