# toggleLike 接口性能排查与优化报告

## 1. 问题背景

**现象**：`toggleLike`（点赞/取消点赞）接口在并发量超过 320 时响应速度明显变慢。

**接口入口**：`POST /api/picture/like/{pictureId}` → `PictureController.toggleLike` → `CachedSocialServiceImpl.toggleLike`

**已有监控**：方法标注了 `@RedisTimed(value = "toggleLike", warnThreshold = 100)`，超过 100ms 会自动打印 WARN 日志。

---

## 2. 排查过程

### 2.1 梳理 toggleLike 热路径调用链

逐行分析 `CachedSocialServiceImpl.toggleLike` 的执行流程：

```
用户请求
  │
  ├─ validPicturePublic(pictureId)          ← 每次查 DB
  │    └─ pictureMapper.selectById(id)
  │
  ├─ Redis SETNX 获取分布式锁               ← 1 RTT
  │
  ├─ Pipeline 批量读取当前状态               ← 1 RTT
  │    ├─ GET likeKey
  │    └─ EXISTS statsKey
  │
  ├─ [条件] Hash 未初始化 → selectById 查 DB  ← 可能再查一次 DB
  │
  ├─ Pipeline 批量写入                       ← 1 RTT
  │    ├─ PUT Hash / SET likeKey / INCR / SADD
  │    └─ GET likeCount
  │
  ├─ sendSocialActionMessage()              ← MQ 异步，快
  │
  ├─ deleteByPattern("list:liked:{uid}:*")  ← SCAN 全库！
  │
  └─ Lua 脚本释放锁                         ← 1 RTT
```

### 2.2 识别瓶颈

对照 `application.yml` 配置逐项排查：

#### 瓶颈 1（最大嫌疑）：HikariCP 连接池耗尽

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 只有 20 个 DB 连接
      connection-timeout: 10000    # 等待连接最多 10 秒
```

`validPicturePublic` 每次都执行 `pictureMapper.selectById(pictureId)` 查数据库。320 并发请求涌入时，20 个连接瞬间用完，剩余 300 个线程阻塞在连接池等待队列中。

**判断依据**：不是 Redis 慢，是线程排队等数据库连接。即使后续的 Redis 操作很快，前面的 DB 等待已经拉高了总耗时。

#### 瓶颈 2：deleteByPattern 的 SCAN 开销

```java
// CachedSocialServiceImpl.java:195
redisCacheUtil.deleteByPattern(String.format("list:liked:%d:*", userId));
```

`deleteByPattern` 内部实现：

```java
// RedisCacheUtil.java
public void deleteByPattern(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
    try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
        cursor.forEachRemaining(keys::add);     // 遍历整个 keyspace
    }
    if (!keys.isEmpty()) {
        stringRedisTemplate.delete(keys);       // 批量删除
    }
}
```

虽然用的是 SCAN 而非 KEYS（不阻塞 Redis 单线程），但 SCAN 仍然要**遍历整个 keyspace 进行模式匹配**。320 并发意味着 320 个 SCAN 同时运行，如果 Redis 中总 key 数量很大（数十万级），每次 SCAN 的迭代次数非常多。

#### 瓶颈 3：Lettuce 连接池配置过高

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 1000    # 严重过高
          max-idle: 1000
          min-idle: 200
```

Lettuce 与 Jedis 不同，每条 TCP 连接是多路复用的，官方建议连接池大小 **1~10** 即可。配置 1000 意味着可能建立上千条 TCP 连接，Redis 单线程需要在它们之间做 I/O 调度，反而增加开销。

### 2.3 瓶颈严重程度排序

| 优先级 | 瓶颈 | 影响 | 修复难度 |
|--------|------|------|----------|
| P0 | HikariCP 20 连接池耗尽 | 300/320 线程阻塞等连接 | 中（加 Redis 缓存） |
| P1 | deleteByPattern SCAN | 每次点赞触发全库扫描 | 低（版本号替代） |
| P2 | Lettuce max-active 过高 | Redis 调度压力增大 | 低（改配置） |

---

## 3. 优化方案

### 3.1 优化一：缓存 validPicturePublic（消除热点 DB 查询）

**思路**：图片的公开状态（是否存在、是否属于公共图库）几乎不变，适合 Redis 缓存。

**实现**：

```
缓存 Key:  pic:public:{pictureId}
缓存 Value: Picture 对象的 JSON 字符串
特殊标记:  "DELETED" → 图片不存在 / "PRIVATE" → 私有空间图片
TTL:       10 分钟 + 随机抖动（防雪崩）
```

**读取流程**：

```
validPicturePublic(pictureId)
    │
    ├─ Redis GET pic:public:{id}
    │   ├─ 命中 "DELETED" → 直接抛异常（不查 DB）
    │   ├─ 命中 "PRIVATE" → 直接抛异常（不查 DB）
    │   ├─ 命中 JSON     → 反序列化返回 Picture 对象
    │   └─ 未命中         → 查 DB → 回填缓存 → 返回
    │
    └─ Redis 异常 → 降级到 DB 查询（不影响核心功能）
```

