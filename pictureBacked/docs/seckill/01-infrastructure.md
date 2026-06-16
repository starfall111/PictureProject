# Phase 1 — 基础设施

> 优先级：P0 | 依赖：无 | 预估工时：1.5d

## 目标

搭建秒杀编码券系统的数据库、缓存、消息队列基础设施，以及所有新表的 Entity/Mapper/Service 骨架代码。

---

## 1. 数据库建表

### 1.1 新建 SQL 文件

创建 `sql/seckill_tables.sql`，包含以下内容：

```sql
-- ============================================================
-- Phase 1: 秒杀编码券系统 — 数据库建表
-- 依赖: database.sql 中的 user 表已存在
-- 执行顺序: 先执行本文件的 CREATE TABLE，再执行 ALTER TABLE
-- ============================================================

-- 1. 发放批次表
CREATE TABLE IF NOT EXISTS code_coupon_batch (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no        VARCHAR(32) UNIQUE NOT NULL COMMENT '批次号，如 BATCH20260606001',
    name            VARCHAR(128) NOT NULL COMMENT '批次名称',
    type            TINYINT NOT NULL COMMENT '券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP',
    total_stock     INT NOT NULL COMMENT '总库存（发放总量）',
    current_stock   INT NOT NULL DEFAULT 0 COMMENT '当前剩余库存',
    start_time      DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time        DATETIME DEFAULT NULL COMMENT '秒杀结束时间（NULL表示发完即止）',
    status          TINYINT DEFAULT 0 COMMENT '0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    editTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status_time (status, start_time)
) COMMENT '编码券发放批次' COLLATE = utf8mb4_unicode_ci;

-- 2. 编码券表
CREATE TABLE IF NOT EXISTS code_coupon (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id        BIGINT NOT NULL COMMENT '批次ID',
    user_id         BIGINT DEFAULT NULL COMMENT '用户ID（未发放时为NULL）',
    code            VARCHAR(32) UNIQUE NOT NULL COMMENT '唯一编码券码',
    type            TINYINT NOT NULL COMMENT '3/7/30天',
    status          TINYINT DEFAULT 0 COMMENT '0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款',
    issued_at       DATETIME DEFAULT NULL COMMENT '领取时间',
    activated_at    DATETIME DEFAULT NULL COMMENT '激活（使用）时间',
    expire_at       DATETIME NOT NULL COMMENT '过期时间（领取后N天）',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_code (code),
    KEY idx_batch_user (batch_id, user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_status_expire (status, expire_at)
) COMMENT '编码券' COLLATE = utf8mb4_unicode_ci;

-- 3. 秒杀订单/防重表
CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    batch_id        BIGINT NOT NULL COMMENT '批次ID',
    coupon_id       BIGINT DEFAULT NULL COMMENT '编码券ID（异步写入后回填）',
    order_no        VARCHAR(32) UNIQUE NOT NULL COMMENT '订单号',
    status          TINYINT DEFAULT 0 COMMENT '0-处理中, 1-成功, 2-失败',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_user_batch (user_id, batch_id),
    KEY idx_order_no (order_no)
) COMMENT '秒杀订单（防重+状态追踪）' COLLATE = utf8mb4_unicode_ci;

-- 4. User 表扩展 — 会员字段
ALTER TABLE `user`
    ADD COLUMN `vipType`          TINYINT    NOT NULL DEFAULT 0 COMMENT '会员类型: 0-普通用户, 1-VIP会员' AFTER `userRole`,
    ADD COLUMN `vipExpireTime`    DATETIME   DEFAULT NULL COMMENT 'VIP会员到期时间（NULL表示非会员）' AFTER `vipType`,
    ADD COLUMN `vipActivatedAt`   DATETIME   DEFAULT NULL COMMENT '最近一次激活VIP的时间' AFTER `vipExpireTime`,
    ADD COLUMN `vipTotalDays`     INT        NOT NULL DEFAULT 0 COMMENT 'VIP累计总天数（含历史）' AFTER `vipActivatedAt`,
    ADD INDEX `idx_vip_status` (`vipType`, `vipExpireTime`);
```

### 1.2 设计说明

