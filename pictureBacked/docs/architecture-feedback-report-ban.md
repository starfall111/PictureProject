# 反馈-举报-封禁 统一架构设计文档

**版本**: 1.0
**创建日期**: 2026-06-05
**基于**: feedback-design.md + technical-design-report-ban-mechanism.md

---

## 一、模块职责划分与边界

### 1.1 三模块定位

三个模块解决不同维度的问题，通过事件驱动松耦合协作：

```
┌──────────────────────────────────────────────────────────────────────┐
│                        用户端 / 管理端                                │
└──────┬──────────────────────┬──────────────────────┬────────────────┘
       │                      │                      │
       ▼                      ▼                      ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Feedback   │    │     Report       │    │      Ban         │
│   反馈模块   │    │   举报模块       │    │   封禁模块       │
│              │    │                  │    │                  │
│ Bug 报告    │    │ 图片违规举报    │    │ 临时封禁        │
│ 功能建议    │    │ 用户行为举报    │    │ 永久封禁        │
│ 体验反馈    │    │ 防重复/限流    │    │ 自动解封        │
│ 账号问题    │    │ 举报人信誉分   │    │ 手动解封        │
│ 其他        │    │ 批量审核       │    │ 违规计数        │
└──────┬───────┘    └───────┬──────────┘    └───────┬──────────┘
       │                    │                       │
       │                    │  审核通过时触发        │ 封禁/解封时触发
       │                    ▼                       ▼
       │            ┌──────────────────────────────────┐
       │            │        Notification 通知模块      │
       │            │  (现有基础设施，扩展通知类型)      │
       │            └──────────────────────────────────┘
       │
       │  反馈内容中如涉及违规
       │  → 管理员可一键转举报
       ▼
┌──────────────────┐
│   Report 模块    │
│  (转为举报工单)  │
└──────────────────┘
```

### 1.2 职责矩阵

| 维度 | Feedback 反馈 | Report 举报 | Ban 封禁 |
|------|-------------|------------|---------|
| **核心目的** | 收集用户声音，改进产品 | 处理内容/行为违规 | 惩罚违规用户 |
| **发起方** | 普通用户 | 普通用户 | 管理员（或系统自动） |
| **处理方** | 客服/运营 | 审核/运营 | 运营/系统 |
| **目标对象** | 平台本身（产品/服务） | 图片/用户 | 用户 |
| **结果** | 回复、改进、关闭 | 下架、封禁、驳回 | 禁止登录/操作 |
| **与另两模块关系** | 可转举报 | 审核通过触发封禁 | 封禁后拦截登录 |

### 1.3 重叠点处理

原 `feedback-design.md` 中 `type=COMPLAINT` 和 `source=PICTURE_REPORT/USER_REPORT` 与 Report 模块重叠。统一方案：

| 原反馈类型 | 新归属 | 处理方式 |
|-----------|--------|---------|
| `BUG` | Feedback | 保留 |
| `FEATURE` | Feedback | 保留 |
| `COMPLAINT` | **迁移至 Report** | 删除此类型，前端直接走举报流程 |
| `ACCOUNT` | Feedback | 保留 |
| `EXPERIENCE` | Feedback | 保留 |
| `CONTENT` | **迁移至 Report** | 删除此类型，前端直接走举报流程 |
| `OTHER` | Feedback | 保留 |
| `PICTURE_REPORT` source | **Report 独占** | 不再出现在 Feedback |
| `USER_REPORT` source | **Report 独占** | 不再出现在 Feedback |

**新增跨模块联动**：管理员查看反馈详情时，可一键将反馈「转为举报」，系统自动在 Report 表创建记录并关联原 Feedback ID。

---

## 二、数据库设计

### 2.1 ER 关系图

```
┌────────────┐       ┌──────────────────┐       ┌──────────────┐
│   user     │       │    feedback      │       │    report    │
│            │       │                  │       │              │
│ id ◄───┐   │       │ userId ──────┐   │       │ reporterId ─┤──► user.id
│ banStatus │  │       │ relatedPictureId │       │ targetId    │
│ banEndTime│  │       │ relatedUserId    │       │ target_type │
│ violation │  │       └──────┬───────────┘       └──────┬───────┘
└────────────┘  │              │                          │
                │              │ feedback_reply            │
                │              ▼                          │
                │       ┌──────────────────┐              │
                │       │ feedback_reply   │              │ ban_record
                │       │ feedbackId ──────┤              │
                │       └──────────────────┘              ▼
                │       ┌──────────────────┐       ┌──────────────┐
                │       │feedback_status_log│       │  ban_record  │
                │       │ feedbackId       │       │              │
                │       └──────────────────┘       │ userId ──────┤──► user.id
                │                                   │ reportId ────┤──► report.id
                └──────────────────────────────────►│              │
                                                    └──────────────┘
```

### 2.2 feedback 表（反馈主表）

