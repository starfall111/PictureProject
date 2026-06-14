# 缓存架构深度讨论文件

> 本文件记录需要深入讨论的缓存架构问题，待团队/个人充分理解后做出技术决策。

---

## 讨论项 11：toggleLike/toggleFavorite 的 DB 写操作在 Redis 锁内

### 现状

```java
// CachedSocialServiceImpl.toggleLike()
try {
    // 1. Redis 操作（快）
    stringRedisTemplate.opsForValue().set(likeKey, "1");
    stringRedisTemplate.opsForHash().increment(statsKey, "likeCount", 1);
    // 2. DB 操作（慢）← 在锁内
    pictureLikeMapper.insert(pictureLike);
} finally {
    stringRedisTemplate.delete(lockKey);  // 锁 TTL 10s
}
```

### 问题

1. DB 操作延迟会拉长锁持有时间，降低并发吞吐
2. 如果 DB 操作 > 10s，锁过期，另一个线程获取锁 → 两次 insert 或 insert+delete 交叉
3. DB 异常时 Redis 已经改了，数据不一致

### 讨论方向

| 方案 | 优点 | 缺点 |
|------|------|------|
| **A. 现状（同步双写+锁）** | 简单，一致性好 | 锁持有时间长 |
| **B. Redis 先行 + MQ 异步写 DB** | 锁持有极短，吞吐高 | 引入 MQ 组件，最终一致 |
| **C. Redis 先行 + 本地队列异步写 DB** | 不依赖外部 MQ | 本地队列重启丢数据 |
| **D. 延长锁 TTL（如 30s）** | 改动最小 | 治标不治本，吞吐更低 |

### 待决策

- 当前业务量级下 DB 操作是否真的会超过 10s？
- 是否接受最终一致性（方案 B/C）？
- 是否愿意引入消息队列中间件？

---

## 讨论项 12：PictureStatisticsSyncScheduled 的 UPSERT 原子性

### 现状

```java
// PictureStatisticsSyncScheduled.doSync()
int updated = pictureStatisticsMapper.updateById(stat);
if (updated == 0) {
    pictureStatisticsMapper.insert(stat);  // 可能 duplicate key
}
```

### 问题

1. `update → insert` 不原子，并发时可能两处都 insert → 主键冲突
2. 当前分布式锁 TTL 240s、任务间隔 300s，窗口很小但存在
3. MyBatis-Plus 的 `updateById` 依赖主键存在，首次同步新图片会走 insert

### 讨论方向

| 方案 | SQL | 说明 |
|------|-----|------|
| **A. INSERT ... ON DUPLICATE KEY UPDATE** | MySQL 原生 UPSERT | 最简洁，推荐 |
| **B. saveOrUpdate (MyBatis-Plus)** | 框架封装 | 底层也是先查后写，仍有竞态 |
| **C. 分布式锁 + 现有逻辑** | 不变 | 靠锁保证唯一执行，依赖锁可靠性 |

### 待决策

- MySQL 版本是否支持 `ON DUPLICATE KEY UPDATE`（5.7+ 均支持）？
- 是否有其他进程/定时任务可能同时写 `picture_statistics` 表？

---

## 讨论项 16：Redis 连接池配置

### 现状

```java
// RedisConfig.java — 仅创建默认 StringRedisTemplate
@Bean
public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
}
```

### 问题

1. Spring Boot 默认 Lettuce 连接池：无连接数上限配置
2. 无超时配置：命令执行慢时不中断
3. 无重试策略：Redis 短暂网络抖动直接报错
4. 单连接复用（Lettuce 默认）：高并发下成为瓶颈

### 讨论方向

```yaml
# 需要在 application.yml 中添加的配置
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 5
    lettuce:
      pool:
        max-active: 50      # 最大连接数
        max-idle: 20        # 最大空闲连接
        min-idle: 5         # 最小空闲连接
        max-wait: 3000ms    # 获取连接最大等待时间
      shutdown-timeout: 200ms
    timeout: 3000ms         # 命令超时
```

### 待决策

- 预估并发量级？连接池参数需要根据实际压测调整
- 是否需要配置 Redis Sentinel/Cluster（高可用）？
- 是否考虑从 Lettuce 切换到 Jedis（同步模型，调试更直观）？

---

## 讨论项 17：Write-Behind 统计数据的数据丢失风险

### 现状

```
用户浏览/下载/分享 → Redis INCR → 脏集合标记 → 每 5 分钟定时同步到 DB
```

### 问题

