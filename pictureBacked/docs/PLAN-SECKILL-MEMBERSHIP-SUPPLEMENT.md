# 秒杀编码券系统 -- 会员体系补充方案

> 本文档是 PLAN-SECKILL.md 的补充，覆盖会员字段设计、编码券激活流程、权益拦截机制、过期/续费生命周期，以及原方案遗漏的关键设计点。

---

## 一、现状分析

### 1.1 当前 User 表结构

当前 `user` 表没有会员相关字段。用户角色仅有 `userRole`（user/admin）两个字段，空间级别由 `space` 表的 `spaceLevel` 字段独立控制（0-普通版/1-专业版/2-旗舰版），与用户会员状态无关联。

### 1.2 当前权限体系

- `CheckAuth` 注解 + `CheckAuthAop` 切面：仅支持 `mustRole` 单一维度的角色校验（user/admin）。
- `LoginInterceptor`：登录校验 + 封禁状态检查，无会员状态检查。
- `SpaceServiceImpl.addSpace()`：硬编码判断非 COMMON 级别空间仅管理员可创建。
- 编码券表 `code_coupon`（PLAN-SECKILL.md 中设计）状态流转：0-未发放 -> 1-已领取未使用 -> 2-已激活使用中 -> 3-已过期。

### 1.3 缺失环节

PLAN-SECKILL.md 设计了完整的秒杀抢购 -> 领取编码券链路，但以下关键链路未覆盖：

1. 编码券激活后如何影响用户会员状态
2. 用户表需要哪些字段记录会员信息
3. 会员权益如何在业务层拦截（空间升级、批量获取图片等）
4. 会员到期后的降级处理
5. 多张券叠加激活的时间累加逻辑

---

## 二、用户表会员字段 DDL

### 2.1 ALTER TABLE 语句

```sql
-- User 表扩展 — 会员字段
ALTER TABLE `user`
  ADD COLUMN `vipType`          TINYINT    NOT NULL DEFAULT 0 COMMENT '会员类型: 0-普通用户, 1-VIP会员' AFTER `userRole`,
  ADD COLUMN `vipExpireTime`    DATETIME   DEFAULT NULL COMMENT 'VIP会员到期时间（NULL表示非会员）' AFTER `vipType`,
  ADD COLUMN `vipActivatedAt`   DATETIME   DEFAULT NULL COMMENT '最近一次激活VIP的时间' AFTER `vipExpireTime`,
  ADD COLUMN `vipTotalDays`     INT        NOT NULL DEFAULT 0 COMMENT 'VIP累计总天数（含历史）' AFTER `vipActivatedAt`,
  ADD INDEX `idx_vip_status` (`vipType`, `vipExpireTime`);
```

### 2.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `vipType` | TINYINT | 会员类型枚举。0=普通用户, 1=VIP会员。预留未来扩展（2=SVIP等） |
| `vipExpireTime` | DATETIME | VIP到期时间戳。NULL 表示从未开通或已过期。查询时 `vipType=1 AND vipExpireTime > NOW()` 即为有效会员 |
| `vipActivatedAt` | DATETIME | 最近一次激活时间，用于展示"已连续VIP X天"等用户画像 |
| `vipTotalDays` | INT | 历史累计VIP天数，用于运营数据统计和忠诚度分析 |

### 2.3 设计决策说明

**为什么在 user 表加字段而不是新建 user_vip 表？**

- 会员状态是高频读取字段（每次请求都可能需要判断），放在 user 表避免 JOIN 查询。
- 当前会员体系简单（仅 VIP 一级），不需要独立的会员等级/积分/成长值体系。
- 与现有的 `banStatus` 字段模式一致——user 表已承载封禁状态，会员状态同理。
- `CachedUserServiceImpl` 已有 user 维度的 Redis 缓存机制（`USER_INFO_KEY`），新增字段自动进入缓存层，无需额外缓存设计。

**如果未来会员体系变复杂（多等级/积分/成长值），再考虑抽取独立表。**

---

## 三、编码券激活为会员的完整流程

### 3.1 流程图

