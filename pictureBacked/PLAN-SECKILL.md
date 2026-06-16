# 秒杀编码券系统 — 完整落地方案

> 基于《基于秒杀服务和超卖问题的场景设计.md》的分析补充，覆盖技术架构、数据库设计、接口规范、前端配合、产品流程。

---

## 一、系统架构总览

```
                          ┌─────────────┐
                          │   前端 Vue   │
                          └──────┬──────┘
                                 │
                    ┌────────────▼────────────┐
                    │      Spring Boot 3.4     │
                    │   (Controller + Service)  │
                    └─┬──────┬──────┬──────┬───┘
                      │      │      │      │
               ┌──────▼┐ ┌───▼───┐  │  ┌───▼────┐
               │ Redis  │ │  MQ   │  │  │ MySQL  │
               │(Lua扣  │ │(异步  │  │  │(乐观锁 │
               │ 库存)  │ │ 写DB) │  │  │ 兜底)  │
               └───────┘ └───────┘  │  └────────┘
                                    │
                              ┌─────▼──────┐
                              │  WebSocket │
                              │ (结果推送)  │
                              └────────────┘
```

### 核心数据流

```
用户抢购 → 限流校验 → 幂等检查 → Redis Lua原子扣库存 → MQ异步消息 → 返回"抢购中"
                                                                    ↓
                                              MQ消费者 → DB写编码券 + 扣批次库存 + 写防重订单
                                                                    ↓
                                              WebSocket → 通知用户抢购结果
```

---

## 二、数据库设计（DDL）

### 2.1 发放批次表（新增）

```sql
CREATE TABLE code_coupon_batch (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no        VARCHAR(32) UNIQUE NOT NULL COMMENT '批次号，如 BATCH20260606001',
    name            VARCHAR(128) NOT NULL COMMENT '批次名称',
    type            TINYINT NOT NULL COMMENT '券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP',
    total_stock     INT NOT NULL COMMENT '总库存（发放总量）',
    current_stock   INT NOT NULL DEFAULT 0 COMMENT '当前剩余库存',
    start_time      DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time        DATETIME COMMENT '秒杀结束时间（NULL表示发完即止）',
    status          TINYINT DEFAULT 0 COMMENT '0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT DEFAULT 0,
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status_time (status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码券发放批次';
```

### 2.2 编码券表（优化）

```sql
CREATE TABLE code_coupon (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id        BIGINT NOT NULL COMMENT '批次ID',
    user_id         BIGINT COMMENT '用户ID（未发放时为NULL）',
    code            VARCHAR(32) UNIQUE NOT NULL COMMENT '唯一编码券码',
    type            TINYINT NOT NULL COMMENT '3/7/30天',
    status          TINYINT DEFAULT 0 COMMENT '0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款',
    issued_at       DATETIME COMMENT '领取时间',
    activated_at    DATETIME COMMENT '激活（使用）时间',
    expire_at       DATETIME NOT NULL COMMENT '过期时间（领取后N天）',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_batch_user (batch_id, user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_status_expire (status, expire_at),
    FOREIGN KEY (batch_id) REFERENCES code_coupon_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码券';
```

### 2.3 秒杀订单/防重表（新增）

```sql
CREATE TABLE seckill_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    coupon_id       BIGINT COMMENT '编码券ID（异步写入后回填）',
    order_no        VARCHAR(32) UNIQUE NOT NULL COMMENT '订单号',
    status          TINYINT DEFAULT 0 COMMENT '0-处理中, 1-成功, 2-失败',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_batch (user_id, batch_id),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单（防重+状态追踪）';
```

---

## 三、Redis Key 设计规范

