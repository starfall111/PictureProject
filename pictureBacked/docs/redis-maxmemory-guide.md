# Redis 内存限制配置指南

## 1. 查看当前配置

```bash
# 查看 maxmemory 设置（0 表示无限制）
redis-cli -a 125801 CONFIG GET maxmemory

# 查看淘汰策略
redis-cli -a 125801 CONFIG GET maxmemory-policy

# 查看实际内存使用情况
redis-cli -a 125801 INFO memory
```

## 2. 修改 maxmemory

### 2.1 运行时热修改（立即生效）

```bash
# 设置内存上限（根据服务器内存调整）
redis-cli -a 125801 CONFIG SET maxmemory 1gb

# 设置淘汰策略
redis-cli -a 125801 CONFIG SET maxmemory-policy allkeys-lru

# 持久化到配置文件（避免重启后丢失）
redis-cli -a 125801 CONFIG REWRITE
```

### 2.2 修改配置文件（需重启）

找到 Redis 配置文件，直接编辑：

| 安装方式 | 配置文件路径 |
|----------|-------------|
| Windows 服务 | `安装目录\redis.windows-service.conf` |
| Windows 命令行 | `安装目录\redis.windows.conf` |
| WSL | `/etc/redis/redis.conf` |
| Memurai | `C:\Program Files\Memurai\memurai.conf` |

在配置文件中添加或修改：

```conf
maxmemory 1gb
maxmemory-policy allkeys-lru
```

修改后重启 Redis 服务：

```bash
net stop Redis
net start Redis
```

### 2.3 确认配置文件路径

```bash
redis-cli -a 125801 CONFIG GET config_file
```

## 3. 容量规划参考

| 服务器内存 | 建议 maxmemory | 说明 |
|-----------|---------------|------|
| 2GB | 1GB | 留给 OS、MySQL、Java 等进程 |
| 4GB | 1.5GB ~ 2.5GB | |
| 8GB | 4GB ~ 6GB | |
| 16GB | 8GB ~ 12GB | |

**经验法则**：设为物理内存的 50%~70%。

## 4. 淘汰策略说明

本项目使用 `allkeys-lru`，理由：

- 大量缓存数据（CachedXxxServiceImpl）丢失后可从数据库重建
- Session 访问频繁，不会被 LRU 淘汰
- 秒杀库存数据短时效，过期自动清理

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| `allkeys-lru` | 所有 key 中淘汰最久未使用的 | **纯缓存（推荐）** |
| `volatile-lru` | 只淘汰设了 TTL 的 key | 部分数据不能丢 |
| `allkeys-random` | 随机淘汰 | 无明显冷热特征 |
| `volatile-ttl` | 淘汰 TTL 最短的 | 需要优先保留长效缓存 |
| `noeviction` | 内存满直接报错 | 数据绝不能丢 |

## 5. 监控与维护

### 5.1 日常监控

```bash
# 查看内存概览
redis-cli -a 125801 INFO memory
```

重点关注：

| 指标 | 含义 | 警戒值 |
|------|------|--------|
| `used_memory` | Redis 实际使用内存 | > 80% maxmemory |
| `used_memory_rss` | OS 分配的内存（含碎片） | > 1.5x used_memory |
| `mem_fragmentation_ratio` | 内存碎片率 | > 1.5 需关注 |

### 5.2 碎片整理

```bash
# 开启自动碎片整理
redis-cli -a 125801 CONFIG SET activedefrag yes

# 持久化
redis-cli -a 125801 CONFIG REWRITE
```

### 5.3 查看 key 数量与占用

```bash
# 查看 database 5 的 key 数量
redis-cli -a 125801 -n 5 DBSIZE

# 查看 key 分布（慎用，key 多时较慢）
redis-cli -a 125801 -n 5 --scan --pattern "myapp:*" | head -20
```

## 6. 常见问题

### Q: 修改后重启 Redis 内存限制消失了？

运行 `CONFIG REWRITE` 将运行配置写入配置文件，否则只存在内存中。

### Q: 内存满了会怎样？

设置了 `allkeys-lru` 后，Redis 会自动淘汰最久未访问的 key，新写入正常进行。
如果没设 maxmemory（值为 0），Redis 会一直占内存直到 OS OOM。