```
用户点击"使用"按钮
        |
        v
  前端 POST /api/coupon/activate {couponId}
        |
        v
  Controller 层
    - 登录校验（LoginInterceptor）
    - 参数校验（couponId 非空）
        |
        v
  Service 层 — activateCoupon(userId, couponId)
    |
    +-- 1. 查询编码券，校验归属和状态
    |     - coupon.userId == 当前用户
    |     - coupon.status == 1（已领取未使用）
    |     - coupon.expireAt > NOW()（券未过期）
    |
    +-- 2. 计算新的会员到期时间
    |     - 用户当前非会员: NOW() + coupon.type 天
    |     - 用户当前是会员且未过期: vipExpireTime + coupon.type 天
    |     - 用户当前是会员但已过期: NOW() + coupon.type 天
    |
    +-- 3. 事务内执行（TransactionTemplate）
    |     a. 更新 user 表会员字段
    |        - vipType = 1
    |        - vipExpireTime = 计算后的到期时间
    |        - vipActivatedAt = NOW()（首次激活时）
    |        - vipTotalDays += coupon.type
    |     b. 更新 code_coupon 表
    |        - status = 2（已激活使用中）
    |        - activatedAt = NOW()
    |
    +-- 4. 失效用户缓存
    |     - invalidateUserCache(userId)
    |
    +-- 5. 发送通知
    |     - 发布 NotificationEvent (VIP_ACTIVATED)
    |
    +-- 6. 异步：升级用户空间（见 3.2）
    |
        v
  返回激活结果（含新的到期时间）
```

### 3.2 编码券激活时的空间自动升级

VIP 会员应自动获得专业版空间权限。激活编码券时，需要同步处理用户的空间：

```java
// 在 activateCoupon 事务外异步处理（空间升级不要求强一致）
private void upgradeSpaceForVip(Long userId) {
    Space space = spaceService.getOne(
        new LambdaQueryWrapper<Space>()
            .eq(Space::getUserId, userId)
    );

    if (space == null) {
        // 用户没有空间，自动创建专业版空间
        SpaceAddDTO dto = new SpaceAddDTO();
        dto.setSpaceName("默认空间");
        dto.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());
        // 注意：这里需要绕过权限校验，由系统内部创建
        spaceService.addSpaceInternal(dto, userId);
    } else if (space.getSpaceLevel() < SpaceLevelEnum.PROFESSIONAL.getValue()) {
        // 升级到专业版
        space.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());
        space.setMaxCount(SpaceLevelEnum.PROFESSIONAL.getMaxCount());
        space.setMaxSize(SpaceLevelEnum.PROFESSIONAL.getMaxSize());
        spaceService.updateById(space);
    }
    // 如果已经是专业版或旗舰版，不降级
}
```

### 3.3 会员状态查询接口

```
GET /api/user/vip/status
```

**响应：**
```json
{
    "code": 0,
    "data": {
        "isVip": true,
        "vipType": 1,
        "vipTypeName": "VIP会员",
        "expireTime": "2026-07-06T14:30:00",
        "remainingDays": 28,
        "activatedAt": "2026-06-06T14:30:00",
        "totalDays": 37
    }
}
```

此接口应被 `CachedUserServiceImpl` 缓存（复用 `USER_INFO_KEY`），因为 `LoginUserVO` 已经缓存了用户基本信息，会员字段加入后自动缓存。

---

## 四、会员权益拦截机制

### 4.1 VIP 权益矩阵

| 功能 | 普通用户 | VIP会员 |
|------|---------|---------|
| 图片上传（单张） | 支持 | 支持 |
| 空间容量 | 100张 / 100MB | 1000张 / 1GB |
| 批量获取图片（Pexels/Bing） | 不支持 | 支持（每次最多30张） |
| 空间级别 | 普通版(0) | 专业版(1) |
| 图片下载 | 支持 | 支持 |

### 4.2 CheckAuth 注解扩展