```java
// ==================== 秒杀模块 Key ====================

// 批次库存 Hash — Field: stock(int), version(int)
public static final String SECKILL_BATCH_STOCK = "seckill:batch:stock:%d";  // batchId

// 批次详情（预热数据）— STRING (JSON)
public static final String SECKILL_BATCH_INFO = "seckill:batch:info:%d";    // batchId

// 用户幂等 Token — STRING "1", TTL 5min
public static final String SECKILL_TOKEN = "seckill:token:%s";             // token值

// 防重标记 — STRING couponId, TTL 24h
public static final String SECKILL_DEDUPE = "seckill:dedupe:%d:%d";        // userId:batchId

// 分布式锁
public static final String LOCK_SECKILL_BATCH = "lock:seckill:batch:%d";   // batchId

// 降级开关 — STRING level(0-4)
public static final String SECKILL_DEGRADE = "seckill:degrade:level";

// IP 限流 — 资源: seckill_ip
// 复用已有 RateLimitUtil，窗口 60s, 最大 10次

// 用户限流 — 资源: seckill_user
// 复用已有 RateLimitUtil，窗口 3600s, 最大 3次

// Pexels API 配额监控
public static final String PEXELS_HOURLY_COUNT = "pexels:quota:hourly:%s"; // 日期小时
public static final String PEXELS_MONTHLY_COUNT = "pexels:quota:monthly:%s"; // 年月
```

---

## 四、核心接口设计

### 4.1 秒杀抢购接口

```
POST /api/seckill/grab
```

**请求参数：**
```json
{
    "batchId": 1,
    "token": "uuid-token-from-redis"
}
```

**响应（抢购中）：**
```json
{
    "code": 200,
    "message": "排队中，请等待结果",
    "data": {
        "orderNo": "SK20260606143000001",
        "status": "PENDING"
    }
}
```

**响应（失败场景）：**
```json
// 库存不足
{ "code": 4001, "message": "编码券已抢光" }

// 重复抢购
{ "code": 4002, "message": "您已参与过本轮抢购" }

// 限流
{ "code": 4291, "message": "当前使用人数过多，请稍后再试" }

// Token无效
{ "code": 4003, "message": "请求无效，请刷新页面重试" }

// 活动未开始/已结束
{ "code": 4004, "message": "活动未开始" }
{ "code": 4005, "message": "活动已结束" }
```

### 4.2 获取秒杀Token

```
GET /api/seckill/token?batchId=1
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "expireIn": 300
    }
}
```

### 4.3 查询抢购结果

```
GET /api/seckill/result?orderNo=SK20260606143000001
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "orderNo": "SK20260606143000001",
        "status": "SUCCESS",
        "coupon": {
            "id": 101,
            "code": "VIP-2026-ABCDEFGH",
            "type": 7,
            "expireAt": "2026-06-13T14:30:00"
        }
    }
}
```

### 4.4 我的券包

```
GET /api/coupon/my?page=1&size=10&status=1
```

**响应：**
```json
{
    "code": 200,
    "data": {
        "total": 3,
        "list": [
            {
                "id": 101,
                "code": "VIP-2026-ABCDEFGH",
                "type": 7,
                "typeName": "7天VIP",
                "status": 1,
                "statusName": "待使用",
                "issuedAt": "2026-06-06T14:30:00",
                "expireAt": "2026-06-13T14:30:00",
                "remainingDays": 6
            }
        ]
    }
}
```

### 4.5 激活/使用编码券

```
POST /api/coupon/activate
```

```json
{
    "couponId": 101
}
```

### 4.6 批量获取图片（VIP通道）

```
POST /api/picture/batch-fetch
```

```json
{
    "keyword": "nature",
    "count": 10,
    "size": "medium"
}
```

**响应（异步）：**
```json
{
    "code": 200,
    "data": {
        "taskId": "batch-fetch-uuid-001",
        "message": "已提交，获取完成后将通知您"
    }
}
```

### 4.7 WebSocket 推送格式

```
WS /ws/notification?token=xxx
```

**推送消息格式：**
```json
// 编码券抢购结果
{
    "type": "SECKILL_RESULT",
    "data": {
        "orderNo": "SK20260606143000001",
        "success": true,
        "couponCode": "VIP-2026-ABCDEFGH"
    }
}

// 批量获取图片完成
{
    "type": "BATCH_FETCH_COMPLETE",
    "data": {
        "taskId": "batch-fetch-uuid-001",
        "success": 8,
        "failed": 2,
        "pictures": ["url1", "url2", "..."]
    }
}
```