- 字段命名遵循项目现有规范（`createTime`/`updateTime`/`isDelete`/`editTime`），而非下划线风格
- `code_coupon_batch` 与 `code_coupon` 不使用外键约束（高并发场景外锁性能差），通过应用层保证一致性
- `seckill_order` 的 `uk_user_batch` 唯一索引充当数据库层防重保障
- `User` 表 ALTER 中 `vipType` 默认值 0，不影响现有用户数据

---

## 2. Entity 层

### 2.1 CodeCouponBatch 实体

创建 `pojo/src/main/java/org/example/pojo/entity/CodeCouponBatch.java`：

```java
package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 编码券发放批次
 */
@Data
@TableName("code_coupon_batch")
public class CodeCouponBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String name;

    /**
     * 券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP
     */
    private Integer type;

    private Integer totalStock;

    private Integer currentStock;

    private Date startTime;

    /**
     * 秒杀结束时间（NULL表示发完即止）
     */
    private Date endTime;

    /**
     * 0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消
     */
    private Integer status;

    @Version
    private Integer version;

    private Date editTime;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
```

### 2.2 CodeCoupon 实体

创建 `pojo/src/main/java/org/example/pojo/entity/CodeCoupon.java`：

```java
package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 编码券
 */
@Data
@TableName("code_coupon")
public class CodeCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    /**
     * 用户ID（未发放时为NULL）
     */
    private Long userId;

    /**
     * 唯一编码券码
     */
    private String code;

    /**
     * 券类型天数: 3/7/30
     */
    private Integer type;

    /**
     * 0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款
     */
    private Integer status;

    /**
     * 领取时间
     */
    private Date issuedAt;

    /**
     * 激活时间
     */
    private Date activatedAt;

    /**
     * 过期时间（领取后N天）
     */
    private Date expireAt;

    @Version
    private Integer version;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
```

### 2.3 SeckillOrder 实体

创建 `pojo/src/main/java/org/example/pojo/entity/SeckillOrder.java`：

```java
package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 秒杀订单（防重+状态追踪）
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long batchId;

    /**
     * 编码券ID（异步写入后回填）
     */
    private Long couponId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 0-处理中, 1-成功, 2-失败
     */
    private Integer status;

    private Date createTime;
}
```

---

## 3. Mapper 层

### 3.1 CodeCouponBatchMapper

创建 `server/src/main/java/org/example/server/mapper/CodeCouponBatchMapper.java`：

```java
package org.example.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.entity.CodeCouponBatch;

@Mapper
public interface CodeCouponBatchMapper extends BaseMapper<CodeCouponBatch> {
}
```

创建 `server/src/main/resources/mapper/CodeCouponBatchMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.server.mapper.CodeCouponBatchMapper">

    <sql id="Base_Column_List">
        id,batchNo,name,type,totalStock,currentStock,
        startTime,endTime,status,version,
        editTime,createTime,updateTime,isDelete
    </sql>

</mapper>
```

### 3.2 CodeCouponMapper

创建 `server/src/main/java/org/example/server/mapper/CodeCouponMapper.java`：

```java
package org.example.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.entity.CodeCoupon;

@Mapper
public interface CodeCouponMapper extends BaseMapper<CodeCoupon> {
}
```

创建 `server/src/main/resources/mapper/CodeCouponMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.server.mapper.CodeCouponMapper">

    <sql id="Base_Column_List">
        id,batchId,userId,code,type,status,
        issuedAt,activatedAt,expireAt,version,
        createTime,updateTime,isDelete
    </sql>

</mapper>
```

### 3.3 SeckillOrderMapper

创建 `server/src/main/java/org/example/server/mapper/SeckillOrderMapper.java`：

```java
package org.example.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.pojo.entity.SeckillOrder;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
```

---

## 4. Service 接口骨架

### 4.1 SeckillService

创建 `server/src/main/java/org/example/server/service/SeckillService.java`：

```java
package org.example.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.SeckillOrder;

/**
 * 秒杀服务
 */
public interface SeckillService extends IService<SeckillOrder> {

    /**
     * 获取秒杀 Token（防脚本刷接口）
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @return Token 字符串
     */
    String getToken(Long userId, Long batchId);

    /**
     * 秒杀抢购
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @param token   秒杀Token
     * @param clientIP 客户端IP
     * @return 订单号
     */
    String grab(Long userId, Long batchId, String token, String clientIP);

    /**
     * 查询抢购结果
     *
     * @param userId   用户ID
     * @param orderNo  订单号
     * @return 订单信息
     */
    SeckillOrder getResult(Long userId, String orderNo);

    /**
     * 预热批次（生成编码券 + 写 Redis）
     *
     * @param batchId 批次ID
     */
    void preheat(Long batchId);
}
```