```sql
CREATE TABLE `feedback` (
  `id` BIGINT NOT NULL COMMENT '反馈ID（雪花算法）',
  `userId` BIGINT DEFAULT NULL COMMENT '提交者用户ID（匿名反馈为NULL）',
  `title` VARCHAR(100) NOT NULL COMMENT '反馈标题',
  `content` TEXT NOT NULL COMMENT '内容描述',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：BUG/FEATURE/ACCOUNT/EXPERIENCE/OTHER',
  `priority` VARCHAR(5) NOT NULL DEFAULT 'P3' COMMENT '优先级：P0/P1/P2/P3',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/REJECTED/REOPENED/CLOSED',
  `source` VARCHAR(20) NOT NULL DEFAULT 'APP' COMMENT '来源：APP/SYSTEM/ADMIN',
  `relatedPictureId` BIGINT DEFAULT NULL COMMENT '关联图片ID（可选）',
  `relatedUserId` BIGINT DEFAULT NULL COMMENT '关联用户ID（可选）',
  `handlerId` BIGINT DEFAULT NULL COMMENT '当前处理人管理员ID',
  `isAnonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名提交',
  `closeReason` VARCHAR(50) DEFAULT NULL COMMENT '关闭原因：USER_WITHDRAW/AUTO_CLOSE/ADMIN_CLOSE/USER_CONFIRM',
  `reopenCount` INT NOT NULL DEFAULT 0 COMMENT '重新打开次数，上限2',
  `convertedReportId` BIGINT DEFAULT NULL COMMENT '转为举报后的report.id',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_user` (`userId`, `isDelete`),
  KEY `idx_feedback_status` (`status`, `isDelete`),
  KEY `idx_feedback_handler` (`handlerId`, `status`, `isDelete`),
  KEY `idx_feedback_type_priority` (`type`, `priority`, `isDelete`),
  KEY `idx_feedback_create_time` (`createTime` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';
```

**变更说明**（相比原 feedback-design.md）：
- 删除 `attachment_urls` JSON 字段 → 附件改用独立子表 `feedback_attachment`，详见 2.5
- 删除 `type` 中的 `COMPLAINT`/`CONTENT` → 迁移到 Report 模块
- 删除 `source` 中的 `PICTURE_REPORT`/`USER_REPORT` → Report 独占
- 新增 `converted_report_id` → 支持反馈转举报

### 2.3 feedback_reply 表（反馈回复）