当前 `CheckAuth` 仅支持 `mustRole`（admin/user）。需要增加 VIP 权限维度：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuth {

    String mustRole() default "";

    /**
     * 是否需要 VIP 会员权限
     */
    boolean requireVip() default false;
}
```

### 4.3 CheckAuthAop 切面扩展

```java
@Around("@annotation(checkAuth)")
public Object checkRoleInterceptor(ProceedingJoinPoint checkPoint, CheckAuth checkAuth) throws Throwable {
    // ... 现有的角色校验逻辑保持不变 ...

    // VIP 会员权限校验
    if (checkAuth.requireVip()) {
        // 此时 user 已经从 DB 获取（上面现有逻辑已有 user = userService.getById）
        if (user.getVipType() != 1 || user.getVipExpireTime() == null
                || user.getVipExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该功能需要VIP会员");
        }
    }

    return checkPoint.proceed();
}
```

**注意**：VIP 校验放在 `CheckAuthAop` 而非 `LoginInterceptor` 的原因：
- `LoginInterceptor` 是全局拦截器，每次请求都执行，VIP 校验仅部分接口需要。
- `CheckAuthAop` 是注解驱动的，仅标注了 `@CheckAuth(requireVip=true)` 的接口才触发校验。
- 性能更优，避免全局拦截器增加每个请求的额外判断。

### 4.4 接口级 VIP 拦截示例

```java
// 批量获取图片 — VIP 专属
@PostMapping("/batch-fetch")
@CheckAuth(requireVip = true)
public BaseResponse<BatchTaskVO> batchFetchPictures(@RequestBody PictureUploadByBatchDTO dto) {
    // ...
}

// 空间创建 — 根据会员状态限制级别
@PostMapping("/add")
public BaseResponse<Long> addSpace(@RequestBody SpaceAddDTO spaceAddDTO) {
    // addSpace 内部的权限判断需要修改为检查 VIP 状态
    // 而非硬编码检查 admin 角色
}
```

### 4.5 SpaceServiceImpl.addSpace() 权限逻辑修改

当前逻辑：
```java
if (!SpaceLevelEnum.COMMON.equals(spaceLevel) && !UserEnum.ADMIN.getValue().equals(user.getUserRole())) {
    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
}
```

修改为：
```java
boolean isAdmin = UserEnum.ADMIN.getValue().equals(user.getUserRole());
boolean isVip = user.getVipType() == 1 && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new Date());

if (!SpaceLevelEnum.COMMON.equals(spaceLevel)) {
    // 非普通版空间：管理员 或 有效VIP会员可创建
    if (!isAdmin && !isVip) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "升级为VIP会员以解锁专业版空间");
    }
}
// 管理员可创建旗舰版空间（保持现有逻辑）
if (SpaceLevelEnum.FLAGSHIP.equals(spaceLevel) && !isAdmin) {
    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "旗舰版空间仅管理员可创建");
}
```

---

## 五、会员过期/续费处理

### 5.1 会员过期定时任务

```java
/**
 * VIP 会员过期处理 — 每小时执行
 * 扫描已过期会员，降级用户状态和空间
 */
@Scheduled(cron = "0 0 * * * ?")
public void expireVipMembers() {
    String lockKey = "lock:vip:expire";
    if (!tryLock(lockKey, 3000)) {
        return;
    }
    try {
        Date now = new Date();

        // 1. 查询所有已过期的 VIP 用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getVipType, 1)
               .isNotNull(User::getVipExpireTime)
               .lt(User::getVipExpireTime, now);

        List<User> expiredUsers = userService.list(wrapper);

        for (User user : expiredUsers) {
            try {
                handleVipExpiration(user);
            } catch (Exception e) {
                log.error("处理VIP过期失败, userId={}", user.getId(), e);
            }
        }

        log.info("VIP过期处理完成, 处理 {} 位用户", expiredUsers.size());
    } finally {
        unlock(lockKey);
    }
}

private void handleVipExpiration(User user) {
    Date now = new Date();

    // 1. 降级用户 VIP 状态
    user.setVipType(0);
    // vipExpireTime 保留不动（记录历史），也可以置 NULL
    boolean updated = userService.updateById(user);

    if (updated) {
        // 2. 失效缓存
        invalidateUserCache(user.getId());

        // 3. 降级空间（如果当前是专业版）
        downgradeSpaceForExpiredVip(user.getId());

        // 4. 发送到期通知
        NotificationEvent event = new NotificationEvent(
            this, user.getId(), 0L, "系统", null,
            NotificationTypeEnum.VIP_EXPIRED, "VIP会员已到期",
            "您的VIP会员已到期，部分功能将受到限制。获取新的编码券可继续享受VIP权益。",
            null, null
        );
        eventPublisher.publishEvent(event);
    }
}

