# 用户反馈机制 — 技术设计文档

## 一、数据库表设计

### 1.1 主表：feedback

| 字段 | 类型 | 是否必填 | 说明 |
|------|------|---------|------|
| id | BIGINT PK | 自增 | 主键 |
| userId | BIGINT | 是 | 提交者用户 ID（匿名反馈为 NULL） |
| title | VARCHAR(100) | 是 | 反馈标题，最多 50 字 |
| content | TEXT | 是 | 内容描述，最多 2000 字 |
| type | VARCHAR(20) | 是 | 类型：BUG / FEATURE / COMPLAINT / ACCOUNT / EXPERIENCE / CONTENT / OTHER |
| priority | VARCHAR(5) | 是 | 优先级：P0 / P1 / P2 / P3 |
| status | VARCHAR(20) | 是 | 状态：PENDING / PROCESSING / RESOLVED / REJECTED / REOPENED / CLOSED |
| source | VARCHAR(20) | 是 | 来源：APP / PICTURE_REPORT / USER_REPORT / SYSTEM / ADMIN |
| relatedPictureId | BIGINT | 否 | 关联图片 ID |
| relatedUserId | BIGINT | 否 | 关联用户 ID（被举报用户） |
| handlerId | BIGINT | 否 | 当前处理人管理员 ID |
| attachmentUrls | JSON | 否 | 附件图片 URL 数组，最多 3 张 |
| isAnonymous | TINYINT(1) | 是 | 是否匿名提交，默认 0 |
| closeReason | VARCHAR(50) | 否 | 关闭原因：USER_WITHDRAW / AUTO_CLOSE / ADMIN_CLOSE / USER_CONFIRM |
| reopenCount | INT | 是 | 重新打开次数，默认 0，上限 2 |
| createTime | DATETIME | 是 | 创建时间 |
| updateTime | DATETIME | 是 | 更新时间 |
| isDelete | TINYINT(1) | 是 | 逻辑删除，默认 0 |

**索引建议**：

```sql
CREATE INDEX idx_feedback_user_id ON feedback(userId, isDelete);
CREATE INDEX idx_feedback_status ON feedback(status, isDelete);
CREATE INDEX idx_feedback_handler ON feedback(handlerId, status, isDelete);
CREATE INDEX idx_feedback_type_priority ON feedback(type, priority, isDelete);
CREATE INDEX idx_feedback_create_time ON feedback(createTime DESC);
CREATE INDEX idx_feedback_related_picture ON feedback(relatedPictureId, isDelete);
CREATE INDEX idx_feedback_related_user ON feedback(relatedUserId, isDelete);
```

### 1.2 子表：feedback_reply

| 字段 | 类型 | 是否必填 | 说明 |
|------|------|---------|------|
| id | BIGINT PK | 自增 | 主键 |
| feedbackId | BIGINT | 是 | 关联反馈 ID |
| userId | BIGINT | 是 | 回复者用户 ID（用户本人或管理员） |
| replyType | VARCHAR(20) | 是 | 回复类型：USER_REPLY / ADMIN_REPLY / INTERNAL_NOTE |
| content | TEXT | 是 | 回复内容，最多 1000 字 |
| createTime | DATETIME | 是 | 回复时间 |
| isDelete | TINYINT(1) | 是 | 逻辑删除，默认 0 |

**索引建议**：

```sql
CREATE INDEX idx_reply_feedback_id ON feedback_reply(feedbackId, createTime ASC, isDelete);
CREATE INDEX idx_reply_user_id ON feedback_reply(userId, isDelete);
```

### 1.3 子表：feedback_status_log