```sql
CREATE TABLE `feedback_reply` (
  `id` BIGINT NOT NULL COMMENT '回复ID（雪花算法）',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `userId` BIGINT NOT NULL COMMENT '回复者用户ID',
  `replyType` VARCHAR(20) NOT NULL COMMENT '回复类型：USER_REPLY/ADMIN_REPLY/INTERNAL_NOTE',
  `content` TEXT NOT NULL COMMENT '回复内容',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reply_feedback` (`feedbackId`, `createTime` ASC, `isDelete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈回复表';
```

### 2.4 feedback_status_log 表（反馈状态日志）

```sql
CREATE TABLE `feedback_status_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `fromStatus` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
  `toStatus` VARCHAR(20) NOT NULL COMMENT '变更后状态',
  `operatorId` BIGINT NOT NULL COMMENT '操作人ID',
  `operatorType` VARCHAR(10) NOT NULL COMMENT '操作人类型：USER/ADMIN/SYSTEM',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_log_feedback` (`feedbackId`, `createTime` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈状态日志表';
```

### 2.5 feedback_attachment 表（反馈附件）— 新增

解决原设计中 `attachment_urls` JSON 字段的持久化问题。附件通过 OSS 存储，数据库只存 URL 引用。

```sql
CREATE TABLE `feedback_attachment` (
  `id` BIGINT NOT NULL COMMENT '附件ID',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `fileUrl` VARCHAR(500) NOT NULL COMMENT 'OSS 文件 URL',
  `fileName` VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
  `fileSize` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `fileType` VARCHAR(20) DEFAULT NULL COMMENT '文件类型：PNG/JPG/JPEG/GIF',
  `sortOrder` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_attachment_feedback` (`feedbackId`, `sortOrder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈附件表';
```

**附件上传流程**：

```
前端                          后端                           OSS
 │                            │                             │
 │  1. POST /feedback/upload  │                             │
 │  (MultipartFile)           │                             │
 │ ──────────────────────────►│  2. 校验文件                │
 │                            │     (大小≤5MB, 格式PNG/JPG) │
 │                            │  3. aliOssUtil.upload()     │
 │                            │ ───────────────────────────►│
 │                            │  4. 返回 OSS URL            │
 │                            │ ◄───────────────────────────│
 │  5. 返回 { attachmentId,   │                             │
 │           url }            │                             │
 │ ◄──────────────────────────│                             │
 │                            │                             │
 │  6. POST /feedback/submit  │                             │
 │  { ..., attachmentIds: [] }│                             │
 │ ──────────────────────────►│  7. 关联附件到反馈          │
```

**设计要点**：
- 附件先上传获取 ID，提交反馈时关联（两步式上传）
- 复用现有 `AliOssUtil.upload()` 工具类，上传至 `feedback/yyyy/MM/` 目录
- 最多 3 张附件，单张 ≤ 5MB，格式限 PNG/JPG/JPEG
- 反馈删除时，附件仅逻辑删除（保留 OSS 文件用于审计）

### 2.6 report 表（举报表）— 保留原设计

```sql
CREATE TABLE `report` (
  `id` BIGINT NOT NULL COMMENT '举报ID（雪花算法）',
  `reporterId` BIGINT NOT NULL COMMENT '举报人ID',
  `targetType` VARCHAR(20) NOT NULL COMMENT '举报对象类型：PICTURE/USER',
  `targetId` BIGINT NOT NULL COMMENT '举报目标ID',
  `reasonType` VARCHAR(20) NOT NULL COMMENT '举报原因：PORNOGRAPHY/VIOLENCE/SCAM/COPYRIGHT/SPAM/HARASSMENT/OTHER',
  `description` TEXT DEFAULT NULL COMMENT '补充描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/APPROVED/REJECTED/WARN_ONLY/REMOVE_ONLY',
  `handlerId` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handleResult` VARCHAR(50) DEFAULT NULL COMMENT '处理结果：WARN/BAN_TEMP_3/BAN_TEMP_7/BAN_TEMP_30/BAN_PERMANENT/REMOVE_PICTURE/REJECT',
  `handleReason` TEXT DEFAULT NULL COMMENT '处理理由',
  `handleTime` DATETIME DEFAULT NULL COMMENT '处理时间',
  `reportCount` INT NOT NULL DEFAULT 0 COMMENT '该目标累计被举报次数',
  `sourceFeedbackId` BIGINT DEFAULT NULL COMMENT '来源反馈ID（从反馈转来时填写）',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`targetType`, `targetId`),
  KEY `idx_status_count` (`status`, `reportCount` DESC, `createTime`),
  KEY `idx_reporter_dup` (`reporterId`, `targetType`, `targetId`),
  KEY `idx_target_count` (`targetType`, `targetId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';
```

**变更说明**：
- 新增 `source_feedback_id` → 记录从哪个反馈转来，支持溯源

### 2.7 ban_record 表（封禁记录表）— 保留原设计

```sql
CREATE TABLE `ban_record` (
  `id` BIGINT NOT NULL COMMENT '封禁记录ID（雪花算法）',
  `userId` BIGINT NOT NULL COMMENT '被封禁用户ID',
  `banType` VARCHAR(20) NOT NULL COMMENT '封禁类型：TEMP/PERMANENT',
  `banDuration` INT DEFAULT NULL COMMENT '封禁天数（3/7/30）',
  `banStartTime` DATETIME NOT NULL COMMENT '封禁开始时间',
  `banEndTime` DATETIME DEFAULT NULL COMMENT '封禁结束时间（永久封禁为NULL）',
  `banReason` VARCHAR(200) NOT NULL COMMENT '封禁原因',
  `violationCount` INT NOT NULL DEFAULT 0 COMMENT '封禁时累计违规次数',
  `unbanned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封',
  `unbanTime` DATETIME DEFAULT NULL COMMENT '解封时间',
  `unbanReason` TEXT DEFAULT NULL COMMENT '解封理由',
  `unbanOperatorId` BIGINT DEFAULT NULL COMMENT '解封操作人ID',
  `banOperatorId` BIGINT NOT NULL COMMENT '封禁操作人ID',
  `banOperatorName` VARCHAR(100) NOT NULL COMMENT '封禁操作人姓名',
  `reportId` BIGINT DEFAULT NULL COMMENT '关联举报ID',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`userId`, `banStartTime` DESC),
  KEY `idx_unbanned_end` (`unbanned`, `banEndTime`),
  KEY `idx_ban_end` (`banEndTime`, `unbanned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封禁记录表';
```

### 2.8 user 表扩展

```sql
ALTER TABLE `user`
  ADD COLUMN `banStatus` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '封禁状态：NONE/TEMP/PERMANENT' AFTER `userRole`,
  ADD COLUMN `banEndTime` DATETIME DEFAULT NULL COMMENT '封禁结束时间' AFTER `banStatus`,
  ADD COLUMN `violationCount` INT NOT NULL DEFAULT 0 COMMENT '累计违规次数（6个月滚动窗口）' AFTER `banEndTime`,
  ADD COLUMN `lastViolationTime` DATETIME DEFAULT NULL COMMENT '最近一次违规时间' AFTER `violationCount`,
  ADD INDEX `idx_ban_status` (`banStatus`, `banEndTime`);
```

---

## 三、枚举定义

### 3.1 Feedback 相关枚举

```java
// FeedbackType — 反馈类型
BUG("BUG", "Bug报告"),
FEATURE("FEATURE", "功能建议"),
ACCOUNT("ACCOUNT", "账号问题"),
EXPERIENCE("EXPERIENCE", "体验反馈"),
OTHER("OTHER", "其他");
// 注意：COMPLAINT、CONTENT 已迁移至 Report 模块

// FeedbackStatus — 反馈状态
PENDING("PENDING", "待处理"),
PROCESSING("PROCESSING", "处理中"),
RESOLVED("RESOLVED", "已解决"),
REJECTED("REJECTED", "已拒绝"),
REOPENED("REOPENED", "已重新打开"),
CLOSED("CLOSED", "已关闭");

// FeedbackSource — 反馈来源
APP("APP", "应用内"),
SYSTEM("SYSTEM", "系统自动"),
ADMIN("ADMIN", "管理员创建");

// FeedbackPriority — 优先级
P0("P0", "紧急-2h内响应"),
P1("P1", "高-8h内响应"),
P2("P2", "中-24h内响应"),
P3("P3", "低-72h内响应");

// FeedbackReplyType — 回复类型
USER_REPLY("USER_REPLY", "用户回复"),
ADMIN_REPLY("ADMIN_REPLY", "管理员回复"),
INTERNAL_NOTE("INTERNAL_NOTE", "内部备注");

// FeedbackCloseReason — 关闭原因
USER_WITHDRAW("USER_WITHDRAW", "用户撤回"),
AUTO_CLOSE("AUTO_CLOSE", "系统自动关闭"),
ADMIN_CLOSE("ADMIN_CLOSE", "管理员关闭"),
USER_CONFIRM("USER_CONFIRM", "用户确认解决");
```

### 3.2 Report 相关枚举

```java
// ReportReason — 举报原因
PORNOGRAPHY("PORNOGRAPHY", "色情内容", "HIGH"),
VIOLENCE("VIOLENCE", "暴力内容", "HIGH"),
SCAM("SCAM", "诈骗信息", "HIGH"),
COPYRIGHT("COPYRIGHT", "侵权内容", "MEDIUM"),
SPAM("SPAM", "垃圾信息", "MEDIUM"),
HARASSMENT("HARASSMENT", "恶意行为", "MEDIUM"),
OTHER("OTHER", "其他", "LOW");

// ReportStatus — 举报状态
PENDING("PENDING", "待审核"),
PROCESSING("PROCESSING", "处理中"),
APPROVED("APPROVED", "已通过"),
REJECTED("REJECTED", "已驳回"),
WARN_ONLY("WARN_ONLY", "仅警告"),
REMOVE_ONLY("REMOVE_ONLY", "仅下架");

// ReportTargetType — 举报目标类型
PICTURE("PICTURE", "图片"),
USER("USER", "用户");

// HandleResult — 处理结果
REJECT("REJECT", "驳回"),
WARN("WARN", "警告"),
BAN_TEMP_3("BAN_TEMP_3", "封禁3天"),
BAN_TEMP_7("BAN_TEMP_7", "封禁7天"),
BAN_TEMP_30("BAN_TEMP_30", "封禁30天"),
BAN_PERMANENT("BAN_PERMANENT", "永久封禁"),
REMOVE_PICTURE("REMOVE_PICTURE", "下架图片"),
RESTORE_PICTURE("RESTORE_PICTURE", "恢复图片");
```

### 3.3 Ban 相关枚举

```java
// BanType — 封禁类型
NONE("NONE", "正常"),
TEMP("TEMP", "临时封禁"),
PERMANENT("PERMANENT", "永久封禁");

// BanDuration — 封禁时长
DAYS_3(3, "3天"),
DAYS_7(7, "7天"),
DAYS_30(30, "30天");
```

### 3.4 NotificationType 扩展

在现有 `NotificationTypeEnum` 基础上新增：

```java
// === 反馈相关 ===
FEEDBACK_REPLIED("FEEDBACK_REPLIED", "反馈已回复"),
FEEDBACK_RESOLVED("FEEDBACK_RESOLVED", "反馈已解决"),
FEEDBACK_REJECTED("FEEDBACK_REJECTED", "反馈已拒绝"),

// === 举报相关 ===
REPORT_ACCEPTED("REPORT_ACCEPTED", "举报已受理"),
REPORT_REJECTED("REPORT_REJECTED", "举报已驳回"),

// === 被举报人相关 ===
USER_WARNED("USER_WARNED", "违规警告"),
USER_BANNED("USER_BANNED", "账号封禁"),
USER_UNBANNED("USER_UNBANNED", "账号解封"),

// === 图片相关 ===
PICTURE_REMOVED("PICTURE_REMOVED", "图片下架"),
PICTURE_RESTORED("PICTURE_RESTORED", "图片恢复"),

// === 封禁相关 ===
BAN_EXPIRING("BAN_EXPIRING", "封禁即将到期"),
REPORT_RESTRICTED("REPORT_RESTRICTED", "举报权限暂停");
```

---

## 四、Redis 缓存设计

### 4.1 Key 清单

| Key 模式 | 类型 | TTL | 所属模块 | 用途 |
|---------|------|-----|---------|------|
| `feedback:submit:count:{userId}` | STRING(INT) | 到当天23:59:59 | Feedback | 提交限流 5条/天 |
| `feedback:stats:daily:{date}` | STRING(JSON) | 7天 | Feedback | 每日统计缓存 |
| `feedback:admin:pending:count` | STRING(INT) | 5分钟 | Feedback | 待处理计数红点 |
| `feedback:detail:{id}` | HASH | 30分钟 | Feedback | 热点反馈详情 |
| `lock:feedback:submit:{userId}` | STRING | 10秒 | Feedback | 提交防重锁 |
| `feedback:auto:close:check` | ZSET | 持久 | Feedback | 自动关闭队列 |
| `rate_limit:report:{userId}` | STRING | 60秒 | Report | 举报限流 3次/分钟 |
| `report:dup:{reporterId}:{targetType}:{targetId}` | STRING | 7天 | Report | 防重复举报 |
| `report:credit:{userId}` | STRING(JSON) | 24小时 | Report | 举报人信誉分 |
| `ban:status:{userId}` | STRING(JSON) | 30分钟 | Ban | 单用户封禁状态 |
| `ban:blacklist` | ZSET | 持久 | Ban | 黑名单(member=userId, score=banEndTime) |

### 4.2 RedisKeyConstants 新增常量

```java
// ==================== Feedback 模块 ====================
public static final String FEEDBACK_SUBMIT_COUNT = "feedback:submit:count:";
public static final String FEEDBACK_STATS_DAILY = "feedback:stats:daily:";
public static final String FEEDBACK_ADMIN_PENDING_COUNT = "feedback:admin:pending:count";
public static final String FEEDBACK_DETAIL = "feedback:detail:";
public static final String LOCK_FEEDBACK_SUBMIT = "lock:feedback:submit:";
public static final String FEEDBACK_AUTO_CLOSE_CHECK = "feedback:auto:close:check";

// ==================== Report 模块 ====================
public static final String REPORT_RATE_LIMIT_KEY = "rate_limit:report:%d";
public static final String REPORT_DUPLICATE_KEY = "report:dup:%d:%s:%d";
public static final String REPORT_CREDIT_KEY = "report:credit:%d";

// ==================== Ban 模块 ====================
public static final String BAN_STATUS_KEY = "ban:status:%d";
public static final String BAN_BLACKLIST_ZSET = "ban:blacklist";

// ==================== TTL 常量 ====================
public static final int BAN_STATUS_TTL = 30 * 60;
public static final int REPORT_RATE_LIMIT_WINDOW = 60;
public static final int REPORT_RATE_LIMIT_MAX = 3;
public static final int REPORT_DUPLICATE_TTL = 7 * 24 * 3600;
public static final int REPORT_CREDIT_TTL = 24 * 3600;
public static final int BAN_SCHEDULE_INTERVAL = 5 * 60;
```

---

## 五、API 接口设计

### 5.1 接口总览

```
/api/feedback/**           → 用户端反馈接口（需 ROLE_USER）
/api/admin/feedback/**     → 管理端反馈接口（需 ROLE_ADMIN）
/api/report/**             → 用户端举报接口（需 ROLE_USER）
/api/admin/report/**       → 管理端举报接口（需 ROLE_ADMIN）
/api/admin/ban/**          → 管理端封禁接口（需 ROLE_ADMIN）
```

### 5.2 Feedback 用户端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/feedback/upload` | 上传反馈附件（返回 attachmentId + URL） |
| POST | `/feedback/submit` | 提交反馈 |
| DELETE | `/feedback/{id}/withdraw` | 撤回反馈（仅 PENDING 状态） |
| GET | `/feedback/list` | 个人反馈列表 |
| GET | `/feedback/{id}` | 反馈详情（含回复、状态日志、附件） |
| POST | `/feedback/{id}/reply` | 追加回复 |
| POST | `/feedback/{id}/reopen` | 重新打开（限 2 次） |
| POST | `/feedback/{id}/confirm` | 确认解决并关闭 |
| GET | `/feedback/stats` | 个人反馈统计 |

**POST `/feedback/submit`**：

```json
// Request
{
  "title": "图片上传失败",
  "content": "上传 PNG 格式图片时提示服务器错误",
  "type": "BUG",
  "relatedPictureId": null,
  "relatedUserId": null,
  "attachmentIds": [1001, 1002],   // 先上传附件获得的 ID 列表
  "isAnonymous": false
}

// Response
{
  "code": 0,
  "message": "提交成功",
  "data": { "feedbackId": 10086 }
}
```

**POST `/feedback/upload`**（新增，两步式上传）：

```
POST /feedback/upload
Content-Type: multipart/form-data

file: (binary)

// Response
{
  "code": 0,
  "data": {
    "attachmentId": 1001,
    "url": "https://zoustarfall.xin/feedback/2026/06/xxx.png"
  }
}
```

### 5.3 Feedback 管理端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/feedback/list` | 全量反馈列表 |
| GET | `/admin/feedback/{id}` | 反馈详情（含提交者信息） |
| POST | `/admin/feedback/{id}/claim` | 认领反馈 |
| POST | `/admin/feedback/{id}/transfer` | 转派反馈 |
| POST | `/admin/feedback/{id}/reply` | 管理员回复 |
| POST | `/admin/feedback/{id}/note` | 内部备注 |
| POST | `/admin/feedback/{id}/reject` | 拒绝反馈 |
| POST | `/admin/feedback/{id}/priority` | 调整优先级 |
| POST | `/admin/feedback/{id}/convert` | **转举报**（新增） |
| POST | `/admin/feedback/batch-close` | 批量关闭 |
| GET | `/admin/feedback/stats` | 统计看板 |
| GET | `/admin/feedback/export` | 导出 |

**POST `/admin/feedback/{id}/convert`**（反馈转举报，新增）：

```json
// Request
{
  "targetType": "PICTURE",        // 根据反馈内容判断
  "targetId": 123456,
  "reasonType": "PORNOGRAPHY",
  "description": "从反馈 #10086 转入：用户反馈的图片内容涉嫌违规"
}

// Response
{
  "code": 0,
  "message": "已转为举报工单",
  "data": { "reportId": 9876543210 }
}
```

### 5.4 Report 用户端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/report/submit` | 提交举报 |
| GET | `/report/my-list` | 我的举报列表 |
| GET | `/report/detail/{reportId}` | 举报详情 |
| POST | `/report/cancel` | 撤回举报（10分钟内，PENDING 状态） |

### 5.5 Report 管理端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/report/list` | 举报列表（按被举报次数排序） |
| GET | `/admin/report/detail/{reportId}` | 举报详情（含目标信息、关联举报） |
| POST | `/admin/report/handle` | 审核处理举报（可触发封禁） |
| POST | `/admin/report/batch-handle` | 批量处理 |

### 5.6 Ban 管理端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/ban/list` | 封禁记录列表 |
| POST | `/admin/ban/unban` | 手动解封 |
| GET | `/admin/ban/stats` | 封禁统计 |

### 5.7 ErrorCode 新增

```java
// Feedback 相关
FEEDBACK_RATE_LIMIT(40010, "今日反馈次数已达上限"),
FEEDBACK_DUPLICATE(40011, "请勿重复提交"),
FEEDBACK_NOT_FOUND(40012, "反馈不存在"),
FEEDBACK_NO_PERMISSION(40013, "无权操作此反馈"),
FEEDBACK_STATUS_ERROR(40014, "当前状态不允许此操作"),
FEEDBACK_REOPEN_LIMIT(40015, "重新打开次数已达上限"),
FEEDBACK_ATTACHMENT_LIMIT(40016, "附件数量超过上限（最多3张）"),
FEEDBACK_ATTACHMENT_TOO_LARGE(40017, "附件大小超过限制（最大5MB）"),

// Report 相关
REPORT_RATE_LIMIT_EXCEEDED(40020, "举报频率超限，请稍后再试"),
REPORT_DUPLICATE(40021, "请勿重复举报"),
REPORT_TARGET_NOT_FOUND(40022, "举报目标不存在"),
REPORT_SELF(40023, "不能举报自己"),
REPORT_RESTRICTED(40024, "举报权限已被暂停"),
REPORT_CANCEL_TIMEOUT(40025, "举报已超过撤回时间"),

// Ban 相关
USER_BANNED(40102, "账号已被封禁"),
USER_BANNED_PERMANENT(40103, "账号已被永久封禁"),
USER_BANNED_EXPIRED(40104, "封禁已过期，请重新登录");
```

---

## 六、核心模块改造

### 6.1 LoginInterceptor 封禁检查

在现有 `LoginInterceptor.java` 的 `preHandle` 方法中新增封禁检查：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 放行 OPTIONS 预检请求
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }

    HttpSession session = request.getSession(false);
    if (ObjUtil.hasEmpty(session, session.getAttribute(UserConstant.USER_LOGIN_STATE))) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }

    User user = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);

    // 新增：封禁检查（Redis → DB 降级）
    checkBanStatus(user);

    UserContext.set(user);
    return true;
}
```

检查逻辑优先级：Redis 缓存 → 数据库 → 降级放行（仅 Redis 异常时）。

### 6.2 User 实体扩展

在 `User.java` 新增字段：

```java
/** 封禁状态：NONE/TEMP/PERMANENT */
private String banStatus;