/**
 * 降级空间到普通版
 * 仅降级专业版空间，旗舰版不动（管理员空间）
 */
private void downgradeSpaceForExpiredVip(Long userId) {
    Space space = spaceService.getOne(
        new LambdaQueryWrapper<Space>()
            .eq(Space::getUserId, userId)
            .eq(Space::getSpaceLevel, SpaceLevelEnum.PROFESSIONAL.getValue())
    );
    if (space != null) {
        // 仅在当前用量不超过普通版限额时才降级
        if (space.getTotalCount() <= SpaceLevelEnum.COMMON.getMaxCount()
                && space.getTotalSize() <= SpaceLevelEnum.COMMON.getMaxSize()) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            space.setMaxCount(SpaceLevelEnum.COMMON.getMaxCount());
            space.setMaxSize(SpaceLevelEnum.COMMON.getMaxSize());
            spaceService.updateById(space);
        }
        // 如果超出普通版限额，保持专业版但标记需要关注
        // 后续用户无法继续上传（已超额），但不会丢失已有图片
    }
}
```

### 5.2 多张券叠加激活

用户可能持有多张编码券（不同类型），激活时需要正确累加时间：

```java
/**
 * 计算激活后的到期时间
 * @param currentExpireTime 当前到期时间（可能为null）
 * @param couponType 券类型（3/7/30天）
 * @return 新的到期时间
 */
private Date calculateNewExpireTime(Date currentExpireTime, int couponType) {
    Date now = new Date();
    Date baseTime;

    if (currentExpireTime != null && currentExpireTime.after(now)) {
        // 当前会员未过期，在现有到期时间上累加
        baseTime = currentExpireTime;
    } else {
        // 当前非会员或已过期，从现在开始计算
        baseTime = now;
    }

    // 加上天数
    return new Date(baseTime.getTime() + (long) couponType * 24 * 3600 * 1000);
}
```

### 5.3 会员状态实时校验

会员状态的判断不能仅依赖 `vipType` 字段，必须同时检查 `vipExpireTime`：

```java
/**
 * 判断用户是否为有效VIP会员
 * 需要同时满足: vipType=1 AND vipExpireTime > NOW()
 */
public static boolean isActiveVip(User user) {
    return user != null
        && user.getVipType() == 1
        && user.getVipExpireTime() != null
        && user.getVipExpireTime().after(new Date());
}
```

这个工具方法应放在 `common` 模块，供 `CheckAuthAop`、`SpaceServiceImpl`、`PictureController` 等多处使用。

---

## 六、User 实体与 VO 层修改

### 6.1 User 实体新增字段

在 `User.java`（`pojo/src/main/java/org/example/pojo/entity/User.java`）中新增：

```java
/**
 * VIP会员类型: 0-普通用户, 1-VIP会员
 */
private Integer vipType;

/**
 * VIP到期时间
 */
@TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
private Date vipExpireTime;

/**
 * 最近一次激活VIP时间
 */
@TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
private Date vipActivatedAt;

/**
 * VIP累计总天数
 */
private Integer vipTotalDays;
```

### 6.2 LoginUserVO 新增字段

在 `LoginUserVO.java` 中新增，确保前端能从登录信息中直接获取会员状态：

```java
/**
 * VIP会员类型: 0-普通用户, 1-VIP会员
 */
private Integer vipType;

/**
 * VIP到期时间
 */
private Date vipExpireTime;
```

### 6.3 UserProfileVO 新增字段

在 `UserProfileVO.java` 中新增，用于用户档案页展示：

```java
/**
 * VIP会员类型
 */
private Integer vipType;

/**
 * VIP到期时间
 */
private Date vipExpireTime;

/**
 * 是否为有效VIP会员（计算字段，由Service层填充）
 */
private Boolean isActiveVip;
```

### 6.4 UserMapper.xml 更新

`server/src/main/resources/mapper/UserMapper.xml` 的 `Base_Column_List` 需要追加：

```xml
<sql id="Base_Column_List">
    id,userAccount,userPhone,userEmail,userPassword,userName,
    userAvatar,userProfile,userRole,banStatus,banEndTime,
    violationCount,lastViolationTime,
    vipType,vipExpireTime,vipActivatedAt,vipTotalDays,
    editTime,createTime,updateTime,isDelete