| 字段 | 类型 | 是否必填 | 说明 |
|------|------|---------|------|
| id | BIGINT PK | 自增 | 主键 |
| feedbackId | BIGINT | 是 | 关联反馈 ID |
| fromStatus | VARCHAR(20) | 否 | 变更前状态（首次提交时为 NULL） |
| toStatus | VARCHAR(20) | 是 | 变更后状态 |
| operatorId | BIGINT | 是 | 操作人 ID |
| operatorType | VARCHAR(10) | 是 | 操作人类型：USER / ADMIN / SYSTEM |
| remark | VARCHAR(500) | 否 | 备注说明 |
| createTime | DATETIME | 是 | 操作时间 |

**索引建议**：

```sql
CREATE INDEX idx_status_log_feedback_id ON feedback_status_log(feedbackId, createTime ASC);
```

### 1.4 枚举定义参考

```
-- type 反馈类型
BUG          Bug 报告
FEATURE      功能建议
COMPLAINT    投诉举报
ACCOUNT      账号问题
EXPERIENCE   体验反馈
CONTENT      内容审核申诉
OTHER        其他

-- status 状态
PENDING      待处理
PROCESSING   处理中
RESOLVED     已解决
REJECTED     已拒绝
REOPENED     已重新打开
CLOSED       已关闭

-- source 来源
APP              应用内反馈入口
PICTURE_REPORT   图片举报
USER_REPORT      用户举报
SYSTEM           系统自动生成
ADMIN            管理员手动创建

-- priority 优先级
P0   紧急 — 2 小时内响应
P1   高   — 8 小时内响应
P2   中   — 24 小时内响应
P3   低   — 72 小时内响应

-- replyType 回复类型
USER_REPLY      用户回复
ADMIN_REPLY     管理员回复（对用户可见）
INTERNAL_NOTE   内部备注（仅管理员可见）

-- closeReason 关闭原因
USER_WITHDRAW   用户撤回
AUTO_CLOSE      系统自动关闭
ADMIN_CLOSE     管理员关闭
USER_CONFIRM    用户确认解决

-- operatorType 操作人类型
USER    用户
ADMIN   管理员
SYSTEM  系统
```

---

## 二、API 接口清单

### 2.1 用户端接口

| 方法 | 路径 | 说明 | 请求参数 | 返回数据 |
|------|------|------|---------|---------|
| POST | `/feedback/submit` | 提交反馈 | `{ title, content, type, relatedPictureId?, relatedUserId?, attachmentUrls?, isAnonymous? }` | `{ feedbackId }` |
| DELETE | `/feedback/{id}/withdraw` | 撤回反馈（仅 PENDING） | 路径参数 id | 成功/失败 |
| GET | `/feedback/list` | 查询个人反馈列表 | `?page=1&size=10&status=&type=` | `{ total, list: [{ id, title, type, status, createTime, updateTime }] }` |
| GET | `/feedback/{id}` | 查询反馈详情 | 路径参数 id | `{ 完整反馈信息 + replies + statusLogs }` |
| POST | `/feedback/{id}/reply` | 追加回复 | `{ content }` | `{ replyId }` |
| POST | `/feedback/{id}/reopen` | 重新打开反馈 | `{ reason? }` | 成功/失败 |
| POST | `/feedback/{id}/confirm` | 确认解决并关闭 | 无 | 成功/失败 |
| POST | `/feedback/{id}/rate` | 满意度评价 | `{ satisfied: boolean }` | 成功/失败 |
| GET | `/feedback/stats` | 个人反馈统计 | 无 | `{ total, pending, processing, resolved, closed }` |

#### 接口详细设计

**POST `/feedback/submit` — 提交反馈**

```json
// Request
{
  "title": "图片上传失败",
  "content": "上传 PNG 格式图片时提示服务器错误，已尝试多次",
  "type": "BUG",
  "relatedPictureId": null,
  "relatedUserId": null,
  "attachmentUrls": ["https://oss.example.com/feedback/screenshot1.png"],
  "isAnonymous": false
}

// Response
{
  "code": 200,
  "message": "提交成功",
  "data": {
    "feedbackId": 10086,
    "estimatedResponseTime": "24小时内"
  }
}
```