/** 封禁结束时间 */
private Date banEndTime;

/** 累计违规次数（6个月滚动窗口） */
private Integer violationCount;

/** 最近一次违规时间 */
private Date lastViolationTime;
```

### 6.3 通知集成

所有通知复用现有 `NotificationEvent` + `NotificationEventListener` 机制，扩展 `NotificationTypeEnum` 即可。

通知场景矩阵：

| 场景 | 通知类型 | 接收者 | 触发模块 |
|------|---------|--------|---------|
| 反馈已回复 | FEEDBACK_REPLIED | 反馈提交者 | Feedback |
| 反馈已解决 | FEEDBACK_RESOLVED | 反馈提交者 | Feedback |
| 举报已受理 | REPORT_ACCEPTED | 举报人 | Report |
| 举报已驳回 | REPORT_REJECTED | 举报人 | Report |
| 违规警告 | USER_WARNED | 被举报人 | Report |
| 账号封禁 | USER_BANNED | 被封禁用户 | Ban |
| 账号解封 | USER_UNBANNED | 被解封用户 | Ban |
| 图片下架 | PICTURE_REMOVED | 图片作者 | Report |
| 封禁到期提醒 | BAN_EXPIRING | 封禁用户 | Ban(定时任务) |
| 举报权限暂停 | REPORT_RESTRICTED | 恶意举报人 | Report |

---

## 七、模块间联动设计

### 7.1 反馈 → 举报（转举报流程）

```
管理员查看反馈详情 → 发现违规内容 → 点击「转举报」
     │
     ▼