**效果**：10 分钟 TTL 窗口内，同一图片的所有社交操作（点赞/收藏/分享/浏览/下载）都走 Redis，不再消耗 DB 连接。320 并发下，DB 连接池压力大幅降低。

### 3.2 优化二：版本号缓存失效（消除 SCAN 开销）

**思路**：不再主动删除旧缓存 key，而是通过版本号让旧 key 自动过期失效。

**改动前后对比**：

```
改动前（写入端 - toggleLike）：
  redisCacheUtil.deleteByPattern("list:liked:{userId}:*")    ← SCAN 全库 O(N)

改动后（写入端 - toggleLike）：
  incrListVersion("list:liked:version:{userId}")              ← INCR O(1)
```

```
改动前（读取端 - getUserLikedPictures）：
  cacheKey = "list:liked:{userId}:{md5}"

改动后（读取端 - getUserLikedPictures）：
  version = GET "list:liked:version:{userId}"   // 不存在默认 "0"
  cacheKey = "list:liked:{userId}:v{version}:{md5}"
```

**版本号机制原理**：

```
点赞时：INCR version: 5 → 6（1 次 Redis 命令）
                     ↓
    旧 key list:liked:123:v5:* 不再被读取，TTL 到期自动删除

查询时：GET version → 6
    读 list:liked:123:v6:{md5}  → 命中返回 / 未命中回源 DB
```

**效果**：从 O(N) 的 SCAN + DEL 降级为 O(1) 的 INCR，旧 key 靠 300-480s 业务 TTL 自然过期。

---

## 4. 改动清单

### 4.1 RedisKeyConstants.java（新增 7 个常量）

| 常量 | 值 | 用途 |
|------|---|------|
| `PICTURE_PUBLIC_KEY` | `pic:public:%d` | 图片公开状态缓存 |
| `PICTURE_PUBLIC_TTL_BASE` | 600s (10min) | 基础 TTL |
| `PICTURE_PUBLIC_TTL_JITTER` | 120s (2min) | 抖动防雪崩 |
| `LIST_LIKED_VERSION_KEY` | `list:liked:version:%d` | 点赞列表版本号 |
| `LIST_FAV_VERSION_KEY` | `list:fav:version:%d` | 收藏列表版本号 |
| `LIST_VERSION_TTL` | 30 天 | 版本号 key TTL |
| `LIST_LIKED_KEY` | `list:liked:%d:v%s:%s` | 含版本号的缓存 key |
| `LIST_FAV_KEY` | `list:fav:%d:v%s:%s` | 含版本号的缓存 key |

### 4.2 CachedSocialServiceImpl.java（3 处核心改造）

| 方法 | 改动 |
|------|------|
| `validPicturePublic` | 加入 Redis 缓存读取 + 降级逻辑 |
| `toggleLike` | `deleteByPattern` → `incrListVersion` |
| `toggleFavorite` | `deleteByPattern` → `incrListVersion` |
| `getUserLikedPictures` | 读取版本号拼入 cache key |
| `getUserFavoritedPictures` | 读取版本号拼入 cache key |
| 新增 `incrListVersion` | O(1) INCR 递增版本号 |
| 新增 `getListVersion` | 读取版本号，不存在默认 "0" |

---

## 5. 优化后调用链

```
用户请求
  │
  ├─ validPicturePublic(pictureId)          ← Redis 缓存命中 ~1ms
  │    └─ GET pic:public:{id}               （不再查 DB）
  │
  ├─ Redis SETNX 获取分布式锁               ← 1 RTT
  │
  ├─ Pipeline 批量读取                       ← 1 RTT
  │
  ├─ [条件] Hash 未初始化 → 查 DB             ← 首次访问才触发
  │
  ├─ Pipeline 批量写入                       ← 1 RTT
  │
  ├─ MQ 异步写 DB                            ← 不阻塞
  │
  ├─ INCR list:liked:version:{uid}          ← O(1)，替代 SCAN
  │
  └─ Lua 脚本释放锁                         ← 1 RTT
```

**热路径开销**：3-4 次 Redis RTT（约 3-5ms），0 次 DB 查询，0 次 SCAN。

---

## 6. 待办事项

- [ ] 调整 Lettuce 连接池配置（`max-active` 从 1000 降至 10-20）
- [ ] 调整 HikariCP 连接池大小（从 20 提升至 40-50 作为兜底）
- [ ] 图片删除/修改时主动失效 `pic:public:{id}` 缓存
- [ ] 压测验证 320+ 并发下接口响应时间
- [ ] 关注 Lettuce `max-active: 1000` 过高的问题，建议降至合理值