</sql>
```

---

## 七、Redis Key 新增

在 `RedisKeyConstants.java` 中新增会员相关 Key：

```java
// ==================== VIP 会员模块 Key ====================

/**
 * VIP到期提醒已发送标记
 * Value: STRING "1", TTL 到当天 23:59:59
 * 格式: vip:expire:remind:{userId}
 */
public static final String VIP_EXPIRE_REMIND_KEY = "vip:expire:remind:%d";

/**
 * 编码券激活防重锁
 * Value: STRING, TTL 10s
 * 格式: lock:coupon:activate:{userId}:{couponId}
 */
public static final String COUPON_ACTIVATE_LOCK_KEY = "lock:coupon:activate:%d:%d";

/**
 * 编码券激活限流
 * Value: STRING(INT), TTL 60s
 * 格式: rate_limit:coupon:activate:{userId}
 */
public static final String COUPON_ACTIVATE_RATE_LIMIT_KEY = "rate_limit:coupon:activate:%d";
/** 编码券激活限流窗口（秒）= 60 秒 */
public static final int COUPON_ACTIVATE_RATE_LIMIT_WINDOW = 60;
/** 编码券激活限流最大次数 */
public static final int COUPON_ACTIVATE_RATE_LIMIT_MAX = 5;
```

---

## 八、原方案遗漏的关键设计点

### 8.1 编码券激活接口的安全防护

PLAN-SECKILL.md 的 `POST /api/coupon/activate` 接口缺乏详细的安全设计。

**需要补充：**

1. **归属校验**：`coupon.userId` 必须等于当前登录用户 ID，防止 A 用户激活 B 用户的券。
2. **状态校验**：仅 `status=1`（已领取未使用）的券可激活，已激活/已过期的券拒绝。
3. **过期校验**：券本身有 `expireAt` 字段，过期券不可激活。
4. **防重提交**：使用 Redis 分布式锁（`COUPON_ACTIVATE_LOCK_KEY`），同一用户同一券 10 秒内不可重复请求。
5. **限流**：每用户每分钟最多激活 5 张券（`COUPON_ACTIVATE_RATE_LIMIT_KEY`）。
6. **事务一致性**：user 表更新 + coupon 表更新必须在同一事务内。

### 8.2 编码券过期与会员过期的关系澄清

PLAN-SECKILL.md 中编码券有两个"过期"概念，需要明确区分：

| 概念 | 字段 | 含义 | 影响 |
|------|------|------|------|
| 券领取过期 | `code_coupon.expireAt` | 用户领取券后，N天内必须激活，否则券作废 | 券不可再激活，库存归还 |
| 会员服务过期 | `user.vipExpireTime` | VIP 权益到期 | 降级为普通用户 |

**流程：**
```
抢到券 -> 券领取过期倒计时开始（如7天券，领取后7天内需激活）
  -> 用户在领取过期前激活 -> 会员开始（如30天）
  -> 用户未在领取过期前激活 -> 券状态变为"已过期"(3)，库存归还
```

### 8.3 编码券过期后库存归还的并发安全

PLAN-SECKILL.md 5.5 节的过期券回收定时任务直接 `redis.hincr(stock, 1)` 存在并发风险：

**问题：** 如果定时任务归还库存的同时有用户在抢购（Lua 扣库存），可能出现库存不一致。

**解决方案：** 库存归还也应通过 Lua 脚本执行，保证原子性：

```lua
-- seckill_rollback.lua
-- KEYS[1] = seckill:batch:stock:{batchId}
-- ARGV[1] = 归还数量

local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))
if stock then
    redis.call('HINCRBY', KEYS[1], 'stock', tonumber(ARGV[1]))
    redis.call('HINCRBY', KEYS[1], 'version', 1)
    return 1