### 4.8 管理后台接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/batch/create` | POST | 创建发放批次（指定类型、数量、开始时间） |
| `/api/admin/batch/list` | GET | 批次列表（含库存、领取统计） |
| `/api/admin/batch/{id}/preheat` | POST | 手动触发预热 |
| `/api/admin/batch/{id}/cancel` | POST | 取消批次（归还库存） |
| `/api/admin/coupon/list` | GET | 编码券列表（支持按状态、用户筛选） |
| `/api/admin/coupon/{id}/revoke` | POST | 吊销编码券 |
| `/api/admin/seckill/stats` | GET | 秒杀数据看板（领取率、使用率、QPS） |
| `/api/admin/pexels/quota` | GET | Pexels API 配额使用情况 |

---

## 五、后端核心实现要点

### 5.1 Redis Lua 扣库存脚本

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

redis.call('HINCRBY', KEYS[1], 'stock', -1)
redis.call('HINCRBY', KEYS[1], 'version', 1)
return 1  -- 扣减成功
```

### 5.2 秒杀抢购 Service 核心逻辑（伪代码）

```java
public SeckillResult grab(Long userId, Long batchId, String token) {
    // 1. 降级检查
    if (degradeService.isDegraded()) {
        throw new BusinessException("活动暂时不可用");
    }

    // 2. 活动状态校验（读Redis预热数据）
    BatchInfo batch = getBatchFromRedis(batchId);
    checkBatchStatus(batch); // 未开始/已结束

    // 3. Token校验（防重放）
    String tokenKey = SECKILL_TOKEN + token;
    if (!redis.delete(tokenKey)) {
        throw new BusinessException("请求无效，请刷新页面");
    }

    // 4. 限流校验
    rateLimitUtil.checkRateLimit("seckill_ip", clientIP, 60, 10);
    rateLimitUtil.checkRateLimit("seckill_user", userId, 3600, 3);

    // 5. 防重校验（Redis + DB双重保障）
    String dedupeKey = SECKILL_DEDUPE + userId + ":" + batchId;
    if (!redis.setIfAbsent(dedupeKey, "1", 24h)) {
        throw new BusinessException("您已参与过本轮抢购");
    }

    // 6. Lua原子扣库存
    int version = getVersionFromRedis(batchId);
    Long result = redis.executeLua(seckill_deduct, batchId, 1, version);
    if (result == -1) throw new BusinessException("编码券已抢光");
    if (result == -2) throw new BusinessException("请重试");

    // 7. 发MQ消息（异步写DB）
    String orderNo = generateOrderNo();
    SeckillMessage msg = new SeckillMessage(userId, batchId, orderNo);
    rabbitTemplate.convertAndSend("seckill.exchange", "seckill.grab", msg);

    // 8. 返回排队中
    return SeckillResult.pending(orderNo);
}
```

### 5.3 MQ 消费者（异步写DB）

```java
@RabbitListener(queues = "seckill.queue")
public void handleSeckillGrab(SeckillMessage msg) {
    // 1. 幂等检查（DB防重表）
    if (seckillOrderMapper.existsByUserAndBatch(msg.getUserId(), msg.getBatchId())) {
        log.warn("重复消费，跳过: {}", msg);
        return;
    }

    try {
        // 2. 写秒杀订单
        SeckillOrder order = new SeckillOrder(msg);
        seckillOrderMapper.insert(order);

        // 3. 分配编码券（DB乐观锁）
        CodeCoupon coupon = couponMapper.selectOne(
            new QueryWrapper<CodeCoupon>()
                .eq("batch_id", msg.getBatchId())
                .eq("status", 0)  // 未发放
                .last("FOR UPDATE SKIP LOCKED")  // 非阻塞锁
        );
        if (coupon == null) {
            // 库存不足（极端情况），回滚Redis库存
            rollbackRedisStock(msg.getBatchId());
            order.setStatus(FAILED);
            seckillOrderMapper.updateById(order);
            return;
        }

        // 4. 绑定用户
        coupon.setUserId(msg.getUserId());
        coupon.setStatus(1); // 已领取
        coupon.setIssuedAt(LocalDateTime.now());
        coupon.setExpireAt(LocalDateTime.now().plusDays(coupon.getType()));
        couponMapper.updateById(coupon);

        // 5. 回填券ID到订单
        order.setCouponId(coupon.getId());
        order.setStatus(SUCCESS);
        seckillOrderMapper.updateById(order);

        // 6. WebSocket通知用户
        webSocketService.sendToUser(msg.getUserId(),
            new SeckillResultNotification(order.getOrderNo(), true, coupon.getCode()));

    } catch (Exception e) {
        log.error("秒杀处理异常", e);
        // 进入死信队列，人工处理
        throw e;
    }
}
```

### 5.4 预热逻辑

```java
public void preheat(Long batchId) {
    Batch batch = batchMapper.selectById(batchId);

    // 1. 生成编码券（批量插入）
    List<CodeCoupon> coupons = new ArrayList<>();
    for (int i = 0; i < batch.getTotalStock(); i++) {
        coupons.add(new CodeCoupon(batchId, generateCode(), batch.getType()));
    }
    couponMapper.batchInsert(coupons); // 分批插入，每批500

    // 2. 写入Redis
    String stockKey = SECKILL_BATCH_STOCK + batchId;
    redis.hset(stockKey, "stock", batch.getTotalStock());
    redis.hset(stockKey, "version", 0);

    String infoKey = SECKILL_BATCH_INFO + batchId;
    redis.set(infoKey, JSON.toJSONString(batch));

    // 3. 更新批次状态为"预热中"
    batch.setStatus(PREHEATING);
    batchMapper.updateById(batch);
}
```

### 5.5 库存回滚定时任务

```java
// 每小时执行
@Scheduled(cron = "0 0 * * * ?")
public void expireCoupons() {
    // 1. 扫描过期编码券
    List<CodeCoupon> expired = couponMapper.selectList(
        new QueryWrapper<CodeCoupon>()
            .eq("status", 1) // 已领取未使用
            .lt("expire_at", LocalDateTime.now())
    );

    for (CodeCoupon coupon : expired) {
        // 2. 更新状态
        coupon.setStatus(3); // 已过期
        couponMapper.updateById(coupon);

        // 3. Redis库存+1
        String stockKey = SECKILL_BATCH_STOCK + coupon.getBatchId();
        redis.hincr(stockKey, "stock", 1);
    }

    // 4. 清理防重标记（释放用户参与配额）
    expired.forEach(c -> {
        String dedupeKey = SECKILL_DEDUPE + c.getUserId() + ":" + c.getBatchId();
        redis.delete(dedupeKey);
    });
}
```

### 5.6 降级策略

```java
@Component
public class SeckillDegradationService {

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 降级等级：
     * 0 - 正常（全部可用）
     * 1 - Redis限流降级（跳过Redis限流，走DB限流）
     * 2 - MQ降级（同步写DB，响应变慢）
     * 3 - 只读降级（暂停抢购，可查询结果）
     * 4 - 全部降级（显示活动已结束）
     */
    public int getDegradeLevel() {
        String level = redis.opsForValue().get(SECKILL_DEGRADE);
        return level != null ? Integer.parseInt(level) : 0;
    }