FeedbackService.convertToReport(feedbackId, convertDTO)
     │
     ├── 1. 校验反馈状态（仅 PENDING/PROCESSING 可转）
     ├── 2. 创建 Report 记录
     │      └── sourceFeedbackId = feedbackId
     ├── 3. 更新 Feedback 状态
     │      └── convertedReportId = reportId
     │      └── status = CLOSED, closeReason = ADMIN_CLOSE
     ├── 4. 迁移附件（反馈附件引用到举报描述中）
     └── 5. 发送通知（告知反馈提交者已转处理）
```

### 7.2 举报 → 封禁（审核处理流程）

```
管理员审核举报 → handleResult=APPROVED + banAction
     │
     ▼
ReportService.handleReport(handleDTO)
     │
     ├── 1. 更新 Report 状态
     ├── 2. BanService.executeBan(userId, banAction, operatorId)
     │      ├── 创建 ban_record 记录
     │      ├── 更新 user 表 banStatus / banEndTime / violationCount
     │      ├── 写入 Redis 黑名单 ZSET
     │      └── 清除用户 Session（强制登出）
     ├── 3. 如需下架图片 → 更新 picture.reviewStatus
     └── 4. 发送通知（USER_BANNED / PICTURE_REMOVED）
```

### 7.3 封禁 → 登录拦截

```
用户请求 → LoginInterceptor.preHandle()
     │
     ├── Redis 查 ban:status:{userId}
     │      ├── 命中且 banStatus != NONE → 拒绝
     │      └── 未命中 → 查 DB → 回写缓存
     └── Redis 异常 → 降级查 DB