end
return 0
```

### 8.4 用户封禁与会员权益的交互

**问题：** 被封禁的用户持有未使用的编码券，封禁期间券过期怎么办？

**策略：**
- 封禁期间，券的领取过期计时**不暂停**（实现复杂度太高）。
- 封禁期间，会员服务过期计时**也不暂停**。
- 解封后，用户可查看已过期的券/会员状态，但无法恢复。
- 管理员可通过后台手动补偿（吊销旧券 -> 发新券）。

### 8.5 编码券的退款机制

PLAN-SECKILL.md 配置了 `coupon.refund-hours: 1`（允许退款时长1小时），但未设计退款流程。

**退款流程：**
1. 用户在激活后 1 小时内可申请退款。
2. 退款时撤销会员时间（`vipExpireTime` 减去券天数）。
3. 如果减去后会员已过期，则 `vipType` 回退为 0。
4. 券状态变为 4（已退款），库存不归还（券已使用过）。
5. 空间同步降级（如果触发降级条件）。

```java
public void refundCoupon(Long userId, Long couponId) {
    // 1. 校验：券归属、状态=2（已激活）、激活时间在1小时内
    CodeCoupon coupon = couponMapper.selectById(couponId);
    ThrowUtils.throwIf(!userId.equals(coupon.getUserId()), ErrorCode.NO_AUTH_ERROR);
    ThrowUtils.throwIf(coupon.getStatus() != 2, ErrorCode.PARAMS_ERROR, "券状态不支持退款");

    long activatedAt = coupon.getActivatedAt().getTime();
    long now = System.currentTimeMillis();
    long refundWindow = couponRefundHours * 3600 * 1000L;
    ThrowUtils.throwIf(now - activatedAt > refundWindow, ErrorCode.PARAMS_ERROR, "已超过退款时限");

    // 2. 事务内执行
    transactionTemplate.execute(status -> {
        // 2a. 扣减会员时间
        User user = userService.getById(userId);
        Date newExpireTime = new Date(user.getVipExpireTime().getTime() - (long) coupon.getType() * 24 * 3600 * 1000);
        user.setVipExpireTime(newExpireTime);
        user.setVipTotalDays(Math.max(0, user.getVipTotalDays() - coupon.getType()));
        if (newExpireTime.before(new Date())) {
            user.setVipType(0);
        }
        userService.updateById(user);

        // 2b. 更新券状态
        coupon.setStatus(4); // 已退款
        couponMapper.updateById(coupon);
        return true;
    });

    // 3. 失效缓存
    invalidateUserCache(userId);

    // 4. 降级空间（如果需要）
    if (newExpireTime.before(new Date())) {
        downgradeSpaceForExpiredVip(userId);
    }
}
```

### 8.6 秒杀抢购前的会员状态校验

**问题：** 已经是有效 VIP 会员的用户是否还能参与秒杀？

**策略选项：**
- 方案 A：允许。用户可以为后续续费囤券。（推荐）
- 方案 B：禁止。避免资源浪费，让更多人获得机会。

**推荐方案 A**，理由：
- 编码券有时间限制（领取后 N 天不激活就过期），用户囤积的动力有限。
- 禁止已有会员参与会增加抢购接口的复杂度（需要额外查库/缓存判断会员状态）。
- 产品角度，付费用户获得更多权益是合理的。

### 8.7 激活编码券时的并发安全

**场景：** 用户快速双击"使用"按钮，可能触发两次激活。

**防护措施：**
1. **前端防抖**：按钮点击后立即禁用，等待接口返回。
2. **后端分布式锁**：`lock:coupon:activate:{userId}:{couponId}`，TTL 10 秒。
3. **数据库乐观锁**：`code_coupon` 表的 `version` 字段（原方案已设计）。

```java
// 激活时使用乐观锁
coupon.setVersion(coupon.getVersion()); // WHERE version = ?
coupon.setStatus(2);
int rows = couponMapper.updateById(coupon);
ThrowUtils.throwIf(rows == 0, ErrorCode.OPERATION_ERROR, "激活失败，请重试");
```

### 8.8 Redis-DB 库存对账任务中的编码券状态一致性

PLAN-SECKILL.md 提到了对账任务但未详细设计。补充对账逻辑：

```java
/**
 * Redis-DB 库存对账 — 每10分钟
 */