    public boolean isSeckillEnabled() {
        return getDegradeLevel() < 3;
    }
}
```

---

## 六、前端配合事项

### 6.1 秒杀抢购页面

#### 页面元素
- 活动倒计时（秒杀未开始时显示）
- "立即抢购"按钮（带防抖）
- 排队动画（抢购中显示）
- 结果反馈（成功/失败弹窗）

#### 关键交互逻辑

```javascript
// === 秒杀抢购流程 ===

// 1. 页面加载时：获取活动信息 + 倒计时
async function loadSeckillInfo(batchId) {
    const { data } = await api.get(`/seckill/batch/${batchId}`)
    if (data.status === 'PREHEATING') {
        startCountdown(data.startTime) // 倒计时到开始时间
    }
}

// 2. 倒计时结束后：获取Token（防脚本刷接口）
async function fetchToken(batchId) {
    const { data } = await api.get('/seckill/token', { batchId })
    seckillToken.value = data.token // 保存Token，5分钟有效
}

// 3. 点击抢购（核心防抖逻辑）
const grabLock = ref(false)
async function handleGrab(batchId) {
    if (grabLock.value) return   // 防重复点击
    grabLock.value = true
    grabBtnDisabled.value = true // 立即禁用按钮
    showLoading.value = true     // 显示排队动画

    try {
        const { data } = await api.post('/seckill/grab', {
            batchId,
            token: seckillToken.value
        })

        if (data.status === 'PENDING') {
            // 进入轮询等待结果
            startPolling(data.orderNo)
        }
    } catch (err) {
        showError(err.message)    // 库存不足/已参与/限流等
        grabBtnDisabled.value = false
    } finally {
        grabLock.value = false
    }
}