```

### 7.4 封禁 → 自动解封（定时任务）

```
每5分钟 → BanScheduled.autoUnbanExpiredUsers()
     │
     ├── ZRANGEBYSCORE ban:blacklist 0 {now}
     ├── 逐条调用 BanService.unbanUser()
     │      ├── 更新 banRecord.unbanned = 1
     │      ├── 更新 user.banStatus = NONE
     │      ├── ZREM ban:blacklist
     │      └── 发送 USER_UNBANNED 通知
     └── ZREMRANGEBYSCORE 清理已过期记录
```

---

## 八、代码结构规划

### 8.1 新增类清单

```
pojo/
├── entity/
│   ├── Feedback.java                      # 反馈实体
│   ├── FeedbackReply.java                 # 反馈回复实体
│   ├── FeedbackStatusLog.java             # 反馈状态日志实体
│   ├── FeedbackAttachment.java            # 反馈附件实体
│   ├── Report.java                        # 举报实体
│   └── BanRecord.java                     # 封禁记录实体
├── dto/
│   ├── feedback/
│   │   ├── FeedbackSubmitDTO.java         # 提交反馈
│   │   ├── FeedbackQueryDTO.java          # 反馈查询
│   │   ├── FeedbackReplyDTO.java          # 反馈回复
│   │   └── FeedbackConvertDTO.java        # 反馈转举报
│   └── report/
│       ├── ReportSubmitDTO.java           # 提交举报
│       ├── ReportQueryDTO.java            # 举报查询
│       ├── ReportHandleDTO.java           # 处理举报
│       ├── BanActionDTO.java              # 封禁动作
│       └── BanUnbanDTO.java               # 解封请求
└── vo/
    ├── feedback/
    │   ├── FeedbackVO.java                # 反馈详情 VO
    │   ├── FeedbackListItemVO.java        # 反馈列表项 VO
    │   ├── FeedbackStatsVO.java           # 反馈统计 VO
    │   └── FeedbackAttachmentVO.java      # 附件 VO
    └── report/
        ├── ReportVO.java                  # 举报详情 VO
        ├── ReportTargetVO.java            # 被举报目标 VO
        └── BanRecordVO.java               # 封禁记录 VO