@Scheduled(cron = "0 */10 * * * ?")
public void reconcileStock() {
    String lockKey = "lock:seckill:reconcile";
    if (!tryLock(lockKey, 600)) {
        return;
    }
    try {
        // 查询所有进行中的批次
        List<CodeCouponBatch> activeBatches = batchMapper.selectList(
            new LambdaQueryWrapper<CodeCouponBatch>()
                .in(CodeCouponBatch::getStatus, Arrays.asList(1, 2))
        );

        for (CodeCouponBatch batch : activeBatches) {
            // DB 层面计算实际库存
            Long dbStock = couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                    .eq(CodeCoupon::getBatchId, batch.getId())
                    .eq(CodeCoupon::getStatus, 0) // 未发放
            );

            // Redis 层面获取库存
            String stockKey = String.format(SECKILL_BATCH_STOCK, batch.getId());
            String redisStock = redis.opsForHash().get(stockKey, "stock").toString();

            long diff = Long.parseLong(redisStock) - dbStock;
            if (Math.abs(diff) > 5) {
                // 差异超过阈值，告警
                log.error("库存对账异常! batchId={}, Redis={}, DB={}, diff={}",
                    batch.getId(), redisStock, dbStock, diff);
                // 以 DB 为准修正 Redis
                redis.opsForHash().put(stockKey, "stock", String.valueOf(dbStock));
            }
        }
    } finally {
        unlock(lockKey);
    }
}
```

### 8.9 VIP 到期提前提醒

新增定时任务，在 VIP 到期前 3 天和 1 天分别发送提醒通知：

```java
/**
 * VIP到期提醒 — 每天上午 10 点
 */
@Scheduled(cron = "0 0 10 * * ?")
public void remindVipExpiring() {
    Date now = new Date();
    int[] daysBefore = {3, 1}; // 提前3天、1天提醒

    for (int days : daysBefore) {
        Date remindTime = new Date(now.getTime() + (long) days * 24 * 3600 * 1000);
        Date remindStart = new Date(remindTime.getTime() - 12 * 3600 * 1000); // 前后12小时窗口

        List<User> expiringUsers = userService.list(
            new LambdaQueryWrapper<User>()
                .eq(User::getVipType, 1)
                .between(User::getVipExpireTime, remindStart, remindTime)
        );

        for (User user : expiringUsers) {
            // 防重：检查今天是否已发过提醒
            String remindKey = String.format(VIP_EXPIRE_REMIND_KEY, user.getId());
            if (!redis.opsForValue().setIfAbsent(remindKey, "1", 24, TimeUnit.HOURS)) {
                continue;
            }

            long remainingMs = user.getVipExpireTime().getTime() - now.getTime();
            int remainingDays = (int) (remainingMs / (24 * 3600 * 1000));

            NotificationEvent event = new NotificationEvent(
                this, user.getId(), 0L, "系统", null,
                NotificationTypeEnum.VIP_EXPIRING, "VIP即将到期",
                String.format("您的VIP会员将在 %d 天后到期，届时部分功能将受到限制。", remainingDays),
                null, null
            );
            eventPublisher.publishEvent(event);
        }
    }
}
```

### 8.10 管理后台会员管理接口

补充 PLAN-SECKILL.md 管理后台中未涉及的会员管理接口：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/vip/list` | GET | 会员列表（支持按状态、到期时间筛选） |
| `/api/admin/vip/stats` | GET | 会员统计（总人数、活跃会员数、今日新增、即将到期） |
| `/api/admin/vip/{userId}/grant` | POST | 手动赠送 VIP（管理员操作，指定天数） |
| `/api/admin/vip/{userId}/revoke` | POST | 撤销 VIP（立即过期） |
| `/api/admin/coupon/{id}/reassign` | POST | 将券转给其他用户（运营补偿用） |

---

## 九、枚举类新增

### 9.1 VipTypeEnum

在 `common/src/main/java/org/example/common/enums/` 下新增：

```java
package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum VipTypeEnum {

    NORMAL("普通用户", 0),
    VIP("VIP会员", 1);

    private final String text;
    private final int value;

    VipTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static VipTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (VipTypeEnum e : VipTypeEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
```

### 9.2 CouponStatusEnum

```java
package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum CouponStatusEnum {

    UNSOLD("未发放", 0),
    CLAIMED("已领取未使用", 1),
    ACTIVATED("已激活使用中", 2),
    EXPIRED("已过期", 3),
    REFUNDED("已退款", 4);

    private final String text;
    private final int value;

    CouponStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static CouponStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CouponStatusEnum e : CouponStatusEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
```

---

## 十、编码券激活接口完整设计

### 10.1 接口定义