// 4. 轮询抢购结果（配合WebSocket）
async function startPolling(orderNo) {
    // 优先使用WebSocket
    if (wsConnected.value) {
        waitForWsResult(orderNo) // 等待WS推送
        return
    }
    // 降级为HTTP轮询
    const poll = setInterval(async () => {
        const { data } = await api.get('/seckill/result', { orderNo })
        if (data.status !== 'PENDING') {
            clearInterval(poll)
            showResult(data)
        }
    }, 2000) // 每2秒轮询一次，最多轮询30次
}
```

#### 前端需要处理的错误码

| 错误码 | 含义 | 前端处理 |
|--------|------|---------|
| 4001 | 库存不足 | 显示"券已抢光"，隐藏抢购按钮 |
| 4002 | 重复抢购 | 显示"您已参与过本轮活动"，跳转我的券包 |
| 4003 | Token无效 | 刷新页面重新获取Token |
| 4291 | 限流 | 显示"当前使用人数过多，请稍后再试" |
| 4004 | 活动未开始 | 显示倒计时 |
| 4005 | 活动已结束 | 显示"活动已结束" |

### 6.2 我的券包页面

#### 页面元素
- Tab 切换：全部 / 待使用 / 使用中 / 已过期
- 券卡片：类型标签（3天/7天/30天）、状态、过期时间、剩余天数
- "使用"按钮（待使用状态的券）
- 来源标注：底部显示"Powered by Pexels"

#### 接口对接
```
GET /api/coupon/my?page=1&size=10&status=1
```

### 6.3 批量获取图片页面（VIP模式）

#### 页面元素
- 搜索框 + 数量选择（1-30张）
- 图片尺寸选择（small/medium/large）
- "获取图片"按钮
- 进度条（后端异步获取时）
- 获取结果展示（网格布局）
- **每张图片下方必须标注**："Photo by {photographer} on Pexels"（带链接）

#### 关键交互逻辑

```javascript
async function batchFetchPictures(keyword, count, size) {
    // 1. 提交获取请求（异步）
    const { data } = await api.post('/picture/batch-fetch', {
        keyword, count, size
    })

    taskId.value = data.taskId
    showProgress.value = true

    // 2. WebSocket 监听完成通知
    // 已有的 WS 连接会推送 BATCH_FETCH_COMPLETE 事件
}

// WS 消息处理
function handleWsMessage(msg) {
    if (msg.type === 'BATCH_FETCH_COMPLETE') {
        showProgress.value = false
        pictures.value = msg.data.pictures
        showResult(msg.data) // 显示成功8张/失败2张
    }
}
```

#### Pexels 合规要求（前端必须实现）

```
每张从 Pexels 获取的图片必须显示：
1. 摄影师署名："Photo by {photographer_name}"
2. Pexels 链接：指向 Pexels 照片页面的超链接
3. 示例：<a href="{pexels_photo_url}">Photo by {name} on Pexels</a>
```

### 6.4 管理后台页面（管理员端）

#### 批次管理
- 创建批次表单（类型、数量、开始时间）
- 批次列表（状态、库存、领取统计）
- 手动预热按钮
- 取消/结束按钮

#### 编码券管理
- 券列表（支持按批次、状态、用户筛选）
- 券详情（领取时间、使用时间、绑定用户）
- 吊销操作

#### 数据看板
- 秒杀活动统计（QPS、成功率、领取率）
- Pexels API 配额监控（每小时/每月使用量）
- 编码券使用漏斗（领取 → 激活 → 使用图片功能）

### 6.5 全局 WebSocket 连接管理

```javascript
// 需要监听的事件类型
const WS_EVENT_TYPES = {
    SECKILL_RESULT: 'seckillResult',        // 秒杀结果
    BATCH_FETCH_COMPLETE: 'batchFetchDone',  // 批量获取完成
}