1. **应用崩溃**：脏集合中未同步的数据在下次启动时仍在 Redis 中（如果 Redis 存活），但如果 Redis 也崩溃或重启，数据永久丢失
2. **Redis 重启**：所有未同步的 viewCount/downloadCount/shareCount 增量永久丢失
3. **无 WAL / redo log**：没有预写日志，无法恢复丢失数据
4. **同步间隔越长，丢失窗口越大**

### 影响评估

| 数据类型 | 丢失影响 | 可接受？ |
|----------|---------|---------|
| viewCount | 浏览数偏低 | 可接受（非交易数据） |
| downloadCount | 下载数偏低 | 勉强接受 |
| shareCount | 分享数偏低 | 勉强接受 |
| likeCount | **不走 write-behind，实时双写** | 不受影响 |
| favoriteCount | **不走 write-behind，实时双写** | 不受影响 |

### 讨论方向

| 方案 | 优点 | 缺点 |
|------|------|------|
| **A. 接受风险（现状）** | 零成本 | 浏览/下载数据可能不准 |
| **B. 缩短同步间隔（如 1 分钟）** | 丢失窗口缩小 | Redis 和 DB 压力增大 |
| **C. AOF 持久化 + fsync everysec** | Redis 重启不丢数据 | 性能有损耗（~10%） |
| **D. RDB + AOF 混合持久化** | 兼顾性能和安全 | 配置复杂 |
| **E. 改为实时双写（与 like/fav 一致）** | 零丢失 | view/download 写入频率极高，DB 压力大 |

### 待决策

- 浏览/下载数据是否允许有少量偏差？
- Redis 当前持久化策略是什么？（默认通常不开启 AOF）
- 是否愿意为数据安全牺牲部分性能？

---

## 讨论项 18：缓存监控与可观测性

### 现状

- 无缓存命中率统计
- 无 Redis key 数量/内存监控
- 无慢查询日志
- 无告警机制

### 为什么需要监控

> 没有监控的缓存比没有缓存更危险——你不知道它在帮你还是在害你。

1. **缓存命中率**：如果命中率 < 50%，缓存可能反而在增加延迟
2. **Redis 内存**：无 TTL 的 key 持续增长，需要提前预警
3. **慢命令**：`KEYS`、`SMEMBERS` 大集合可能阻塞 Redis
4. **锁竞争**：分布式锁获取失败率反映并发压力

### 讨论方向

| 层面 | 工具 | 说明 |
|------|------|------|
| **Spring Boot Actuator + Micrometer** | 内置 | 暴露 metrics 端点，接入 Prometheus |
| **Redis INFO 命令** | 内置 | `used_memory`、`connected_clients`、`keyspace_hits/misses` |
| **自定义 CacheMetrics** | 开发 | 在 `RedisCacheUtil` 中统计 hit/miss/lock_fail |
| **Grafana 看板** | 运维 | 可视化缓存命中率、Redis 内存、命令延迟 |
| **告警** | 运维 | 内存 > 80%、命中率 < 30%、锁失败率 > 10% |

### 待决策

- 是否已有 Prometheus/Grafana 基础设施？
- 是否愿意先做一个最小化方案（自定义 metrics + 日志）？
- 优先级如何？（当前开发阶段可以先记录，上线前必须完善）

---

## 优先级建议

| 讨论项 | 紧迫度 | 建议时间点 |
|--------|--------|-----------|
| #11 DB 在锁内 | 中 | 上线前优化 |
| #12 UPSERT 原子性 | 高 | 下次迭代修复 |
| #16 连接池配置 | 高 | 上线前必须配置 |
| #17 数据丢失风险 | 中 | 根据业务要求决定 |
| #18 监控 | 中 | 上线前最小化方案，后续完善 |

---

## 决策记录（2026-05-29）

| 讨论项 | 决策 | 实施内容 |
|--------|------|---------|
| **#11 DB 在锁内** | **维持现状 + TODO** | 在 toggleLike/toggleFavorite 添加 TODO 注释，标注未来优化方向 |
| **#12 UPSERT 原子性** | **方案 A：INSERT ON DUPLICATE KEY UPDATE** | PictureStatisticsMapper 新增 `insertOrUpdate` 方法，替换原 update→insert 竞态逻辑 |
| **#16 连接池配置** | **已配置** | application.yml 添加 Lettuce 连接池（max-active=50, max-idle=20, min-idle=5）+ timeout=3s |
| **#17 数据丢失风险** | **缩短同步间隔 + TODO** | 同步间隔 5min→1min，锁 TTL 240s→50s；添加 TODO 提醒上线前确认 Redis AOF 持久化 |
| **#18 监控** | **最小化方案** | RedisCacheUtil 添加 hit/miss/lockFail 计数器，每 5 分钟输出统计日志 |
