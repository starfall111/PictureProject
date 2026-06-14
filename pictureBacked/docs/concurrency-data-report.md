# 反馈-举报-封禁系统：线程安全与海量数据风险报告

**生成日期**: 2026-06-05
**基于**: architecture-feedback-report-ban.md 架构设计

---

## 一、线程安全与并发问题

### 1.1 举报提交防重复（SETNX 竞态）

**风险等级**: 中

**场景**: 两个请求几乎同时到达 `ReportServiceImpl.submitReport()`，都通过了 `SETNX report:dup:{reporterId}:{targetType}:{targetId}` 的检查。

**现状**: 当前使用 `setIfAbsent()` + 后续 `setex()` 两步操作，存在时间窗口内并发穿透的可能。

**建议**:
```java
// 使用 setIfAbsent(key, value, ttl, TimeUnit) 原子操作
Boolean acquired = stringRedisTemplate.opsForValue()
    .setIfAbsent(dupKey, "1", RedisKeyConstants.REPORT_DUPLICATE_TTL, TimeUnit.SECONDS);
```
当前代码已经是原子操作，风险可控。但需确认 Redis 集群模式下 SETNX 的可靠性（主从切换时可能丢失）。

### 1.2 反馈提交防重锁（Redis 锁超时问题）

**风险等级**: 低

**场景**: 用户提交反馈时获取 `lock:feedback:submit:{userId}` 锁（TTL 10s），如果事务处理超过 10s，锁自动释放，可能导致重复提交。

**建议**: 10s 对于单次反馈提交绰绰有余。如果未来附件处理变慢，考虑延长到 30s 或引入 Redisson 看门狗机制。

### 1.3 封禁执行事务一致性

**风险等级**: 高

**场景**: `BanServiceImpl.executeBan()` 依次执行：
1. 创建 BanRecord（DB）
2. 更新 User 表（DB）
3. 写 Redis 黑名单 ZSET
4. 写 Redis 封禁状态缓存

如果步骤 3/4 失败（Redis 异常），DB 已提交但缓存未更新，导致：
- 用户在封禁检查时仍能通过（缓存未命中 → 查 DB → 正常拦截，降级放行兜底）
- 黑名单 ZSET 缺失该用户，定时任务不会自动解封

**建议**:
1. 在事务提交后（`@TransactionalEventListener`）异步写 Redis，失败时重试
2. 或者定时任务增加兜底扫描：每 30 分钟扫描 DB 中 `banStatus != NONE` 但不在 ZSET 中的用户

### 1.4 自动解封并发竞争

**风险等级**: 中

**场景**: 定时任务 `autoUnbanExpiredUsers()` 在多实例部署时可能同时执行，导致同一用户被多次解封（重复更新 DB、重复发送通知）。

**现状**: 已使用 Redis 分布式锁 `lock:ban:auto-unban`（TTL 4 分钟），解决了多实例竞争问题。

**遗留风险**: ZRANGEBYSCORE + 逐条处理之间，如果某条解封操作失败（DB 超时），后续用户的解封会被阻塞。

**建议**: 改为批量处理，单个失败不影响其他用户：
```java
for (Long userId : expiredUserIds) {
    try {
        banService.unbanUser(userId); // 单独 try-catch
    } catch (Exception e) {
        log.error("解封用户 {} 失败", userId, e);
    }
}
```

### 1.5 LoginInterceptor 封禁检查的热点 Key

**风险等级**: 高

**场景**: 每次请求都经过 `LoginInterceptor.checkBanStatus()`，对 Redis `ban:status:{userId}` 进行 GET 操作。高并发下，如果大量被封禁用户的请求同时到达，会对 Redis 产生大量读压力。

**分析**:
- 正常用户（banStatus=NONE）在代码中先检查 User 实体的 `banStatus` 字段（内存判断），不会打到 Redis
- 只有被标记为封禁的用户才会查 Redis
- 被封禁用户登录后直接被拦截，Session 可能已被清除

**实际影响**: 极低。被封禁用户的请求在 `preHandle` 就被拦截，不会产生持续流量。

### 1.6 举报限流 INCR 竞态

**风险等级**: 低

**场景**: `rate_limit:report:{userId}` 使用 INCR 计数 + EXPIRE 两步操作。

**分析**: INCR 是原子操作。首次创建 Key 时 INCR 返回 1，此时设置 EXPIRE 是安全的。但极端情况下（INCR 成功后进程崩溃），Key 可能没有 TTL 导致永久限流。

**建议**: 使用 Lua 脚本保证 INCR + EXPIRE 原子性，或使用 `stringRedisTemplate.opsForValue().increment()` 后立即判断并设置 TTL（当前实现已处理此场景）。

---

## 二、海量数据问题

### 2.1 report 表查询性能

**风险等级**: 高

**场景**: 举报表随时间增长，管理端列表查询 `getAdminReportList()` 按 `reportCount DESC, createTime DESC` 排序。

**现有索引**:
- `idx_status_count(status, reportCount DESC, createTime)` — 覆盖按状态过滤+排序
- `idx_target(targetType, targetId)` — 按目标查询

**潜在问题**:
- 无状态过滤的全表排序（`ORDER BY reportCount DESC` 无 WHERE 条件）会使用 filesort
- `idx_reporter_dup(reporterId, targetType, targetId)` 用于防重复，无排序支持