**GET `/feedback/list` — 个人反馈列表**

```json
// Query Params
?page=1&size=10&status=PENDING&type=BUG

// Response
{
  "code": 200,
  "data": {
    "total": 15,
    "list": [
      {
        "id": 10086,
        "title": "图片上传失败",
        "type": "BUG",
        "priority": "P2",
        "status": "PENDING",
        "createTime": "2026-06-05 14:30:00",
        "updateTime": "2026-06-05 14:30:00",
        "lastReplyTime": null
      }
    ]
  }
}
```

**GET `/feedback/{id}` — 反馈详情**

```json
// Response
{
  "code": 200,
  "data": {
    "id": 10086,
    "title": "图片上传失败",
    "content": "上传 PNG 格式图片时提示服务器错误",
    "type": "BUG",
    "priority": "P2",
    "status": "RESOLVED",
    "source": "APP",
    "relatedPictureId": null,
    "relatedUserId": null,
    "attachmentUrls": ["https://oss.example.com/feedback/screenshot1.png"],
    "isAnonymous": false,
    "createTime": "2026-06-05 14:30:00",
    "updateTime": "2026-06-05 16:00:00",
    "replies": [
      {
        "id": 1,
        "userId": 1,
        "nickname": "管理员小张",
        "avatar": "https://oss.example.com/avatar/admin1.png",
        "replyType": "ADMIN_REPLY",
        "content": "已定位问题，是 OSS 配置变更导致的，现已修复",
        "createTime": "2026-06-05 16:00:00"
      }
    ],
    "statusLogs": [
      {
        "fromStatus": null,
        "toStatus": "PENDING",
        "operatorType": "USER",
        "remark": "用户提交反馈",
        "createTime": "2026-06-05 14:30:00"
      },
      {
        "fromStatus": "PENDING",
        "toStatus": "PROCESSING",
        "operatorType": "ADMIN",
        "remark": "管理员认领",
        "createTime": "2026-06-05 15:00:00"
      },
      {
        "fromStatus": "PROCESSING",
        "toStatus": "RESOLVED",
        "operatorType": "ADMIN",
        "remark": "管理员回复并标记已解决",
        "createTime": "2026-06-05 16:00:00"
      }
    ]
  }
}
```

---

### 2.2 管理端接口

| 方法 | 路径 | 说明 | 请求参数 | 返回数据 |
|------|------|------|---------|---------|
| GET | `/admin/feedback/list` | 全量反馈列表 | `?page=1&size=20&type=&priority=&status=&handlerId=&startTime=&endTime=&keyword=` | `{ total, list }` |
| GET | `/admin/feedback/{id}` | 反馈详情（含用户信息） | 路径参数 id | `{ 完整信息 + 提交者账号信息 + 内部备注 }` |
| POST | `/admin/feedback/{id}/claim` | 认领反馈 | 路径参数 id | 成功/失败 |
| POST | `/admin/feedback/{id}/transfer` | 转派反馈 | `{ targetAdminId, reason }` | 成功/失败 |
| POST | `/admin/feedback/{id}/reply` | 管理员回复 | `{ content }` | `{ replyId }` |
| POST | `/admin/feedback/{id}/note` | 添加内部备注 | `{ content }` | `{ noteId }` |
| POST | `/admin/feedback/{id}/reject` | 拒绝反馈 | `{ reason }` | 成功/失败 |
| POST | `/admin/feedback/{id}/priority` | 调整优先级 | `{ priority: "P0" }` | 成功/失败 |
| POST | `/admin/feedback/create` | 手动创建反馈 | `{ userId, title, content, type, source: "ADMIN", ... }` | `{ feedbackId }` |
| POST | `/admin/feedback/batch-close` | 批量关闭 | `{ ids: [1,2,3], reason }` | `{ successCount, failCount }` |
| GET | `/admin/feedback/stats` | 统计看板数据 | `?startDate=&endDate=` | `{ 看板数据 }` |
| GET | `/admin/feedback/export` | 导出反馈数据 | `?type=&status=&startTime=&endTime=&format=xlsx` | 文件流 |