### 4.2 CouponService

创建 `server/src/main/java/org/example/server/service/CouponService.java`：

```java
package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.CodeCoupon;

/**
 * 编码券服务
 */
public interface CouponService extends IService<CodeCoupon> {

    /**
     * 用户激活编码券
     *
     * @param userId   用户ID
     * @param couponId 编码券ID
     * @return 激活结果（VIP到期时间等信息）
     */
    Object activateCoupon(Long userId, Long couponId);

    /**
     * 查询用户的券列表
     *
     * @param userId 用户ID
     * @param status 券状态（null=全部）
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    Page<CodeCoupon> listMyCoupons(Long userId, Integer status, int page, int size);

    /**
     * 编码券退款
     *
     * @param userId   用户ID
     * @param couponId 编码券ID
     */
    void refundCoupon(Long userId, Long couponId);
}
```

---

## 5. Redis Key 常量

在 `common/src/main/java/org/example/common/constants/RedisKeyConstants.java` 末尾追加：

```java
// ==================== 秒杀模块 Key ====================

/**
 * 批次库存 Hash — Field: stock(int), version(int)
 * 格式: seckill:batch:stock:{batchId}
 */
public static final String SECKILL_BATCH_STOCK = "seckill:batch:stock:%d";

/**
 * 批次详情（预热数据）— STRING (JSON)
 * 格式: seckill:batch:info:{batchId}
 */
public static final String SECKILL_BATCH_INFO = "seckill:batch:info:%d";

/**
 * 用户幂等 Token — STRING "1", TTL 5min
 * 格式: seckill:token:{tokenValue}
 */
public static final String SECKILL_TOKEN = "seckill:token:%s";

/**
 * 防重标记 — STRING couponId, TTL 24h
 * 格式: seckill:dedupe:{userId}:{batchId}
 */
public static final String SECKILL_DEDUPE = "seckill:dedupe:%d:%d";

/**
 * 秒杀分布式锁
 * 格式: lock:seckill:batch:{batchId}
 */
public static final String LOCK_SECKILL_BATCH = "lock:seckill:batch:%d";

/**
 * 降级开关 — STRING level(0-4)
 * 0-正常, 1-跳过Redis限流, 2-MQ降级同步写DB, 3-只读, 4-全部降级
 */
public static final String SECKILL_DEGRADE = "seckill:degrade:level";

/**
 * 秒杀Token有效期（秒）
 */
public static final int SECKILL_TOKEN_TTL = 300;

/**
 * 防重标记有效期（秒）
 */
public static final int SECKILL_DEDUPE_TTL = 86400;

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

/**
 * 编码券激活限流窗口（秒）
 */
public static final int COUPON_ACTIVATE_RATE_LIMIT_WINDOW = 60;

/**
 * 编码券激活限流最大次数
 */
public static final int COUPON_ACTIVATE_RATE_LIMIT_MAX = 5;

/**
 * VIP过期处理分布式锁
 * 格式: lock:vip:expire
 */
public static final String LOCK_VIP_EXPIRE = "lock:vip:expire";

/**
 * 库存对账分布式锁
 * 格式: lock:seckill:reconcile
 */
public static final String LOCK_SECKILL_RECONCILE = "lock:seckill:reconcile";

/**
 * Pexels API 配额监控 — 每小时
 * 格式: pexels:quota:hourly:{yyyyMMddHH}
 */
public static final String PEXELS_HOURLY_COUNT = "pexels:quota:hourly:%s";

/**
 * Pexels API 配额监控 — 每月
 * 格式: pexels:quota:monthly:{yyyyMM}
 */
public static final String PEXELS_MONTHLY_COUNT = "pexels:quota:monthly:%s";
```

---

## 6. Lua 脚本

### 6.1 扣库存脚本

创建 `server/src/main/resources/lua/seckill_deduct.lua`：