### Q: 开发环境建议设多少？

256MB ~ 512MB 足够。生产环境根据用户量和缓存策略调整。

## 7. 宕机场景与解决方案

> 本项目 QPS 约 500，Redis 自身压力不大，主要风险来自 2GB 服务器内存紧张和 Windows 平台稳定性。

### 7.1 内存耗尽导致 OOM Kill

**表现**：Redis 进程消失，服务直接不可用

```bash
# 检查是否被 OS OOM kill
dmesg | grep -i "killed process"
```

**原因**：maxmemory 未限制（值为 0）或淘汰策略为 `noeviction`

**解决**：配置 `maxmemory 1gb` + `allkeys-lru`（已在第 2 节完成）

### 7.2 RDB 持久化 fork 失败

**表现**：Redis 日志报 `Cannot allocate memory for fork`，之后停止响应

**原因**：RDB 快照时 `fork()` 子进程需复制页表，内存紧张时会失败

**解决方案 A**：允许 fork 失败时继续写入

```bash
redis-cli -a 125801 CONFIG SET stop-writes-on-bgsave-error no
redis-cli -a 125801 CONFIG REWRITE
```

**解决方案 B**：直接关闭 RDB（推荐，本项目缓存数据均可从数据库重建）

```bash
redis-cli -a 125801 CONFIG SET save ""
redis-cli -a 125801 CONFIG REWRITE
```

### 7.3 AOF 重写阻塞

**表现**：AOF rewrite 期间 Redis 卡住，客户端超时

**排查**：

```bash
# 检查是否开启了 AOF
redis-cli -a 125801 CONFIG GET appendonly
```

**解决**：本项目缓存可重建，建议关闭 AOF 减少 IO 压力

```bash
redis-cli -a 125801 CONFIG SET appendonly no
redis-cli -a 125801 CONFIG REWRITE
```

### 7.4 大 Key 导致阻塞

**表现**：周期性卡顿，某些操作耗时异常

**排查**：

```bash
# 扫描大 key
redis-cli -a 125801 --bigkeys

# 查看指定 key 的大小
redis-cli -a 125801 DEBUG OBJECT <key>
```

**本项目风险点**：

| 服务 | 风险 |
|------|------|
| `CachedFeedServiceImpl` | 动态列表缓存可能过大 |
| `CachedPictureServiceImpl` | 图片列表一次性缓存过多数据 |

**解决**：分页缓存，每页一个 key，单个 value 控制在 10KB 以内

### 7.5 客户端连接耗尽

**表现**：报错 `max number of clients reached`

**排查**：

```bash
# 查看最大连接数
redis-cli -a 125801 CONFIG GET maxclients

# 查看当前连接数
redis-cli -a 125801 CLIENT LIST | wc -l
```

**本项目情况**：连接池 `max-active: 50`，QPS 500 下不会触发。如需调整：

```bash
redis-cli -a 125801 CONFIG SET maxclients 1000
redis-cli -a 125801 CONFIG REWRITE
```

### 7.6 Windows 平台不稳定

**表现**：Redis 无故退出，无明显错误日志

**原因**：Windows 版 Redis（微软分支）已停止维护，存在内存泄漏和稳定性问题

**长期方案**：

| 方案 | 推荐度 | 说明 |
|------|--------|------|
| [Memurai](https://www.memurai.com/) | 高 | Windows 原生替代，持续维护 |
| WSL2 + Linux Redis | 高 | 与生产环境一致，最稳定 |
| 生产环境用 Linux 部署 Redis | 高 | 最终方案，性能和稳定性最佳 |
| 升级服务器内存到 4GB+ | 中 | 缓解内存压力，治标不治本 |

## 8. 本项目 Redis 优化清单

| 优先级 | 操作 | 状态 |
|--------|------|------|
| P0 | 配置 maxmemory + allkeys-lru | 已完成 |
| P1 | 关闭 RDB 持久化（缓存可重建） | 待执行 |
| P1 | 关闭 AOF 持久化（缓存可重建） | 待执行 |
| P2 | 排查大 key，列表缓存改为分页 | 待执行 |
| P3 | 迁移到 Linux 或 Memurai | 长期规划 |