#### 接口详细设计

**GET `/admin/feedback/list` — 全量反馈列表**

```json
// Query Params
?page=1&size=20&type=BUG&priority=P1&status=PENDING&handlerId=&startTime=2026-06-01&endTime=2026-06-05&keyword=上传

// Response
{
  "code": 200,
  "data": {
    "total": 42,
    "list": [
      {
        "id": 10086,
        "submitter": {
          "userId": 100,
          "nickname": "用户A",
          "avatar": "https://oss.example.com/avatar/100.png"
        },
        "title": "图片上传失败",
        "type": "BUG",
        "priority": "P2",
        "status": "PENDING",
        "source": "APP",
        "handler": null,
        "createTime": "2026-06-05 14:30:00",
        "updateTime": "2026-06-05 14:30:00"
      }
    ]
  }
}
```

**POST `/admin/feedback/{id}/claim` — 认领反馈**

```json
// Response
{
  "code": 200,
  "message": "认领成功，反馈已进入处理中状态"
}
```

**POST `/admin/feedback/{id}/transfer` — 转派反馈**

```json
// Request
{
  "targetAdminId": 5,
  "reason": "此反馈涉及支付模块，转给支付组处理"
}

// Response
{
  "code": 200,
  "message": "转派成功"
}
```

**GET `/admin/feedback/stats` — 统计看板**

```json
// Response
{
  "code": 200,
  "data": {
    "overview": {
      "today": 5,
      "thisWeek": 23,
      "thisMonth": 87,
      "total": 342
    },
    "statusDistribution": {
      "PENDING": 15,
      "PROCESSING": 8,
      "RESOLVED": 280,
      "REJECTED": 12,
      "REOPENED": 3,
      "CLOSED": 24
    },
    "typeDistribution": {
      "BUG": 120,
      "FEATURE": 85,
      "COMPLAINT": 60,
      "ACCOUNT": 30,
      "EXPERIENCE": 25,
      "CONTENT": 15,
      "OTHER": 7
    },
    "priorityDistribution": {
      "P0": 3,
      "P1": 18,
      "P2": 95,
      "P3": 226
    },
    "avgResponseTime": "4.2h",
    "avgResolveTime": "18.6h",
    "dailyTrend": [
      { "date": "2026-06-01", "count": 3 },
      { "date": "2026-06-02", "count": 5 },
      { "date": "2026-06-03", "count": 2 },
      { "date": "2026-06-04", "count": 6 },
      { "date": "2026-06-05", "count": 5 }
    ],
    "handlerRanking": [
      {
        "handlerId": 1,
        "nickname": "管理员小张",
        "claimed": 45,
        "resolved": 42,
        "avgResponseTime": "2.1h"
      }
    ]
  }
}
```

---

### 2.3 接口鉴权说明

| 接口分组 | 鉴权要求 | 说明 |
|---------|---------|------|
| `/feedback/**` | 用户登录（ROLE_USER） | 操作自己的反馈，`userId` 从 token 中取 |
| `/admin/feedback/**` | 管理员登录（ROLE_ADMIN） | 可操作所有反馈 |

---

## 三、Redis 缓存 Key 设计

### 3.1 Key 清单

| Key 模式 | 类型 | 用途 | TTL |
|---------|------|------|-----|
| `feedback:submit:count:{userId}` | STRING (INT) | 用户当日提交计数（限流：5条/天） | 到当天 23:59:59 |
| `feedback:stats:daily:{date}` | STRING (JSON) | 每日反馈统计摘要（管理端看板缓存） | 7 天 |
| `feedback:admin:pending:count` | STRING (INT) | 待处理反馈总数（管理端 Badge 红点） | 5 分钟 |
| `feedback:detail:{id}` | HASH | 反馈详情缓存（高热点反馈） | 30 分钟 |
| `lock:feedback:submit:{userId}` | STRING | 提交防重锁（防并发重复提交） | 10 秒 |
| `feedback:auto:close:check` | ZSET (score=时间戳) | 自动关闭检测队列，score 为应关闭时间 | 持久 |