```lua
-- seckill_deduct.lua
-- KEYS[1] = seckill:batch:stock:{batchId}
-- ARGV[1] = 扣减数量（1）
-- ARGV[2] = 当前版本号

local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))
local version = tonumber(redis.call('HGET', KEYS[1], 'version'))

if stock == nil or stock <= 0 then
    return -1  -- 库存不足
end

if version ~= tonumber(ARGV[2]) then
    return -2  -- 版本不一致（已被修改）
end

redis.call('HINCRBY', KEYS[1], 'stock', -tonumber(ARGV[1]))
redis.call('HINCRBY', KEYS[1], 'version', 1)
return 1  -- 扣减成功
```

### 6.2 库存归还脚本

创建 `server/src/main/resources/lua/seckill_rollback.lua`：

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

### 6.3 Redis 配置加载 Lua 脚本

在 `server/src/main/java/org/example/server/config/RedisConfig.java` 中追加 Bean：

```java
/**
 * 秒杀扣库存 Lua 脚本
 */
@Bean
public DefaultRedisScript<Long> seckillDeductScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("lua/seckill_deduct.lua"));
    script.setResultType(Long.class);
    return script;
}

/**
 * 秒杀库存归还 Lua 脚本
 */
@Bean
public DefaultRedisScript<Long> seckillRollbackScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("lua/seckill_rollback.lua"));
    script.setResultType(Long.class);
    return script;
}
```

---

## 7. MQ 队列配置

在 `server/src/main/java/org/example/server/config/RabbitMQConfig.java` 中追加：

```java
// ---- 秒杀抢购队列 ----

public static final String SECKILL_QUEUE = "seckill.queue";
public static final String SECKILL_EXCHANGE = "seckill.exchange";
public static final String SECKILL_ROUTING_KEY = "seckill.grab";

public static final String SECKILL_DLQ = "seckill.dead.queue";
public static final String SECKILL_DLX = "seckill.dlx";
public static final String SECKILL_DL_ROUTING_KEY = "seckill.dead";

@Bean
public Queue seckillQueue() {
    return QueueBuilder.durable(SECKILL_QUEUE)
            .withArgument("x-dead-letter-exchange", SECKILL_DLX)
            .withArgument("x-dead-letter-routing-key", SECKILL_DL_ROUTING_KEY)
            .build();
}

@Bean
public TopicExchange seckillExchange() {
    return new TopicExchange(SECKILL_EXCHANGE, true, false);
}

@Bean
public Binding seckillBinding() {
    return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
}

@Bean
public Queue seckillDeadQueue() {
    return QueueBuilder.durable(SECKILL_DLQ).build();
}

@Bean
public DirectExchange seckillDeadExchange() {
    return new DirectExchange(SECKILL_DLX, true, false);
}

@Bean
public Binding seckillDeadBinding() {
    return BindingBuilder.bind(seckillDeadQueue()).to(seckillDeadExchange()).with(SECKILL_DL_ROUTING_KEY);
}
```

同时，在 `DeadLetterConsumer.java` 中追加秒杀死信消费方法：

```java
@RabbitListener(queues = "seckill.dead.queue")
public void handleSeckillDead(String message) {
    log.error("【死信告警】秒杀消息消费失败，需人工排查 | payload={}", message);
}
```

---

## 8. MQ 消息 DTO

创建 `pojo/src/main/java/org/example/pojo/dto/seckill/SeckillMessage.java`：

```java
package org.example.pojo.dto.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long batchId;

    private String orderNo;
}
```

---

## 9. application.yml 配置追加

在 `server/src/main/resources/application.yml` 中追加：

```yaml
# 秒杀系统配置
seckill:
  lock-timeout: 10
  rate-limit:
    ip-window: 60
    ip-max: 10
    user-window: 3600
    user-max: 3
  token-ttl: 300
  dedupe-ttl: 86400
  coupon:
    refund-hours: 1
    code-prefix: "VIP"
  pexels:
    hourly-limit: 200
    monthly-limit: 20000
```

---

## 验证清单

- [ ] 执行 `sql/seckill_tables.sql`，确认 3 张新表创建成功
- [ ] 执行 ALTER TABLE，确认 User 表新增 4 个 VIP 字段
- [ ] 项目编译通过（`mvn compile`）
- [ ] MyBatis-Plus 能正确扫描到 3 个新 Mapper
- [ ] RedisConfig 加载两个 Lua 脚本无报错
- [ ] RabbitMQ 启动后自动创建 seckill.queue + seckill.dead.queue
- [ ] RedisKeyConstants 编译通过