```
POST /api/coupon/activate
```

**请求参数：**
```json
{
    "couponId": 101
}
```

**成功响应：**
```json
{
    "code": 0,
    "data": {
        "activated": true,
        "vipType": 1,
        "vipTypeName": "VIP会员",
        "vipExpireTime": "2026-07-06T14:30:00",
        "couponType": 30,
        "couponTypeName": "30天VIP",
        "message": "激活成功，您已获得30天VIP会员"
    }
}
```

**失败场景：**

```json
// 券不存在
{ "code": 4001, "message": "编码券不存在" }

// 非本人券
{ "code": 4002, "message": "无权操作此编码券" }

// 券状态不对
{ "code": 4003, "message": "编码券状态异常，无法激活" }

// 券已过期（领取后未使用）
{ "code": 4004, "message": "编码券已过期，无法激活" }

// 重复激活
{ "code": 4005, "message": "编码券已被使用" }

// 限流
{ "code": 4291, "message": "操作过于频繁，请稍后再试" }
```

### 10.2 Service 层核心方法签名

```java
// CouponService.java
public interface CouponService extends IService<CodeCoupon> {

    /**
     * 用户激活编码券
     * @param userId 用户ID
     * @param couponId 编码券ID
     * @return 激活结果VO
     */
    CouponActivateVO activateCoupon(Long userId, Long couponId);

    /**
     * 查询用户的券列表
     */
    Page<CouponVO> listMyCoupons(Long userId, Integer status, int page, int size);

    /**
     * 退款
     */
    void refundCoupon(Long userId, Long couponId);
}
```

---

## 十一、开发排期补充

以下任务需要追加到 PLAN-SECKILL.md 的开发排期中：

### Phase 2.5 — 会员基础能力（P0，与秒杀流程并行）

| 序号 | 任务 | 预估工时 |
|------|------|---------|
| T-01 | User 表 ALTER + Entity/VO/Mapper 更新 | 0.5d |
| T-02 | VipTypeEnum / CouponStatusEnum 枚举 | 0.5d |
| T-03 | RedisKeyConstants 会员模块 Key | 0.5d |
| T-04 | CheckAuth 注解 + AOP 扩展（requireVip） | 1d |
| T-05 | SpaceServiceImpl 权限逻辑修改 | 0.5d |
| T-06 | VIP 状态工具方法（common 模块） | 0.5d |

### Phase 3.5 — 编码券激活与会员生命周期（P0）

| 序号 | 任务 | 预估工时 |
|------|------|---------|
| T-07 | CouponService.activateCoupon 核心实现 | 2d |
| T-08 | VIP 空间自动升级/降级 | 1d |
| T-09 | VIP 过期定时任务 + 到期提醒 | 1d |
| T-10 | 编码券退款功能 | 1d |
| T-11 | 会员状态查询接口 | 0.5d |
| T-12 | 前端：VIP 状态展示 + 激活流程 | 1.5d |

### Phase 4.5 — 管理后台会员管理（P1）

| 序号 | 任务 | 预估工时 |
|------|------|---------|
| T-13 | 管理后台：会员列表/统计/手动操作 | 1.5d |
| T-14 | Redis-DB 库存对账完善 | 1d |
| T-15 | 库存归还 Lua 脚本改造 | 0.5d |

---

## 十二、总结

本补充方案覆盖了 PLAN-SECKILL.md 中缺失的会员体系全链路设计：

1. **数据库层**：User 表新增 4 个 VIP 字段（`vipType`/`vipExpireTime`/`vipActivatedAt`/`vipTotalDays`），与现有 ban 模块字段模式一致。
2. **激活流程**：编码券 `status=1 -> 2` 的状态转换触发 User 表会员字段更新，事务保证一致性，支持多券叠加。
3. **权益拦截**：通过扩展 `@CheckAuth(requireVip=true)` 注解实现声明式 VIP 权限校验，无需侵入业务代码。
4. **生命周期**：VIP 过期定时任务自动降级，空间同步降级，到期前提醒通知。
5. **安全加固**：激活接口防重、限流、归属校验、乐观锁；库存归还 Lua 脚本原子化；Redis-DB 对账机制。
6. **边界情况**：封禁用户券处理、退款流程、已有会员抢购策略、空间超额降级策略。
