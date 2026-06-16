# JMeter 秒杀压测完整配置指南

## 一、前期准备

### 1. 准备测试用户

```sql
SELECT id FROM user WHERE isDelete = 0 AND banStatus = 'NONE' LIMIT 1000;
```

### 2. 创建用户数据文件 `userIds.csv`

放在 JMeter 脚本同目录下：

```
userId
1
2
3
4
5
...
```

### 3. 启动后端

```bash
java -jar app.jar --spring.profiles.active=benchmark
```

### 4. 确认秒杀批次

确保数据库中存在状态为 `2`（进行中）的秒杀批次，记下 `batchId`。

---

## 二、JMeter 配置步骤

### Step 1: 创建 Test Plan

打开 JMeter → 右键左侧树 → **新建 Test Plan**

### Step 2: 添加用户定义变量

右键 Test Plan → 添加 → 配置元件 → **用户定义的变量**

| 名称 | 值 | 说明 |
|------|-----|------|
| `server` | `localhost` | 服务器地址 |
| `port` | `4040` | 端口 |
| `batchId` | `100` | 秒杀批次ID（改成你的实际值） |

### Step 3: 添加线程组

右键 Test Plan → 添加 → 线程(用户) → **线程组**

```
名称:                          秒杀压测线程组
Number of Threads (线程数):    ${__P(threads,500)}
Ramp-Up Period (启动时间/s):    ${__P(rampup,10)}
Loop Count (循环次数):          ${__P(loops,1)}
```

> 500 线程、10 秒内启动、每线程执行 1 次 = 模拟 500 用户同时抢购

### Step 4: 添加 CSV 数据文件设置

右键线程组 → 添加 → 配置元件 → **CSV 数据文件设置**

```
文件名:              userIds.csv
变量名称:            userId
忽略首行:            True
分隔符:              ,
循环结束时停止线程:    False
线程共享模式:         当前线程组
```

### Step 5: 添加 HTTP 信息头管理器

右键线程组 → 添加 → 配置元件 → **HTTP 信息头管理器**

| 名称 | 值 |
|------|-----|
| `Content-Type` | `application/json` |

### Step 6: 添加 HTTP 请求 — 获取 Token

右键线程组 → 添加 → 取样器 → **HTTP 请求**

```
名称:         获取Token
协议:         http
服务器名称:    ${server}
端口号:       ${port}
方法:         GET
路径:         /api/benchmark/seckill/token
参数:
  userId   = ${userId}
  batchId  = ${batchId}
```

### Step 7: 为获取 Token 添加 JSON 提取器

右键「获取Token」→ 添加 → 后置处理器 → **JSON 提取器**

```
Names of created variables:     seckillToken
JSON Path expressions:          $.data.token
Match No.:                      0
Default Values:                 TOKEN_FAILED
```

### Step 8: 为获取 Token 添加 JSON 断言（可选）

右键「获取Token」→ 添加 → 断言 → **JSON 断言**

```
Assert JSON Path:    $.code
Expected Value:      0
勾选 "Additionally assert value"
```

### Step 9: 添加 HTTP 请求 — 执行秒杀

右键线程组 → 添加 → 取样器 → **HTTP 请求**

```
名称:         执行秒杀
协议:         http
服务器名称:    ${server}
端口号:       ${port}
方法:         POST
路径:         /api/benchmark/seckill/grab
Body Data:
{
  "userId": ${userId},
  "batchId": ${batchId},
  "token": "${seckillToken}"
}
```

### Step 10: 为秒杀添加 JSON 提取器

右键「执行秒杀」→ 添加 → 后置处理器 → **JSON 提取器**

```
Names of created variables:     orderNo
JSON Path expressions:          $.data.orderNo
Match No.:                      0
Default Values:                 ORDER_FAILED
```

### Step 11: 为秒杀添加响应断言

右键「执行秒杀」→ 添加 → 断言 → **响应断言**

```
字段检查:       响应文本
模式匹配规则:   包含
模式:          "code":0
```

### Step 12: 添加调试取样器（调试用，压测时禁用）

右键线程组 → 添加 → 取样器 → **Debug Sampler**

```
JMeter Variables: True
其余: False
```

### Step 13: 添加监听器

右键线程组 → 添加 → 监听器，分别添加：

| 监听器 | 用途 |
|--------|------|
| **察看结果树** | 调试阶段查看每个请求的请求/响应详情 |
| **聚合报告** | 查看 TPS、平均 RT、错误率、P90/P95/P99 |
| **汇总报告** | 总体统计摘要 |
| **响应时间图** | RT 随时间变化的趋势图 |

---

## 三、完整层级结构

```
Test Plan (秒杀压测)
├── 用户定义的变量
│     server=localhost, port=4040, batchId=100
│
├── 线程组 (秒杀压测线程组)
│     threads=${__P(threads,500)}, rampup=${__P(rampup,10)}, loops=${__P(loops,1)}
│
│   ├── CSV 数据文件设置
│   │     userIds.csv → userId
│   │
│   ├── HTTP 信息头管理器
│   │     Content-Type: application/json
│   │
│   ├── ────── 取样器 1: 获取 Token ──────
│   │   HTTP Request (GET /api/benchmark/seckill/token)
│   │     ├── JSON 提取器 → seckillToken ($.data.token)
│   │     └── JSON 断言   → $.code == 0
│   │
│   ├── ────── 取样器 2: 执行秒杀 ──────
│   │   HTTP Request (POST /api/benchmark/seckill/grab)
│   │     ├── JSON 提取器 → orderNo ($.data.orderNo)
│   │     └── 响应断言     → 包含 "code":0
│   │
│   └── Debug Sampler (调试用，压测时禁用)
│
├── 察看结果树
├── 聚合报告
├── 汇总报告
└── 响应时间图
```