### 3.2 缓存策略说明

#### 3.2.1 提交限流 — `feedback:submit:count:{userId}`

```
场景：防止用户频繁提交反馈
逻辑：
  1. INCR feedback:submit:count:{userId}
  2. 首次设置 EXPIRE 到当天 23:59:59
  3. 若值 > 5，拒绝提交
一致性：不强一致，允许极少量超限
```

#### 3.2.2 每日统计 — `feedback:stats:daily:{date}`

```
场景：管理端看板的每日数据缓存
逻辑：
  1. 每日零点定时任务计算前一天统计，写入缓存
  2. 当天实时数据从数据库查询 + 前一天缓存拼接
  3. 管理端请求时优先读缓存
一致性：最终一致，TTL 到期后重新计算
```

#### 3.2.3 待处理计数 — `feedback:admin:pending:count`

```
场景：管理端导航栏红点数字展示
逻辑：
  1. 查询时读缓存，命中直接返回
  2. 未命中则 COUNT 查库并回填缓存
  3. 新反馈提交/认领/关闭时，主动 INCR/DECR 维护计数
一致性：允许短暂不一致，5 分钟自动刷新
```

#### 3.2.4 反馈详情 — `feedback:detail:{id}`

```
场景：热点反馈详情缓存（如被大量用户关注的投诉）
逻辑：
  1. 读时查缓存，未命中查库并回填
  2. 反馈状态变更/新增回复时，删除缓存（Cache-Aside）
  3. HASH 结构，字段：id, title, content, type, status, ...
一致性：Cache-Aside 模式，写时删除
```

#### 3.2.5 提交防重锁 — `lock:feedback:submit:{userId}`

```
场景：防止前端重复点击导致同一反馈提交多次
逻辑：
  1. SET NX lock:feedback:submit:{userId} 1 EX 10
  2. 设置成功则继续提交，失败则拒绝（提示"请勿重复提交"）
  3. 提交完成（成功或失败）后主动删除 key
一致性：分布式锁，10 秒自动过期兜底
```

#### 3.2.6 自动关闭队列 — `feedback:auto:close:check`

```
场景：RESOLVED 超过 7 天自动关闭、PENDING 超过 30 天自动关闭
逻辑：
  1. 反馈变为 RESOLVED 时，ZADD 以 (当前时间 + 7天) 为 score
  2. 反馈变为 PENDING 时，ZADD 以 (当前时间 + 30天) 为 score
  3. 定时任务每 1 小时执行，ZRANGEBYSCORE 0 (当前时间) 取出到期反馈
  4. 逐条执行关闭逻辑，ZREM 移除
  5. 反馈被手动关闭时，ZREM 移除对应记录
一致性：延时队列模式，定时轮询保证最终一致
```

### 3.3 RedisKeyConstants 常量定义参考

```java
public class RedisKeyConstants {

    // feedback:submit:count:{userId}
    public static final String FEEDBACK_SUBMIT_COUNT = "feedback:submit:count:";

    // feedback:stats:daily:{date}
    public static final String FEEDBACK_STATS_DAILY = "feedback:stats:daily:";

    // feedback:admin:pending:count
    public static final String FEEDBACK_ADMIN_PENDING_COUNT = "feedback:admin:pending:count";

    // feedback:detail:{id}
    public static final String FEEDBACK_DETAIL = "feedback:detail:";

    // lock:feedback:submit:{userId}
    public static final String LOCK_FEEDBACK_SUBMIT = "lock:feedback:submit:";

    // feedback:auto:close:check
    public static final String FEEDBACK_AUTO_CLOSE_CHECK = "feedback:auto:close:check";
}
```