**建议**:
1. 管理端列表必须带 status 过滤条件，避免全表扫描
2. 对于按举报次数排序的需求，考虑增加汇总表 `report_target_summary`:
   ```sql
   CREATE TABLE report_target_summary (
     targetType VARCHAR(20),
     targetId BIGINT,
     totalReportCount INT,
     pendingCount INT,
     lastReportTime DATETIME,
     PRIMARY KEY (targetType, targetId),
     KEY idx_pending_count (pendingCount DESC)
   );
   ```
3. 定时任务同步汇总数据，管理端直接查汇总表

### 2.2 feedback 表查询性能

**风险等级**: 中

**现有索引已覆盖主要查询场景**:
- `idx_feedback_user(userId, isDelete)` — 个人列表
- `idx_feedback_status(status, isDelete)` — 状态过滤
- `idx_feedback_handler(handlerId, status, isDelete)` — 处理人查看
- `idx_feedback_type_priority(type, priority, isDelete)` — 类型+优先级
- `idx_feedback_create_time(createTime DESC)` — 时间排序

**潜在问题**: 管理端 `getAdminFeedbackList()` 先按 priority ASC 再按 createTime DESC 排序，需要 filesort。

**建议**: priority 只有 4 个值（P0-P3），可以用 UNION ALL 分 4 个子查询分别查，然后合并结果。或接受 filesort，配合分页限制（每页 ≤ 50 条）影响可控。

### 2.3 ban_record 表增长

**风险等级**: 低

**分析**: ban_record 表按用户封禁次数增长，增长速度远低于 report/feedback。每个用户被封禁次数有限（3 次后永久封禁），预计数据量可控。

**现有索引**: `idx_user_time(userId, banStartTime DESC)` 支持按用户查询，`idx_unbanned_end(unbanned, banEndTime)` 支持过期查询。

### 2.4 feedback_status_log 表增长

**风险等级**: 中

**场景**: 每次反馈状态变更都插入一条日志。对于活跃反馈，日志量可控。但长期运行后，表可能积累大量数据。

**建议**:
1. 超过 1 年的状态日志可归档到 `feedback_status_log_archive` 表
2. 或在反馈关闭 6 个月后删除对应日志（保留在主表仅供统计）

### 2.5 feedback_attachment 表

**风险等级**: 低

**分析**: 每个反馈最多 3 个附件，数据量 = 反馈数 × 3。OSS 文件存储成本才是主要考量，而非 DB 记录数。

### 2.6 Redis 内存占用

**风险等级**: 中

**场景**:
- `ban:blacklist` ZSET: 每个被封禁用户一条记录（member=userId, score=endTime），永久封禁用户 score=0 永远不会被清理
- `report:dup:{reporterId}:{targetType}:{targetId}`: 7 天 TTL，大量举报时占用较多
- `feedback:auto:close:check` ZSET: 持久存储，不断增长

**建议**:
1. `ban:blacklist` 添加定期清理任务：ZREMRANGEBYSCORE 清理已解封超过 30 天的记录
2. `feedback:auto:close:check` 在自动关闭后及时 ZREM 已处理的记录
3. 监控 Redis 内存使用，设置 maxmemory-policy 为 allkeys-lru

---

## 三、关键并发场景总结

| 场景 | 风险 | 当前防护 | 建议改进 |
|------|------|---------|---------|
| 举报重复提交 | 中 | Redis SETNX 原子操作 | 已足够 |
| 反馈重复提交 | 低 | Redis 锁 10s | 已足够 |
| 封禁执行一致性 | 高 | DB 事务 + Redis 写入 | 增加事务后事件异步写入 |
| 自动解封多实例 | 中 | Redis 分布式锁 | 已足够，增加单条 try-catch |
| 举报限流 | 低 | Redis INCR + EXPIRE | 考虑 Lua 脚本原子化 |
| 封禁检查热点 | 低 | 内存快速判断 + Redis 缓存 | 已足够 |
| 违规计数重置 | 低 | 分布式锁 + 批量更新 | 增加分页处理避免大事务 |

## 四、海量数据场景总结

| 表/数据 | 预估增速 | 主要风险 | 建议措施 |
|---------|---------|---------|---------|
| report 表 | 高（取决于用户量） | 列表查询排序 | 增加汇总表 |
| feedback 表 | 中 | 管理端排序 | 接受 filesort + 分页限制 |
| feedback_status_log | 中低 | 长期增长 | 定期归档 |
| ban_record | 低 | — | 定期归档历史数据 |
| Redis ZSET | 中 | 内存占用 | 定期清理过期记录 |

---

## 五、建议的后续优化项

1. **P0**: 封禁执行的 DB+Redis 一致性改为事务后异步事件模式
2. **P1**: report 表增加 `report_target_summary` 汇总表，优化管理端列表性能
3. **P1**: 自动解封增加单条失败隔离（try-catch per user）
4. **P2**: 状态日志表增加归档策略（关闭超过 6 个月的反馈日志归档）
5. **P2**: Redis 黑名单 ZSET 增加定期清理策略
6. **P3**: 举报限流改用 Lua 脚本保证 INCR + EXPIRE 原子性