---

## 四、调试验证

### 1. 先用 1 线程 1 循环测试

```
线程数: 1
Ramp-Up: 1
循环次数: 1
```

点击 **启动**，查看「察看结果树」：

**获取 Token 期望响应：**
```json
{
  "code": 0,
  "data": {
    "token": "MQ...base64编码的token",
    "expireIn": 300
  }
}
```

**执行秒杀期望响应：**
```json
{
  "code": 0,
  "data": {
    "orderNo": "SK20260608...",
    "status": "PENDING"
  }
}
```

两个请求均返回 `"code":0` 说明链路通畅，可开始正式压测。

### 2. 检查 Redis 库存

```
GET http://localhost:4040/api/benchmark/seckill/stats?batchId=100
```

确认 `stock` 有值、`version` 为 0。

---

## 五、正式压测

### GUI 模式（少量并发）

直接在 JMeter GUI 中修改线程数，点击启动。

### 命令行模式（推荐，大量并发）

```bash
# 500 并发，10 秒启动，每用户抢 1 次
jmeter -n -t seckill_benchmark.jmx \
  -Jthreads=500 \
  -Jrampup=10 \
  -Jloops=1 \
  -l result_500.jtl \
  -e -o report_500/

# 1000 并发
jmeter -n -t seckill_benchmark.jmx \
  -Jthreads=1000 \
  -Jrampup=20 \
  -Jloops=1 \
  -l result_1000.jtl \
  -e -o report_1000/

# 2000 并发
jmeter -n -t seckill_benchmark.jmx \
  -Jthreads=2000 \
  -Jrampup=30 \
  -Jloops=1 \
  -l result_2000.jtl \
  -e -o report_2000/
```

> `-n` 非GUI模式 | `-t` 脚本文件 | `-J` 覆盖变量 | `-l` 结果日志 | `-e -o` 生成 HTML 报告

---

## 六、压测监控要点

### 1. JMeter 聚合报告关注指标

| 指标 | 说明 | 健康阈值 |
|------|------|---------|
| **Throughput (TPS)** | 每秒处理请求数 | 越高越好 |
| **Average (平均RT)** | 平均响应时间 | < 200ms |
| **90% Line** | 90% 请求的响应时间 | < 500ms |
| **99% Line** | 99% 请求的响应时间 | < 1000ms |
| **Error %** | 错误率 | 售罄错误正常，系统错误应 < 1% |

### 2. 后端监控

```bash
# 实时查看 Redis 剩余库存（压测期间反复调用）
curl http://localhost:4040/api/benchmark/seckill/stats?batchId=100
```

RabbitMQ Management UI：浏览器访问 `http://localhost:15672`，查看 `seckill_queue` 的 Ready / Unacked 消息数。

### 3. 压测后数据一致性校验

```sql
-- 检查订单总数是否与库存扣减一致
SELECT status, COUNT(*) FROM seckill_order
WHERE batch_id = 100
GROUP BY status;
-- status: 0=处理中, 1=成功, 2=失败

-- 检查是否超卖
SELECT COUNT(*) FROM seckill_order
WHERE batch_id = 100 AND status IN (0, 1);
-- 应 <= 批次初始库存
```

---

## 七、常见问题排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 所有请求返回 `NOT_LOGIN_ERROR` | InterceptorConfig 没加排除路径 / profile 不是 benchmark | 检查启动参数和配置 |
| token 返回 `TOKEN_FAILED` | 批次不存在或 batchId 错误 | 调用 `/init` 预热或检查 batchId |
| grab 返回 `"活动尚未开始"` | 批次状态不是 2 或不在时间范围内 | 修改批次起止时间和状态 |
| 大量 `"请勿重复参与"` | CSV 中 userId 不够，线程循环复用了同一 userId | 准备足够多的测试用户 |
| 大量 `"已售罄"` | 库存不足，正常现象 | 调大库存或减少并发数 |
| grab RT 突然飙高 | Redis/MQ 连接池耗尽 | 检查 `application-benchmark.yml` 连接池配置 |

---

## 八、秒杀链路说明

```
JMeter
  │
  ├─ GET /benchmark/seckill/token?userId=X&batchId=Y
  │    └→ SeckillService.getToken() → HMAC 签发
  │
  ├─ POST /benchmark/seckill/grab  { userId, batchId, token }
  │    └→ SeckillService.grab()
  │         ├─ 降级检查 (Redis)
  │         ├─ HMAC Token 验签
  │         ├─ 活动状态校验 (Redis → DB 降级)
  │         ├─ IP/用户限流 (Redis Lua 滑动窗口)
  │         ├─ 防重复下单 (Redis SET NX)
  │         ├─ Lua 原子扣库存 (Redis Hash)
  │         └─ MQ 异步下单 (RabbitMQ)
  │              └→ SeckillConsumer → 落库
  │
  └─ GET /benchmark/seckill/stats?batchId=Y
       └→ 读取 Redis 实时库存统计
```