// 连接管理（复用已有WS逻辑）
// 需确保：断线自动重连 + 消息不丢失 + 用户离线时消息缓存
```

---

## 七、后端定时任务清单

| 任务 | Cron | 说明 |
|------|------|------|
| 秒杀预热 | 活动开始前15分钟触发 | 生成编码券 + 写Redis |
| 过期券回收 | `0 0 * * * ?`（每小时） | 过期券状态更新 + Redis库存归还 |
| Redis-DB库存对账 | `0 */10 * * * ?`（每10分钟） | 比对差异，超阈值告警 |
| Pexels配额重置 | `0 0 * * ?`（每天0点） | 重置每小时计数器 |
| 死信队列补偿 | `0 */5 * * * ?`（每5分钟） | 消费死信，人工标记的重新处理 |

---

## 八、MQ 队列与交换机设计

```
Exchange: seckill.exchange (Topic)
  ├── seckill.queue        ← seckill.grab (抢购消息)
  │   └── DLX: seckill.dlx (死信)
  │       └── seckill.dlx.queue (死信消费)
  └── seckill.compensate.queue ← seckill.compensate (补偿消息)
```

**消息确认机制：**
- Publisher Confirm：确认消息到达Broker
- Consumer ACK：手动确认，处理成功才ACK
- 死信路由：消费失败3次后进入死信队列

---

## 九、配置项

```yaml
# application.yml 新增配置
seckill:
  # 分布式锁超时（秒）
  lock-timeout: 10
  # 限流配置
  rate-limit:
    ip-window: 60        # IP限流窗口（秒）
    ip-max: 10           # IP每窗口最大请求数
    user-window: 3600    # 用户限流窗口（秒）
    user-max: 3          # 用户每窗口最大请求数
  # Token配置
  token-ttl: 300         # 秒杀Token有效期（秒）
  # 防重配置
  dedupe-ttl: 86400      # 防重标记有效期（秒）
  # 编码券
  coupon:
    refund-hours: 1      # 允许退款时长（小时）
    code-prefix: "VIP"   # 编码券前缀
  # Pexels API
  pexels:
    hourly-limit: 200
    monthly-limit: 20000
```

---

## 十、开发优先级与排期建议

### Phase 1 — 基础设施（P0）
1. 数据库建表（批次表、编码券表、防重表）
2. Redis Key 常量类 + Lua 脚本
3. MQ 队列/交换机/死信配置
4. 基础 Entity/Mapper/Service 骨架

### Phase 2 — 核心秒杀流程（P0）
5. 预热 Service + 定时任务
6. 秒杀抢购接口（限流 → 幂等 → Lua扣库存 → MQ）
7. MQ 消费者（异步写DB）
8. 抢购结果查询接口
9. 前端秒杀页面 + 抢购交互

### Phase 3 — 编码券管理（P0/P1）
10. 我的券包接口 + 前端页面
11. 激活/使用编码券接口
12. 前端批量获取图片（VIP模式 + Pexels标注）
13. WebSocket 集成（秒杀结果 + 批量获取完成）

### Phase 4 — 运营与管理（P1）
14. 管理后台：批次CRUD + 预热
15. 管理后台：编码券列表 + 数据看板
16. 过期券回收定时任务
17. Redis-DB对账任务
18. 降级策略实现

### Phase 5 — 优化与加固（P2）
19. 秒杀链接动态签名
20. 设备指纹防刷（可选）
21. 活动预约/提醒
22. 数据埋点与运营指标