common/
└── enums/
    ├── FeedbackTypeEnum.java
    ├── FeedbackStatusEnum.java
    ├── FeedbackPriorityEnum.java
    ├── ReportReasonEnum.java
    ├── ReportStatusEnum.java
    ├── BanTypeEnum.java
    └── HandleResultEnum.java

server/
├── mapper/
│   ├── FeedbackMapper.java
│   ├── FeedbackReplyMapper.java
│   ├── FeedbackStatusLogMapper.java
│   ├── FeedbackAttachmentMapper.java
│   ├── ReportMapper.java
│   └── BanRecordMapper.java
├── service/
│   ├── FeedbackService.java
│   ├── ReportService.java
│   └── BanService.java
├── service/impl/
│   ├── CachedFeedbackServiceImpl.java     # 缓存层
│   ├── FeedbackServiceImpl.java           # DB 层
│   ├── CachedReportServiceImpl.java       # 缓存层
│   ├── ReportServiceImpl.java             # DB 层
│   └── BanServiceImpl.java
├── controller/
│   ├── FeedbackController.java            # 用户端反馈
│   ├── ReportController.java             # 用户端举报
│   └── admin/
│       ├── AdminFeedbackController.java   # 管理端反馈
│       ├── AdminReportController.java     # 管理端举报
│       └── AdminBanController.java        # 管理端封禁
└── scheduled/
    ├── FeedbackAutoCloseScheduled.java    # 反馈自动关闭
    └── BanScheduled.java                  # 封禁到期解封
