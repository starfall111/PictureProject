# 举报与封禁模块技术实现方案

**版本**: 1.0  
**作者**: 后端架构师  
**创建日期**: 2026-06-05  
**基于 PRD**: prd_report_ban_mechanism.md v1.0  

---

## 目录

1. [架构设计](#1-架构设计)
2. [数据库设计](#2-数据库设计)
3. [Redis 缓存设计](#3-redis-缓存设计)
4. [API 接口设计](#4-api-接口设计)
5. [核心模块改造](#5-核心模块改造)
6. [代码结构规划](#6-代码结构规划)
7. [定时任务设计](#7-定时任务设计)
8. [性能与安全](#8-性能与安全)
9. [集成方案](#9-集成方案)
10. [实施计划](#10-实施计划)

---

## 1. 架构设计

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                           前端应用                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   用户端     │  │   管理端     │  │   移动端     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot 应用层                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Controller 层                         │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │ReportController│ │AdminReport  │  │AdminBan      │   │    │
│  │  │               │  │Controller    │  │Controller    │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                    │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Service 层                            │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │ReportService │  │BanService    │  │Notification  │   │    │
│  │  │               │  │              │  │Service       │   │    │
│  │  │ 限流检查     │  │ 封禁执行     │  │ 通知发送     │   │    │
│  │  │ 防重复检查   │  │ 自动解封     │  │              │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                    │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                  Interceptor 层                          │    │
│  │  ┌──────────────────────────────────────────────────┐   │    │
│  │  │        LoginInterceptor（新增封禁检查）           │   │    │
│  │  │  1. 验证 Session                                   │   │    │
│  │  │  2. 查询 Redis 黑名单                              │   │    │
│  │  │  3. 降级到 DB 查询                                 │   │    │
│  │  │  4. 封禁则抛异常                                   │   │    │
│  │  └──────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据持久层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   MySQL     │  │    Redis     │  │  RabbitMQ    │         │
│  │              │  │              │  │              │         │
│  │ report 表    │  │ 黑名单 ZSET  │  │ 异步消息     │         │
│  │ ban_record   │  │ 限流计数器   │  │              │         │
│  │ user (扩展)  │  │ 去重标记     │  │              │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| Web 框架 | Spring Boot 3.4 | Jakarta EE |
| ORM | MyBatis-Plus | 数据库操作 |
| 缓存 | Redis 7.x | 黑名单 + 限流 |
| 消息队列 | RabbitMQ | 异步通知 |
| Session 管理 | Spring Session + Redis | 分布式 Session |
| 定时任务 | Spring @Scheduled | 封禁解封 |
| 限流组件 | Redis Lua 脚本 | 滑动窗口 |

---

## 2. 数据库设计

### 2.1 举报表（report）

```sql
CREATE TABLE `report` (
  `id` BIGINT NOT NULL COMMENT '举报ID（雪花算法）',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '举报对象类型：PICTURE/USER',
  `target_id` BIGINT NOT NULL COMMENT '举报目标ID（图片ID或用户ID）',
  `reason_type` VARCHAR(20) NOT NULL COMMENT '举报原因：PORNOGRAPHY/VIOLENCE/SCAM/COPYRIGHT/SPAM/HARASSMENT/OTHER',
  `description` TEXT COMMENT '补充描述（可选）',
  
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/APPROVED/REJECTED/WARN_ONLY/REMOVE_ONLY',
  `handler_id` BIGINT COMMENT '处理人ID（管理员）',
  `handle_result` VARCHAR(50) COMMENT '处理结果：WARN/BAN_TEMP_3/BAN_TEMP_7/BAN_TEMP_30/BAN_PERMANENT/REMOVE_PICTURE/REJECT',
  `handle_reason` TEXT COMMENT '处理理由',
  `handle_time` DATETIME COMMENT '处理时间',
  
  `report_count` INT NOT NULL DEFAULT 0 COMMENT '该目标累计被举报次数（冗余字段）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0=否 1=是',
  
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_status_count` (`status`, `report_count` DESC, `create_time`),
  KEY `idx_reporter_dup` (`reporter_id`, `target_type`, `target_id`),
  KEY `idx_target_count` (`target_type`, `target_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 雪花算法生成，全局唯一 |
| reporter_id | BIGINT | 关联 user.id |
| target_type | VARCHAR(20) | PICTURE=图片，USER=用户 |
| target_id | BIGINT | 根据 target_type 关联 picture.id 或 user.id |
| reason_type | VARCHAR(20) | 举报原因代码（枚举） |
| status | VARCHAR(20) | 状态流转：PENDING → PROCESSING → APPROVED/REJECTED/WARN_ONLY/REMOVE_ONLY |
| handle_result | VARCHAR(50) | 处理结果代码（枚举） |
| report_count | INT | 冗余字段，便于管理员列表排序 |

**索引说明**：

| 索引名 | 字段 | 用途 |
|--------|------|------|
| idx_target | target_type, target_id | 查询特定目标的所有举报 |
| idx_status_count | status, report_count DESC, create_time | 管理员列表主查询（按被举报次数排序） |
| idx_reporter_dup | reporter_id, target_type, target_id | 防重复举报检查 |
| idx_target_count | target_type, target_id, status | 统计被举报次数 |

### 2.2 封禁记录表（ban_record）

```sql
CREATE TABLE `ban_record` (
  `id` BIGINT NOT NULL COMMENT '封禁记录ID（雪花算法）',
  `user_id` BIGINT NOT NULL COMMENT '被封禁用户ID',
  `ban_type` VARCHAR(20) NOT NULL COMMENT '封禁类型：TEMP/PERMANENT',
  `ban_duration` INT COMMENT '封禁天数（临时封禁时填写：3/7/30）',
  `ban_start_time` DATETIME NOT NULL COMMENT '封禁开始时间',
  `ban_end_time` DATETIME COMMENT '封禁结束时间（永久封禁为NULL）',
  `ban_reason` VARCHAR(200) NOT NULL COMMENT '封禁原因',
  `violation_count` INT NOT NULL DEFAULT 0 COMMENT '封禁时的累计违规次数',
  
  `unbanned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封：0=否 1=是',
  `unban_time` DATETIME COMMENT '解封时间',
  `unban_reason` TEXT COMMENT '解封理由',
  `unban_operator_id` BIGINT COMMENT '解封操作人ID',
  
  `ban_operator_id` BIGINT NOT NULL COMMENT '封禁操作人ID',
  `ban_operator_name` VARCHAR(100) NOT NULL COMMENT '封禁操作人姓名（冗余）',
  `report_id` BIGINT COMMENT '关联的举报ID',
  
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0=否 1=是',
  
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `ban_start_time` DESC),
  KEY `idx_unbanned_end` (`unbanned`, `ban_end_time`),
  KEY `idx_ban_end` (`ban_end_time`, `unbanned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封禁记录表';
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| ban_type | VARCHAR(20) | TEMP=临时封禁，PERMANENT=永久封禁 |
| ban_duration | INT | 封禁天数：3、7、30 |
| ban_end_time | DATETIME | 计算公式：ban_start_time + INTERVAL ban_duration DAY |
| unbanned | TINYINT | 0=生效中，1=已解封 |
| ban_operator_name | VARCHAR(100) | 冗余字段，避免关联查询 |

### 2.3 User 表扩展字段

```sql
ALTER TABLE `user` 
ADD COLUMN `ban_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '封禁状态：NONE/TEMP/PERMANENT' AFTER `user_role`,
ADD COLUMN `ban_end_time` DATETIME DEFAULT NULL COMMENT '封禁结束时间' AFTER `ban_status`,
ADD COLUMN `violation_count` INT NOT NULL DEFAULT 0 COMMENT '累计违规次数（6个月滚动窗口）' AFTER `ban_end_time`,
ADD COLUMN `last_violation_time` DATETIME DEFAULT NULL COMMENT '最近一次违规时间' AFTER `violation_count`,
ADD INDEX `idx_ban_status` (`ban_status`, `ban_end_time`);
```

**字段说明**：

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| ban_status | VARCHAR(20) | NONE | 封禁状态枚举：NONE=正常，TEMP=临时封禁，PERMANENT=永久封禁 |
| ban_end_time | DATETIME | NULL | NULL 表示永久封禁或未封禁 |
| violation_count | INT | 0 | 累计违规次数，用于递进式封禁判断 |
| last_violation_time | DATETIME | NULL | 最近违规时间，用于计算 6 个月滚动窗口 |

---

## 3. Redis 缓存设计

### 3.1 缓存结构总览

| Key 模板 | 类型 | TTL | 用途 |
|---------|------|-----|------|
| ban:status:{userId} | STRING (JSON) | 30 分钟 | 单用户封禁状态缓存 |
| ban:blacklist | ZSET | 永久 | 黑名单（member=userId, score=banEndTime） |
| rate_limit:report:{userId} | STRING | 1 分钟 | 举报限流计数器 |
| report:dup:{reporterId}:{targetType}:{targetId} | STRING | 7 天 | 防重复举报标记 |
| report:credit:{userId} | STRING | 24 小时 | 举报人信誉分 |

### 3.2 封禁黑名单缓存

**单个用户状态缓存**：

```
Key: ban:status:{userId}
Type: STRING (JSON)
Value: {
  "banStatus": "TEMP",
  "banEndTime": 1717555200000,
  "banReason": "发布违规内容"
}
TTL: 1800 秒（30 分钟）
```

**黑名单 ZSET**：

```
Key: ban:blacklist
Type: ZSET
Member: userId
Score: banEndTime 毫秒时间戳（永久封禁为 9223372036854775807）
TTL: 永久
```

**用途**：
- 登录拦截时快速查询用户封禁状态
- 定时任务扫描即将过期的封禁（ZSET rangeByScore）

### 3.3 举报频率限制缓存

**滑动窗口限流**：

```
Key: rate_limit:report:{userId}
Type: STRING（计数器）
Value: 当前分钟内举报次数
TTL: 60 秒（滑动窗口）
```

**限流规则**：
- 每用户每分钟最多 3 次举报
- 使用 INCR + EXPIRE 实现原子操作

**防重复举报**：

```
Key: report:dup:{reporterId}:{targetType}:{targetId}
Type: STRING
Value: "1"
TTL: 604800 秒（7 天）
```

**用途**：
- 7 天内同一举报人不能举报同一目标
- 使用 SETNX 实现原子性

### 3.4 举报人信誉分缓存

```
Key: report:credit:{userId}
Type: STRING
Value: {
  "totalReports": 100,
  "rejectedCount": 15,
  "rejectRate": 0.15,
  "isRestricted": false
}
TTL: 86400 秒（24 小时）
```

**用途**：
- 恶意举报检测（驳回率 ≥ 80%）
- 自动暂停举报权限

### 3.5 扩展 RedisKeyConstants

在 `RedisKeyConstants.java` 中新增以下常量：

```java
// ==================== 举报与封禁功能 Key ====================

/**
 * 封禁状态缓存
 * Value: STRING (JSON)
 */
public static final String BAN_STATUS_KEY = "ban:status:%d";

/**
 * 黑名单 ZSET（member=userId, score=banEndTime）
 */
public static final String BAN_BLACKLIST_ZSET = "ban:blacklist";

/**
 * 举报限流 Key
 * 格式: rate_limit:report:{userId}
 */
public static final String REPORT_RATE_LIMIT_KEY = "rate_limit:report:%d";

/**
 * 防重复举报 Key
 * 格式: report:dup:{reporterId}:{targetType}:{targetId}
 */
public static final String REPORT_DUPLICATE_KEY = "report:dup:%d:%s:%d";

/**
 * 举报人信誉分缓存
 * 格式: report:credit:{userId}
 */
public static final String REPORT_CREDIT_KEY = "report:credit:%d";

// ==================== 举报与封禁 TTL ====================

/** 封禁状态缓存 TTL（秒）= 30 分钟 */
public static final int BAN_STATUS_TTL = 30 * 60;

/** 举报限流窗口（秒）= 1 分钟 */
public static final int REPORT_RATE_LIMIT_WINDOW = 60;

/** 举报限流最大次数 */
public static final int REPORT_RATE_LIMIT_MAX = 3;

/** 防重复举报 TTL（秒）= 7 天 */
public static final int REPORT_DUPLICATE_TTL = 7 * 24 * 3600;

/** 举报人信誉分 TTL（秒）= 24 小时 */
public static final int REPORT_CREDIT_TTL = 24 * 3600;

/** 黑名单定时任务扫描间隔（秒）= 5 分钟 */
public static final int BAN_SCHEDULE_INTERVAL = 5 * 60;
```

---

## 4. API 接口设计

### 4.1 用户端接口

#### 4.1.1 提交举报

```http
POST /api/report/submit
Content-Type: application/json
Authorization: Session（登录必需）

{
  "targetType": "PICTURE",           // 必填：PICTURE/USER
  "targetId": 1234567890,            // 必填：目标ID
  "reasonType": "PORNOGRAPHY",      // 必填：举报原因
  "description": "具体描述内容"       // 可选：补充描述
}
```

**返回**：

```json
{
  "code": 0,
  "message": "举报成功，感谢您的反馈",
  "data": {
    "reportId": 1234567890123456789
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 40001 | 参数错误（targetType/targetId/reasonType 缺失或无效） |
| 40002 | 举报频率超限（每分钟最多 3 次） |
| 40003 | 重复举报（7 天内已举报过该目标） |
| 40004 | 举报目标不存在 |
| 40005 | 不能举报自己 |
| 40006 | 举报权限已被暂停（恶意举报） |
| 40101 | 未登录 |

#### 4.1.2 查询我的举报列表

```http
GET /api/report/my-list?status=PENDING&current=1&pageSize=10
Authorization: Session（登录必需）
```

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 筛选状态：ALL/PENDING/PROCESSING/APPROVED/REJECTED，默认 ALL |
| current | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10，最大 50 |

**返回**：

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "reportId": 1234567890123456789,
        "targetType": "PICTURE",
        "targetId": 1234567890,
        "targetName": "图片名称",
        "targetPreview": "https://cdn.example.com/thumb.jpg",
        "reasonType": "PORNOGRAPHY",
        "reasonText": "色情内容",
        "description": "具体描述",
        "status": "PENDING",
        "statusText": "待审核",
        "createTime": "2024-06-05 10:30:00",
        "handleTime": null,
        "handleResult": null
      }
    ],
    "total": 100,
    "current": 1,
    "pageSize": 10
  }
}
```

#### 4.1.3 撤回举报

```http
POST /api/report/cancel
Content-Type: application/json
Authorization: Session（登录必需）

{
  "reportId": 1234567890123456789
}
```

**返回**：

```json
{
  "code": 0,
  "message": "撤回成功"
}
```

**限制**：
- 仅 10 分钟内且状态为 PENDING 的举报可撤回
- 管理员开始处理（状态=PROCESSING）后不可撤回

#### 4.1.4 查询举报详情

```http
GET /api/report/detail/{reportId}
Authorization: Session（登录必需）
```

**返回**：

```json
{
  "code": 0,
  "data": {
    "reportId": 1234567890123456789,
    "targetType": "PICTURE",
    "targetId": 1234567890,
    "targetName": "图片名称",
    "targetPreview": "https://cdn.example.com/thumb.jpg",
    "reasonType": "PORNOGRAPHY",
    "reasonText": "色情内容",
    "description": "具体描述",
    "status": "APPROVED",
    "statusText": "已处理",
    "createTime": "2024-06-05 10:30:00",
    "handleTime": "2024-06-05 11:00:00",
    "handleResult": "REMOVE_PICTURE",
    "handleResultText": "图片已下架",
    "handleReason": "经核实构成违规"
  }
}
```

### 4.2 管理端接口

#### 4.2.1 举报列表（按被举报次数排序）

```http
GET /api/admin/report/list?status=PENDING&targetType=PICTURE&reasonType=PORNOGRAPHY&minReportCount=3&current=1&pageSize=20
Authorization: Session（管理员必需）
```

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 筛选状态：ALL/PENDING/PROCESSING/APPROVED/REJECTED，默认 PENDING |
| targetType | String | 否 | 筛选类型：ALL/PICTURE/USER，默认 ALL |
| reasonType | String | 否 | 筛选原因：如 PORNOGRAPHY |
| minReportCount | Integer | 否 | 最小被举报次数：3/5/10 |
| current | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 20，最大 100 |

**返回**：

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "targetId": 1234567890,
        "targetType": "PICTURE",
        "targetName": "图片名称",
        "targetPreview": "https://cdn.example.com/thumb.jpg",
        "authorId": 111,
        "authorName": "图片作者",
        "reportCount": 5,
        "pendingCount": 3,
        "latestReasonType": "PORNOGRAPHY",
        "latestReasonText": "色情内容",
        "latestReportTime": "2024-06-05 10:30:00",
        "priority": "HIGH",
        "reports": [
          {
            "reportId": 1234567890123456789,
            "reporterId": 222,
            "reporterName": "举报人A",
            "reasonType": "PORNOGRAPHY",
            "description": "具体描述",
            "createTime": "2024-06-05 10:30:00"
          }
        ]
      }
    ],
    "total": 50,
    "current": 1,
    "pageSize": 20
  }
}
```

**优先级规则**：
- HIGH：reportCount ≥ 5
- MEDIUM：reportCount ≥ 3
- LOW：reportCount < 3

#### 4.2.2 举报详情

```http
GET /api/admin/report/detail/{reportId}
Authorization: Session（管理员必需）
```

**返回**：

```json
{
  "code": 0,
  "data": {
    "reportId": 1234567890123456789,
    "reporterId": 222,
    "reporterName": "举报人A",
    "reporterAvatar": "https://cdn.example.com/avatar.jpg",
    "targetType": "PICTURE",
    "targetId": 1234567890,
    "targetName": "图片名称",
    "targetPreview": "https://cdn.example.com/thumb.jpg",
    "targetFullUrl": "https://cdn.example.com/full.jpg",
    "authorId": 111,
    "authorName": "图片作者",
    "authorAvatar": "https://cdn.example.com/author-avatar.jpg",
    "authorProfile": "作者简介",
    "authorBanStatus": "NONE",
    "authorViolationCount": 2,
    "reasonType": "PORNOGRAPHY",
    "reasonText": "色情内容",
    "description": "具体描述",
    "status": "PENDING",
    "statusText": "待审核",
    "createTime": "2024-06-05 10:30:00",
    "relatedReports": [
      {
        "reportId": 1234567890123456790,
        "reporterName": "举报人B",
        "reasonType": "PORNOGRAPHY",
        "createTime": "2024-06-05 10:35:00",
        "status": "PENDING"
      }
    ],
    "banHistory": [
      {
        "banType": "TEMP",
        "banDuration": 3,
        "banReason": "发布违规内容",
        "banStartTime": "2024-05-01 10:00:00",
        "banEndTime": "2024-05-04 10:00:00",
        "operatorName": "管理员A"
      }
    ]
  }
}
```

#### 4.2.3 审核处理举报

```http
POST /api/admin/report/handle
Content-Type: application/json
Authorization: Session（管理员必需）

{
  "reportId": 1234567890123456789,
  "handleResult": "APPROVED",
  "handleReason": "经核实构成违规，执行封禁",
  "banAction": {
    "banType": "TEMP",
    "banDuration": 3,
    "banReason": "累计违规次数过多"
  },
  "takeDownPicture": true
}
```

**参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportId | Long | 是 | 举报ID |
| handleResult | String | 是 | APPROVED（通过）/REJECTED（驳回）/WARN_ONLY（警告）/REMOVE_ONLY（下架） |
| handleReason | String | 是 | 处理理由 |
| banAction | Object | handleResult=APPROVED 时必填 | 封禁动作 |
| banAction.banType | String | banAction 必填 | TEMP/PERMANENT |
| banAction.banDuration | Integer | banType=TEMP 时必填 | 3/7/30 |
| banAction.banReason | String | banAction 必填 | 封禁原因 |
| takeDownPicture | Boolean | targetType=PICTURE 时可选 | 是否下架图片 |

**返回**：

```json
{
  "code": 0,
  "message": "处理成功",
  "data": {
    "reportId": 1234567890123456789,
    "banRecordId": 9876543210123456789
  }
}
```

#### 4.2.4 批量处理举报

```http
POST /api/admin/report/batch-handle
Content-Type: application/json
Authorization: Session（管理员必需）

{
  "reportIds": [1234567890123456789, 1234567890123456790, 1234567890123456791],
  "handleResult": "REJECTED",
  "handleReason": "经核实不构成违规"
}
```

**返回**：

```json
{
  "code": 0,
  "message": "批量处理成功",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failedCount": 0,
    "failedItems": []
  }
}
```

#### 4.2.5 封禁记录列表

```http
GET /api/admin/ban/list?userId=111&status=ACTIVE&current=1&pageSize=20
Authorization: Session（管理员必需）
```

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 否 | 筛选用户ID |
| status | String | 否 | 筛选状态：ALL/ACTIVE/UNBANNED，默认 ALL |
| current | Integer | 否 | 当前页，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 20 |

**返回**：

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "banRecordId": 9876543210123456789,
        "userId": 111,
        "userName": "违规用户",
        "userAvatar": "https://cdn.example.com/avatar.jpg",
        "banType": "TEMP",
        "banTypeText": "临时封禁",
        "banDuration": 3,
        "banStartTime": "2024-06-05 10:00:00",
        "banEndTime": "2024-06-08 10:00:00",
        "banReason": "累计违规次数过多",
        "violationCount": 3,
        "relatedReportId": 1234567890123456789,
        "operatorName": "管理员A",
        "status": "ACTIVE",
        "statusText": "生效中",
        "createTime": "2024-06-05 10:00:00"
      }
    ],
    "total": 100,
    "current": 1,
    "pageSize": 20
  }
}
```

#### 4.2.6 手动解封

```http
POST /api/admin/ban/unban
Content-Type: application/json
Authorization: Session（管理员必需）

{
  "userId": 111,
  "unbanReason": "误封解封"
}
```

**返回**：

```json
{
  "code": 0,
  "message": "解封成功",
  "data": {
    "userId": 111,
    "banStatus": "NONE",
    "banEndTime": null
  }
}
```

---

## 5. 核心模块改造

### 5.1 User 实体扩展

在 `User.java` 中新增字段：

```java
@TableName(value ="user")
@Data
public class User implements Serializable {
    // ... 现有字段 ...
    
    /**
     * 用户角色：user/admin
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String userRole;
    
    // ==================== 新增封禁相关字段 ====================
    
    /**
     * 封禁状态：NONE/TEMP/PERMANENT
     */
    private String banStatus;
    
    /**
     * 封禁结束时间
     */
    private Date banEndTime;
    
    /**
     * 累计违规次数（6个月滚动窗口）
     */
    private Integer violationCount;
    
    /**
     * 最近一次违规时间
     */
    private Date lastViolationTime;
    
    // ... 其他现有字段 ...
}
```

### 5.2 NotificationTypeEnum 扩展

在 `NotificationTypeEnum.java` 中新增类型：

```java
@Getter
public enum NotificationTypeEnum {

    // ... 现有类型 ...
    LIKE("LIKE", "点赞"),
    FAVORITE("FAVORITE", "收藏"),
    COMMENT("COMMENT", "评论"),
    FOLLOW("FOLLOW", "关注"),
    SYSTEM("SYSTEM", "系统通知");
    
    // ==================== 新增举报相关通知类型 ====================
    
    /** 举报受理通知（发给举报人） */
    REPORT_ACCEPTED("REPORT_ACCEPTED", "举报已受理"),
    
    /** 举报驳回通知（发给举报人） */
    REPORT_REJECTED("REPORT_REJECTED", "举报已驳回"),
    
    /** 用户被警告通知（发给被举报人） */
    USER_WARNED("USER_WARNED", "违规警告"),
    
    /** 用户被封禁通知（发给被封禁用户） */
    USER_BANNED("USER_BANNED", "账号封禁"),
    
    /** 用户解封通知（发给被解封用户） */
    USER_UNBANNED("USER_UNBANNED", "账号解封"),
    
    /** 图片下架通知（发给图片作者） */
    PICTURE_REMOVED("PICTURE_REMOVED", "图片下架"),
    
    /** 图片恢复通知（发给图片作者） */
    PICTURE_RESTORED("PICTURE_RESTORED", "图片恢复"),
    
    /** 封禁到期提醒（提前1天） */
    BAN_EXPIRING("BAN_EXPIRING", "封禁即将到期"),
    
    /** 举报权限暂停通知（发给恶意举报人） */
    REPORT_RESTRICTED("REPORT_RESTRICTED", "举报权限暂停");
    
    // ... 其他现有代码 ...
}
```

### 5.3 LoginInterceptor 改造

在 `LoginInterceptor.java` 中新增封禁检查逻辑：

```java
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){

        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if(ObjUtil.hasEmpty(session, session.getAttribute(UserConstant.USER_LOGIN_STATE))){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        User user = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        
        // ==================== 新增：封禁检查 ====================
        checkBanStatus(user);
        
        UserContext.set(user);

        return true;
    }
    
    /**
     * 检查用户封禁状态
     * 
     * 检查顺序：
     * 1. Redis 缓存（快速路径）
     * 2. 数据库（缓存未命中）
     * 3. 降级处理（Redis 不可用）
     */
    private void checkBanStatus(User user) {
        try {
            // 1. 先查 Redis 缓存
            String cacheKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, user.getId());
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                // 缓存命中，解析 JSON 检查状态
                BanStatusCache banCache = JSON.parseObject(cached.toString(), BanStatusCache.class);
                if (isBanned(banCache.getBanStatus())) {
                    throwBanException(banCache);
                }
                // 检查临时封禁是否过期
                if ("TEMP".equals(banCache.getBanStatus()) && banCache.getBanEndTime() != null) {
                    if (System.currentTimeMillis() > banCache.getBanEndTime()) {
                        // 已过期，抛出异常提示重新登录（由定时任务或下次登录处理）
                        throw new BusinessException(ErrorCode.USER_BANNED_EXPIRED, "封禁已过期，请重新登录");
                    }
                }
                return;
            }
            
            // 2. 缓存未命中，查询数据库
            User dbUser = userMapper.selectById(user.getId());
            if (dbUser == null || dbUser.getBanStatus() == null) {
                return; // 用户不存在或无封禁记录，放行
            }
            
            // 检查是否被封禁
            if (isBanned(dbUser.getBanStatus())) {
                // 回写缓存
                cacheBanStatus(dbUser);
                
                // 检查临时封禁是否过期
                if ("TEMP".equals(dbUser.getBanStatus()) && dbUser.getBanEndTime() != null) {
                    if (new Date().after(dbUser.getBanEndTime())) {
                        throw new BusinessException(ErrorCode.USER_BANNED_EXPIRED, "封禁已过期，请重新登录");
                    }
                }
                
                // 抛出封禁异常
                throwBanExceptionFromUser(dbUser);
            }
            
        } catch (RedisConnectionException e) {
            // 3. Redis 降级：直接查数据库
            log.error("Redis连接异常，降级到数据库查询封禁状态: userId={}", user.getId(), e);
            checkBanStatusFromDB(user);
        }
    }
    
    /**
     * 降级方案：直接查询数据库
     */
    private void checkBanStatusFromDB(User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser != null && isBanned(dbUser.getBanStatus())) {
            if ("TEMP".equals(dbUser.getBanStatus()) && dbUser.getBanEndTime() != null) {
                if (new Date().after(dbUser.getBanEndTime())) {
                    throw new BusinessException(ErrorCode.USER_BANNED_EXPIRED, "封禁已过期，请重新登录");
                }
            }
            throwBanExceptionFromUser(dbUser);
        }
    }
    
    /**
     * 回写封禁状态缓存
     */
    private void cacheBanStatus(User user) {
        String cacheKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, user.getId());
        BanStatusCache cache = new BanStatusCache();
        cache.setBanStatus(user.getBanStatus());
        cache.setBanEndTime(user.getBanEndTime() != null ? user.getBanEndTime().getTime() : null);
        cache.setBanReason("封禁中");
        
        redisTemplate.opsForValue().set(
            cacheKey,
            JSON.toJSONString(cache),
            RedisKeyConstants.BAN_STATUS_TTL,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * 判断是否被封禁
     */
    private boolean isBanned(String banStatus) {
        return banStatus != null && !"NONE".equals(banStatus);
    }
    
    /**
     * 抛出封禁异常（从缓存）
     */
    private void throwBanException(BanStatusCache cache) {
        if ("PERMANENT".equals(cache.getBanStatus())) {
            throw new BusinessException(ErrorCode.USER_BANNED_PERMANENT, "账号已被永久封禁");
        }
        throw new BusinessException(ErrorCode.USER_BANNED, 
            String.format("账号已被封禁，解封时间：%s", 
                cache.getBanEndTime() != null ? new Date(cache.getBanEndTime()) : "永久"));
    }
    
    /**
     * 抛出封禁异常（从数据库）
     */
    private void throwBanExceptionFromUser(User user) {
        if ("PERMANENT".equals(user.getBanStatus())) {
            throw new BusinessException(ErrorCode.USER_BANNED_PERMANENT, "账号已被永久封禁");
        }
        throw new BusinessException(ErrorCode.USER_BANNED, 
            String.format("账号已被封禁，解封时间：%s", user.getBanEndTime()));
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
```

**BanStatusCache DTO**：

```java
@Data
public class BanStatusCache {
    private String banStatus;
    private Long banEndTime;
    private String banReason;
}
```

### 5.4 新增 ErrorCode

在 `ErrorCode.java` 中新增错误码：

```java
// 举报相关
REPORT_RATE_LIMIT_EXCEEDED(40002, "举报频率超限，请稍后再试"),
REPORT_DUPLICATE(40003, "请勿重复举报"),
REPORT_TARGET_NOT_FOUND(40004, "举报目标不存在"),
REPORT_SELF(40005, "不能举报自己"),
REPORT_RESTRICTED(40006, "您的举报权限已被暂停，原因：恶意举报"),
REPORT_CANCEL_TIMEOUT(40007, "举报已超过撤回时间"),

// 封禁相关
USER_BANNED(40102, "账号已被封禁"),
USER_BANNED_PERMANENT(40103, "账号已被永久封禁"),
USER_BANNED_EXPIRED(40104, "封禁已过期，请重新登录"),
```

---

## 6. 代码结构规划

### 6.1 新增类清单

| 模块 | 包路径 | 类名 | 说明 |
|------|--------|------|------|
| Entity | `pojo/entity` | Report.java | 举报实体 |
| Entity | `pojo/entity` | BanRecord.java | 封禁记录实体 |
| Enum | `common/enums` | ReportReasonEnum.java | 举报原因枚举 |
| Enum | `common/enums` | ReportStatusEnum.java | 举报状态枚举 |
| Enum | `common/enums` | BanTypeEnum.java | 封禁类型枚举 |
| Enum | `common/enums` | HandleResultEnum.java | 处理结果枚举 |
| DTO | `pojo/dto/report` | ReportSubmitDTO.java | 提交举报请求 |
| DTO | `pojo/dto/report` | ReportQueryDTO.java | 举报查询请求 |
| DTO | `pojo/dto/report` | ReportHandleDTO.java | 处理举报请求 |
| DTO | `pojo/dto/report` | BanActionDTO.java | 封禁动作 |
| DTO | `pojo/dto/report` | BanUnbanDTO.java | 解封请求 |
| DTO | `pojo/dto/report` | BanStatusCache.java | 封禁状态缓存 |
| VO | `pojo/vo/report` | ReportVO.java | 举报详情 VO |
| VO | `pojo/vo/report` | ReportTargetVO.java | 被举报目标 VO |
| VO | `pojo/vo/report` | BanRecordVO.java | 封禁记录 VO |
| VO | `pojo/vo/report` | ReportListItemVO.java | 举报列表项 VO |
| Mapper | `server/mapper` | ReportMapper.java | 举报 Mapper |
| Mapper | `server/mapper` | BanRecordMapper.java | 封禁记录 Mapper |
| Service | `server/service` | ReportService.java | 举报服务接口 |
| Service | `server/service` | BanService.java | 封禁服务接口 |
| ServiceImpl | `server/service/impl` | CachedReportServiceImpl.java | 举报服务实现（含缓存） |
| ServiceImpl | `server/service/impl` | DBReportServiceImpl.java | 举报服务 DB 实现 |
| ServiceImpl | `server/service/impl` | BanServiceImpl.java | 封禁服务实现 |
| Controller | `server/controller` | ReportController.java | 用户端举报接口 |
| Controller | `server/controller/admin` | AdminReportController.java | 管理端举报接口 |
| Controller | `server/controller/admin` | AdminBanController.java | 管理端封禁接口 |
| Scheduled | `server/scheduled` | BanScheduled.java | 封禁定时任务 |

### 6.2 修改现有类清单

| 类名 | 路径 | 改造点 |
|------|------|--------|
| User.java | pojo/entity | 新增 4 个字段 |
| NotificationTypeEnum.java | common/enums | 新增 10 个通知类型 |
| RedisKeyConstants.java | common/constants | 新增举报与封禁相关 Key 和 TTL |
| LoginInterceptor.java | server/Interceptor | 新增封禁检查逻辑 |
| ErrorCode.java | common/exception | 新增 8 个错误码 |
| InterceptorConfig.java | server/config | 新增举报接口放行（可选） |

### 6.3 包结构图

```
pojo/
├── entity/
│   ├── User.java（修改）
│   ├── Report.java（新增）
│   └── BanRecord.java（新增）
├── dto/
│   └── report/（新增目录）
│       ├── ReportSubmitDTO.java
│       ├── ReportQueryDTO.java
│       ├── ReportHandleDTO.java
│       ├── BanActionDTO.java
│       ├── BanUnbanDTO.java
│       └── BanStatusCache.java
└── vo/
    └── report/（新增目录）
        ├── ReportVO.java
        ├── ReportTargetVO.java
        ├── BanRecordVO.java
        └── ReportListItemVO.java

common/
├── enums/
│   ├── ReportReasonEnum.java（新增）
│   ├── ReportStatusEnum.java（新增）
│   ├── BanTypeEnum.java（新增）
│   ├── HandleResultEnum.java（新增）
│   └── NotificationTypeEnum.java（修改）
├── constants/
│   └── RedisKeyConstants.java（修改）
└── exception/
    └── ErrorCode.java（修改）

server/
├── mapper/
│   ├── ReportMapper.java（新增）
│   └── BanRecordMapper.java（新增）
├── service/
│   ├── ReportService.java（新增）
│   └── BanService.java（新增）
├── service/impl/
│   ├── CachedReportServiceImpl.java（新增）
│   ├── DBReportServiceImpl.java（新增）
│   └── BanServiceImpl.java（新增）
├── controller/
│   ├── ReportController.java（新增）
│   └── admin/
│       ├── AdminReportController.java（新增）
│       └── AdminBanController.java（新增）
├── Interceptor/
│   └── LoginInterceptor.java（修改）
├── config/
│   └── InterceptorConfig.java（修改）
└── scheduled/
    └── BanScheduled.java（新增）
```

---

## 7. 定时任务设计

### 7.1 封禁到期自动解封

```java
@Component
@Slf4j
public class BanScheduled {

    @Resource
    private BanService banService;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 5 分钟扫描一次即将过期和已过期的封禁
     */
    @Scheduled(fixedDelay = RedisKeyConstants.BAN_SCHEDULE_INTERVAL * 1000)
    public void autoUnbanExpiredUsers() {
        try {
            // 1. 从 Redis ZSET 中扫描已过期的 userId
            long now = System.currentTimeMillis();
            Set<Object> expiredUsers = redisTemplate.opsForZSet()
                .rangeByScore(RedisKeyConstants.BAN_BLACKLIST_ZSET, 0, now);
            
            if (CollectionUtil.isEmpty(expiredUsers)) {
                return;
            }
            
            log.info("定时任务：扫描到 {} 个过期封禁用户", expiredUsers.size());
            
            // 2. 批量解封
            for (Object userIdObj : expiredUsers) {
                Long userId = Long.valueOf(userIdObj.toString());
                try {
                    banService.unbanUser(userId, "系统自动解封：封禁到期", null);
                    
                    // 从 ZSET 中移除
                    redisTemplate.opsForZSet().remove(
                        RedisKeyConstants.BAN_BLACKLIST_ZSET, 
                        userId
                    );
                    
                    // 清除用户封禁状态缓存
                    String cacheKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
                    redisTemplate.delete(cacheKey);
                    
                } catch (Exception e) {
                    log.error("自动解封失败: userId={}", userId, e);
                }
            }
            
            // 3. 从 ZSET 中移除已解封的用户
            redisTemplate.opsForZSet().removeRangeByScore(
                RedisKeyConstants.BAN_BLACKLIST_ZSET, 
                0, 
                now
            );
            
        } catch (Exception e) {
            log.error("封禁解封定时任务异常", e);
        }
    }
    
    /**
     * 每天凌晨 1 点发送封禁即将到期通知（提前 1 天）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void notifyBanExpiringSoon() {
        try {
            // 查询 24 小时内到期的临时封禁用户
            LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
            LocalDateTime startOfTomorrow = tomorrow.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfTomorrow = startOfTomorrow.plusDays(1);
            
            List<BanRecord> expiringRecords = banRecordMapper.selectList(
                new QueryWrapper<BanRecord>()
                    .eq("unbanned", 0)
                    .eq("ban_type", "TEMP")
                    .between("ban_end_time", startOfTomorrow, endOfTomorrow)
            );
            
            log.info("定时任务：扫描到 {} 个即将到期封禁用户", expiringRecords.size());
            
            // 发送通知
            for (BanRecord record : expiringRecords) {
                try {
                    notificationService.sendBanExpiringNotification(
                        record.getUserId(), 
                        record.getBanEndTime()
                    );
                } catch (Exception e) {
                    log.error("发送封禁到期通知失败: userId={}", record.getUserId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("封禁到期提醒定时任务异常", e);
        }
    }
    
    /**
     * 每天凌晨 2 点重置超过 6 个月的违规计数
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void resetExpiredViolationCount() {
        try {
            // 重置 6 个月前的违规计数
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            
            List<User> users = userMapper.selectList(
                new QueryWrapper<User>()
                    .gt("violation_count", 0)
                    .lt("last_violation_time", sixMonthsAgo)
            );
            
            log.info("定时任务：重置 {} 个用户的违规计数", users.size());
            
            for (User user : users) {
                try {
                    user.setViolationCount(0);
                    user.setLastViolationTime(null);
                    userMapper.updateById(user);
                } catch (Exception e) {
                    log.error("重置违规计数失败: userId={}", user.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("重置违规计数定时任务异常", e);
        }
    }
}
```

### 7.2 定时任务时间表

| 任务 | 执行时间 | 说明 |
|------|---------|------|
| 封禁解封扫描 | 每 5 分钟 | 扫描并解封到期的封禁 |
| 封禁到期提醒 | 每天凌晨 1 点 | 发送即将到期通知 |
| 违规计数重置 | 每天凌晨 2 点 | 重置 6 个月前的违规计数 |

---

## 8. 性能与安全

### 8.1 性能保证

#### 8.1.1 登录拦截性能目标

**目标**：封禁检查响应时间 < 100ms

**实现策略**：

1. **Redis 缓存优先**：98% 请求命中缓存（O(1) 查询）
2. **缓存 TTL 30 分钟**：平衡实时性与性能
3. **Redis 降级**：不可用时直接查数据库（保证可用性）
4. **异步回写**：封禁操作先写 Redis，再同步到 DB

#### 8.1.2 举报列表查询性能

**目标**：管理员列表加载时间 < 1s

**实现策略**：

1. **复合索引优化**：`idx_status_count` (status, report_count DESC, create_time)
2. **分页限制**：最大每页 100 条
3. **冗余字段**：report_count 避免子查询
4. **延迟加载**：relatedReports 仅在详情页加载

#### 8.1.3 举报提交性能

**目标**：举报提交响应时间 < 500ms

**实现策略**：

1. **异步通知**：处理结果通过 RabbitMQ 异步发送
2. **限流 Redis 原子操作**：Lua 脚本保证原子性
3. **防重复 SETNX**：O(1) 操作

### 8.2 安全防护

#### 8.2.1 恶意举报防护

**检测机制**：

1. **驳回率监控**：单用户 10 次举报中 8 次被驳回（驳回率 ≥ 80%）
2. **批量举报检测**：短时间（10 分钟）内举报超过 10 次
3. **信誉分系统**：累计驳回次数影响举报权限

**惩罚措施**：

| 违规次数 | 处罚 |
|---------|------|
| 第 1 次 | 警告通知 |
| 第 2 次 | 暂停举报权限 7 天 |
| 第 3 次 | 暂停举报权限 30 天 |

**实现代码**：

```java
public void checkReportPermission(Long userId) {
    String creditKey = String.format(RedisKeyConstants.REPORT_CREDIT_KEY, userId);
    Object cached = redisTemplate.opsForValue().get(creditKey);
    
    if (cached != null) {
        ReportCredit credit = JSON.parseObject(cached.toString(), ReportCredit.class);
        if (credit.getIsRestricted()) {
            throw new BusinessException(ErrorCode.REPORT_RESTRICTED, 
                "您的举报权限已被暂停，原因：恶意举报");
        }
    }
}
```

#### 8.2.2 举报频率限制

**滑动窗口限流**：

```lua
-- Redis Lua 脚本实现滑动窗口限流
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- 删除窗口外的记录
redis.call('zremrangebyscore', key, 0, now - window * 1000)

-- 获取当前窗口内计数
local count = redis.call('zcard', key)

if count < limit then
    -- 添加当前记录
    redis.call('zadd', key, now, now)
    redis.call('expire', key, window)
    return 1
else
    return 0
end
```

#### 8.2.3 Redis 降级方案

**降级策略**：

1. **Redis 不可用时**：LoginInterceptor 捕获异常，降级到数据库查询
2. **本地缓存兜底**：使用 Caffeine 作为二级缓存，TTL 1 分钟
3. **告警机制**：Redis 连接失败时发送告警

**降级代码**：

```java
private void checkBanStatus(User user) {
    try {
        // Redis 缓存逻辑
    } catch (RedisConnectionException e) {
        log.error("Redis连接异常，降级到数据库查询: userId={}", user.getId(), e);
        checkBanStatusFromDB(user); // 降级到数据库
    }
}
```

### 8.3 数据安全

#### 8.3.1 封禁记录保留

- 封禁记录不可删除，只能标记为已解封
- 举报信息至少保留 2 年（合规要求）
- 使用软删除（is_delete）避免数据丢失

#### 8.3.2 管理员操作审计

- 所有封禁操作记录 operator_id 和 operator_name
- 操作日志单独存储（可扩展为 audit_log 表）
- 敏感操作需二次确认

---

## 9. 集成方案

### 9.1 与 Picture 审核集成

**触发时机**：管理员审核举报通过时（handleResult=APPROVED）

**集成逻辑**：

```java
public void handleReport(ReportHandleDTO dto) {
    // ... 其他处理逻辑 ...
    
    if (dto.getTakeDownPicture() != null && dto.getTakeDownPicture()) {
        // 下架图片
        Picture picture = pictureMapper.selectById(targetId);
        if (picture != null) {
            picture.setReviewStatus(2); // 拒绝
            picture.setReviewMessage(dto.getHandleReason());
            pictureMapper.updateById(picture);
            
            // 发送图片下架通知
            notificationService.sendPictureRemovedNotification(
                picture.getUserId(),
                picture.getId(),
                dto.getHandleReason()
            );
        }
    }
}
```

### 9.2 与 Notification 集成

**通知场景矩阵**：

| 场景 | 通知类型 | 接收者 | 触发时机 |
|------|---------|--------|----------|
| 举报受理 | REPORT_ACCEPTED | 举报人 | 管理员开始处理举报 |
| 举报驳回 | REPORT_REJECTED | 举报人 | 管理员驳回举报 |
| 用户警告 | USER_WARNED | 被举报人 | 管理员选择警告处理 |
| 用户封禁 | USER_BANNED | 被封禁用户 | 管理员执行封禁 |
| 用户解封 | USER_UNBANNED | 被解封用户 | 封禁到期或手动解封 |
| 图片下架 | PICTURE_REMOVED | 图片作者 | 管理员下架图片 |
| 图片恢复 | PICTURE_RESTORED | 图片作者 | 管理员恢复图片 |
| 封禁到期提醒 | BAN_EXPIRING | 封禁用户 | 定时任务（提前1天） |

**通知内容模板**：

```java
public String getNotificationContent(NotificationTypeEnum type, Object... args) {
    switch (type) {
        case USER_BANNED:
            return String.format("您的账号因%s被封禁 %d 天。封禁原因：%s，到期时间：%s", 
                args[0], args[1], args[2], args[3]);
        case USER_UNBANNED:
            return String.format("您的账号已解封，感谢您的理解，请遵守社区规范。");
        case PICTURE_REMOVED:
            return String.format("您的图片《%s》因违规被下架，原因：%s", args[0], args[1]);
        // ... 其他场景 ...
    }
}
```

### 9.3 与 Session 集成

**封禁后强制登出**：

```java
public void banUser(Long userId, BanActionDTO action, Long operatorId) {
    // 1. 执行封禁
    // ... 封禁逻辑 ...
    
    // 2. 清除用户所有 Session
    clearUserSessions(userId);
    
    // 3. 清除 Redis 缓存
    String cacheKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
    redisTemplate.delete(cacheKey);
    
    // 4. 发送通知
    notificationService.sendUserBannedNotification(userId, action);
}

/**
 * 清除用户所有 Session
 */
private void clearUserSessions(Long userId) {
    // 通过 Spring Session Redis 查找并删除用户的所有 Session
    String sessionKey = "spring:session:sessions:" + userId;
    // 实际实现依赖 Spring Session 配置
}
```

---

## 10. 实施计划

### 10.1 开发优先级

| 优先级 | 功能 | 工作量（人日） | 说明 |
|--------|------|---------------|------|
| P0（核心） | 数据库表创建 | 0.5 | DDL 执行 |
| P0 | 基础举报提交 | 2 | 含限流、防重复 |
| P0 | 管理员审核 | 3 | 含列表、详情、处理 |
| P0 | 封禁功能 | 2 | 含执行、查询、手动解封 |
| P1（重要） | Redis 缓存 | 1.5 | 黑名单、状态缓存 |
| P1 | 登录拦截改造 | 1 | 封禁检查、降级 |
| P1 | 定时解封任务 | 1 | 自动解封、到期提醒 |
| P2（优化） | 通知集成 | 1.5 | 10 种通知类型 |
| P2 | 恶意举报防护 | 1 | 信誉分、暂停权限 |
| P2 | 举报撤回 | 0.5 | 10 分钟限制 |
| P3（增强） | 批量处理 | 1 | 批量审核 |
| P3 | 统计报表 | 1 | 举报数据统计 |
| P3 | 前端联调 | 2 | 接口联调 |
| P3 | 测试 | 3 | 单元测试 + 集成测试 |

**总计**：约 20 人日（1 人 4 周，或 2 人 2 周）

### 10.2 里程碑

| 周次 | 里程碑 | 交付物 |
|------|--------|--------|
| 第 1 周 | 核心功能完成 | 举报提交、管理员审核、封禁功能 |
| 第 2 周 | 缓存与拦截 | Redis 缓存、登录拦截、定时任务 |
| 第 3 周 | 优化与集成 | 通知集成、恶意举报防护 |
| 第 4 周 | 测试与上线 | 单元测试、集成测试、上线 |

### 10.3 上线检查清单

**上线前**：

- [ ] 数据库表创建完成
- [ ] 所有接口单元测试通过
- [ ] Redis 缓存验证正常
- [ ] 定时任务验证正常
- [ ] 登录拦截降级测试通过
- [ ] 通知发送验证正常
- [ ] 性能测试达标（登录 < 100ms，举报提交 < 500ms）

**上线后**：

- [ ] 监控 Redis 连接状态
- [ ] 监控定时任务执行情况
- [ ] 监控举报处理时长
- [ ] 收集用户反馈

---

## 附录

### A. 枚举定义

#### A.1 举报原因枚举（ReportReasonEnum）

```java
@Getter
public enum ReportReasonEnum {
    
    PORNOGRAPHY("PORNOGRAPHY", "色情内容", "HIGH"),
    VIOLENCE("VIOLENCE", "暴力内容", "HIGH"),
    SCAM("SCAM", "诈骗信息", "HIGH"),
    COPYRIGHT("COPYRIGHT", "侵权内容", "MEDIUM"),
    SPAM("SPAM", "垃圾信息", "MEDIUM"),
    HARASSMENT("HARASSMENT", "恶意行为", "MEDIUM"),
    OTHER("OTHER", "其他", "LOW");
    
    private final String code;
    private final String desc;
    private final String severity;
}
```

#### A.2 举报状态枚举（ReportStatusEnum）

```java
@Getter
public enum ReportStatusEnum {
    
    PENDING("PENDING", "待审核"),
    PROCESSING("PROCESSING", "处理中"),
    APPROVED("APPROVED", "已处理"),
    REJECTED("REJECTED", "已驳回"),
    WARN_ONLY("WARN_ONLY", "仅警告"),
    REMOVE_ONLY("REMOVE_ONLY", "仅下架");
    
    private final String code;
    private final String desc;
}
```

#### A.3 封禁类型枚举（BanTypeEnum）

```java
@Getter
public enum BanTypeEnum {
    
    NONE("NONE", "正常"),
    TEMP("TEMP", "临时封禁"),
    PERMANENT("PERMANENT", "永久封禁");
    
    private final String code;
    private final String desc;
}
```

#### A.4 处理结果枚举（HandleResultEnum）

```java
@Getter
public enum HandleResultEnum {
    
    REJECT("REJECT", "驳回"),
    WARN("WARN", "警告"),
    BAN_TEMP_3("BAN_TEMP_3", "封禁3天"),
    BAN_TEMP_7("BAN_TEMP_7", "封禁7天"),
    BAN_TEMP_30("BAN_TEMP_30", "封禁30天"),
    BAN_PERMANENT("BAN_PERMANENT", "永久封禁"),
    REMOVE_PICTURE("REMOVE_PICTURE", "下架图片"),
    RESTORE_PICTURE("RESTORE_PICTURE", "恢复图片");
    
    private final String code;
    private final String desc;
}
```

### B. 数据库 Migration 脚本

```sql
-- ========================================
-- 举报与封禁模块 Migration 脚本
-- 版本: 1.0
-- 日期: 2026-06-05
-- ========================================

-- 1. 创建举报表
CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL COMMENT '举报ID（雪花算法）',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '举报对象类型：PICTURE/USER',
  `target_id` BIGINT NOT NULL COMMENT '举报目标ID',
  `reason_type` VARCHAR(20) NOT NULL COMMENT '举报原因',
  `description` TEXT COMMENT '补充描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `handler_id` BIGINT COMMENT '处理人ID',
  `handle_result` VARCHAR(50) COMMENT '处理结果',
  `handle_reason` TEXT COMMENT '处理理由',
  `handle_time` DATETIME COMMENT '处理时间',
  `report_count` INT NOT NULL DEFAULT 0 COMMENT '被举报次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_status_count` (`status`, `report_count` DESC, `create_time`),
  KEY `idx_reporter_dup` (`reporter_id`, `target_type`, `target_id`),
  KEY `idx_target_count` (`target_type`, `target_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 2. 创建封禁记录表
CREATE TABLE IF NOT EXISTS `ban_record` (
  `id` BIGINT NOT NULL COMMENT '封禁记录ID',
  `user_id` BIGINT NOT NULL COMMENT '被封禁用户ID',
  `ban_type` VARCHAR(20) NOT NULL COMMENT '封禁类型',
  `ban_duration` INT COMMENT '封禁天数',
  `ban_start_time` DATETIME NOT NULL COMMENT '封禁开始时间',
  `ban_end_time` DATETIME COMMENT '封禁结束时间',
  `ban_reason` VARCHAR(200) NOT NULL COMMENT '封禁原因',
  `violation_count` INT NOT NULL DEFAULT 0 COMMENT '违规次数',
  `unbanned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封',
  `unban_time` DATETIME COMMENT '解封时间',
  `unban_reason` TEXT COMMENT '解封理由',
  `unban_operator_id` BIGINT COMMENT '解封操作人ID',
  `ban_operator_id` BIGINT NOT NULL COMMENT '封禁操作人ID',
  `ban_operator_name` VARCHAR(100) NOT NULL COMMENT '操作人姓名',
  `report_id` BIGINT COMMENT '关联举报ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `ban_start_time` DESC),
  KEY `idx_unbanned_end` (`unbanned`, `ban_end_time`),
  KEY `idx_ban_end` (`ban_end_time`, `unbanned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封禁记录表';

-- 3. 扩展 User 表
ALTER TABLE `user` 
ADD COLUMN IF NOT EXISTS `ban_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '封禁状态' AFTER `user_role`,
ADD COLUMN IF NOT EXISTS `ban_end_time` DATETIME DEFAULT NULL COMMENT '封禁结束时间' AFTER `ban_status`,
ADD COLUMN IF NOT EXISTS `violation_count` INT NOT NULL DEFAULT 0 COMMENT '违规次数' AFTER `ban_end_time`,
ADD COLUMN IF NOT EXISTS `last_violation_time` DATETIME DEFAULT NULL COMMENT '最近违规时间' AFTER `violation_count`,
ADD INDEX IF NOT EXISTS `idx_ban_status` (`ban_status`, `ban_end_time`);

-- 4. 插入测试数据（可选）
-- INSERT INTO `report` ...
-- INSERT INTO `ban_record` ...
```

---

**文档版本**: 1.0  
**最后更新**: 2026-06-05  
**作者**: 后端架构师  