```

### 8.2 修改现有类清单

| 类 | 改动 |
|----|------|
| `User.java` | 新增 banStatus/banEndTime/violationCount/lastViolationTime |
| `NotificationTypeEnum.java` | 新增 11 个通知类型 |
| `RedisKeyConstants.java` | 新增 3 个模块的 Key 和 TTL 常量 |
| `LoginInterceptor.java` | 新增封禁检查逻辑 |
| `ErrorCode.java` | 新增 16 个错误码 |
| `InterceptorConfig.java` | 添加新接口路由规则 |

---

## 九、定时任务

| 任务 | 执行频率 | 模块 | 说明 |
|------|---------|------|------|
| 封禁自动解封 | 每 5 分钟 | Ban | ZRANGEBYSCORE 扫描过期封禁 |
| 封禁到期提醒 | 每天凌晨 1 点 | Ban | 提前 1 天通知即将解封 |
| 违规计数重置 | 每天凌晨 2 点 | Ban | 重置 6 个月前的违规计数 |
| 反馈自动关闭 | 每 1 小时 | Feedback | RESOLVED 超 7 天自动关闭 |
| 待处理计数刷新 | 每 5 分钟 | Feedback | 刷新管理端红点计数 |

---

## 十、性能与安全

### 10.1 性能目标

| 操作 | 目标 | 策略 |
|------|------|------|
| 登录拦截（封禁检查） | < 100ms | Redis 缓存优先，TTL 30min |
| 反馈提交 | < 500ms | 异步通知、防重锁 |
| 举报提交 | < 500ms | 限流 Redis 原子操作、SETNX 防重复 |
| 管理端列表 | < 1s | 复合索引、分页限制、冗余字段 |

### 10.2 安全防护

| 威胁 | 防护措施 | 模块 |
|------|---------|------|
| 频繁提交反馈 | 每用户每天 5 条上限 + 防重锁 10s | Feedback |
| 频繁举报 | 每分钟 3 次上限 | Report |
| 重复举报 | 7 天内同一目标不能重复举报 | Report |
| 恶意举报 | 信誉分系统，驳回率 ≥ 80% 暂停权限 | Report |
| 附件恶意上传 | 文件大小 ≤ 5MB，格式白名单 | Feedback |
| Redis 故障 | 降级到数据库查询 | Ban |
| 封禁绕过 | Session 清除 + Redis 黑名单 | Ban |

---

## 十一、实施计划

### 11.1 开发顺序

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 数据库建表 + User 表扩展 + ErrorCode + 枚举 | P0 |
| Phase 2 | Report 模块核心（提交/列表/详情） | P0 |
| Phase 3 | Ban 模块核心（执行封禁/解封/登录拦截） | P0 |
| Phase 4 | Report 管理端（审核处理） | P0 |
| Phase 5 | Feedback 模块核心（提交/附件上传/列表/详情） | P1 |
| Phase 6 | Redis 缓存层 + 定时任务 | P1 |
| Phase 7 | Feedback 管理端 + 反馈转举报 | P1 |
| Phase 8 | 通知集成 + 恶意举报防护 | P2 |
| Phase 9 | 前端联调 + 测试 | P3 |
