# Phase 6 — 优化与加固

> 优先级：P2 | 依赖：Phase 4 | 预估工时：按需安排

## 目标

在核心功能完成后，进行安全加固、性能优化和运营增强。此阶段为可选项，按实际需求排期。

---

## 1. 秒杀链接动态签名

### 1.1 问题

固定 URL（`/api/seckill/grab`）容易被脚本直接抓取，需要在秒杀开始时动态生成签名链接。

### 1.2 方案

在 `SeckillController` 中增加链接签名验证：

```java
/**
 * 生成签名抢购链接
 * GET /api/seckill/signed-url?batchId=1
 */
@GetMapping("/signed-url")
@CheckAuth
public BaseResponse<String> getSignedUrl(@RequestParam Long batchId) {
    Long userId = UserContext.get().getId();

    // 签名 = HMAC-SHA256(batchId + userId + timestamp, secretKey)
    long timestamp = System.currentTimeMillis() / 30000; // 30 秒粒度
    String data = batchId + ":" + userId + ":" + timestamp;
    String sign = HmacUtils.hmacSha256Hex(SECRET_KEY, data);

    String signedUrl = String.format("/api/seckill/grab?batchId=%d&sign=%s&ts=%d",
            batchId, sign, timestamp);
    return ResultUtils.success(signedUrl);
}
```

在 `grab` 方法中追加签名校验：

```java
// 在 grab 方法入口追加签名校验
if (StrUtil.isNotBlank(dto.getSign())) {
    long ts = dto.getTs() != null ? dto.getTs() : 0;
    long currentTs = System.currentTimeMillis() / 30000;
    ThrowUtils.throwIf(Math.abs(currentTs - ts) > 1, ErrorCode.PARAMS_ERROR, "链接已过期");

    String expectedSign = HmacUtils.hmacSha256Hex(SECRET_KEY,
            batchId + ":" + userId + ":" + ts);
    ThrowUtils.throwIf(!expectedSign.equals(dto.getSign()), ErrorCode.PARAMS_ERROR, "签名无效");
}
```

---

## 2. 设备指纹防刷（可选）

### 2.1 问题

同一用户多设备、IP 池绕过限流。

### 2.2 方案

前端生成设备指纹（基于浏览器特征），后端在 Redis 中记录设备维度限流：

```java
// 在 grab 方法中追加
String deviceFingerprint = request.getHeader("X-Device-Fingerprint");
if (StrUtil.isNotBlank(deviceFingerprint)) {
    String deviceKey = "seckill:device:" + deviceFingerprint;
    String deviceCount = stringRedisTemplate.opsForValue().get(deviceKey);
    ThrowUtils.throwIf(deviceCount != null && Integer.parseInt(deviceCount) >= 3,
            ErrorCode.SYSTEM_ERROR, "当前设备已参与多次活动");
    stringRedisTemplate.opsForValue().increment(deviceKey);
    stringRedisTemplate.expire(deviceKey, 24, TimeUnit.HOURS);
}
```

---

## 3. 活动预约/提醒

### 3.1 数据表

```sql
CREATE TABLE IF NOT EXISTS seckill_reminder (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    batch_id    BIGINT NOT NULL,
    remind_time DATETIME NOT NULL COMMENT '提醒时间（活动开始前5分钟）',
    notified    TINYINT DEFAULT 0 COMMENT '0-未提醒, 1-已提醒',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_batch (user_id, batch_id),
    KEY idx_remind_time (remind_time, notified)
) COMMENT '秒杀活动预约提醒' COLLATE = utf8mb4_unicode_ci;
```

### 3.2 预约接口

```java
/**
 * 预约秒杀提醒
 * POST /api/seckill/remind
 */
@PostMapping("/remind")
@CheckAuth
public BaseResponse<Boolean> setReminder(@RequestBody Map<String, Long> body) {
    Long batchId = body.get("batchId");
    // 写入 seckill_reminder 表
    // 定时任务在活动开始前 5 分钟发送 WebSocket / 站内通知
    return ResultUtils.success(true);
}
```

### 3.3 定时任务

```java
/**
 * 秒杀提醒推送 — 每分钟检查
 */
@Scheduled(cron = "0 * * * * ?")
public void sendSeckillReminders() {
    // 查询 remind_time 在未来 5 分钟内且 notified=0 的记录
    // 发送 WebSocket / 站内通知
    // 更新 notified=1
}
```

---

## 4. 数据埋点与运营指标

### 4.1 埋点数据结构

在 Redis 中记录秒杀关键指标：

```java
// 秒杀请求总数
public static final String SECKILL_METRIC_REQUESTS = "seckill:metric:requests:%s"; // 日期
// 秒杀成功数
public static final String SECKILL_METRIC_SUCCESS = "seckill:metric:success:%s";
// 秒杀失败数（含库存不足、重复、限流等）
public static final String SECKILL_METRIC_FAIL = "seckill:metric:fail:%s";
// 秒杀响应时间 P99
public static final String SECKILL_METRIC_LATENCY = "seckill:metric:latency:%s";
```

### 4.2 埋点实现

在 `SeckillServiceImpl.grab()` 方法中追加：

```java
// 入口埋点
String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
stringRedisTemplate.opsForValue().increment(
        String.format("seckill:metric:requests:%s", today));

long startTime = System.currentTimeMillis();

// ... 抢购逻辑 ...

// 出口埋点
long latency = System.currentTimeMillis() - startTime;
if (success) {
    stringRedisTemplate.opsForValue().increment(
            String.format("seckill:metric:success:%s", today));
} else {
    stringRedisTemplate.opsForValue().increment(
            String.format("seckill:metric:fail:%s", today));
}
// 记录延迟到 Sorted Set（取 P99）
stringRedisTemplate.opsForZSet().add(
        String.format("seckill:metric:latency:%s", today),
        UUID.randomUUID().toString(), latency);
```

### 4.3 运营指标 API

```java
/**
 * 秒杀实时指标
 * GET /api/admin/seckill/realtime-metrics
 */
@GetMapping("/realtime-metrics")
@CheckAuth(mustRole = "admin")
public BaseResponse<Map<String, Object>> getRealtimeMetrics() {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    Map<String, Object> metrics = new HashMap<>();
    metrics.put("requests", getMetric("seckill:metric:requests:" + today));
    metrics.put("success", getMetric("seckill:metric:success:" + today));
    metrics.put("fail", getMetric("seckill:metric:fail:" + today));

    // 计算成功率
    long requests = (long) metrics.get("requests");
    long success = (long) metrics.get("success");
    metrics.put("successRate", requests > 0 ? (double) success / requests : 0);

    // P99 延迟
    // 从 Sorted Set 取第 99 百分位的延迟值
    // ...

    return ResultUtils.success(metrics);
}
```

---

## 5. Pexels API 配额监控

### 5.1 配额计数

在批量获取图片的 Service 中追加配额计数：

```java
// 每次调用 Pexels API 前检查配额
String hourlyKey = String.format(RedisKeyConstants.PEXELS_HOURLY_COUNT,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
String monthlyKey = String.format(RedisKeyConstants.PEXELS_MONTHLY_COUNT,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")));

String hourlyCount = stringRedisTemplate.opsForValue().get(hourlyKey);
ThrowUtils.throwIf(hourlyCount != null && Integer.parseInt(hourlyCount) >= 200,
        ErrorCode.SYSTEM_ERROR, "Pexels API 每小时配额已用完");

// 调用后递增
stringRedisTemplate.opsForValue().increment(hourlyKey);
stringRedisTemplate.expire(hourlyKey, 2, TimeUnit.HOURS); // 2小时 TTL

stringRedisTemplate.opsForValue().increment(monthlyKey);
stringRedisTemplate.expire(monthlyKey, 32, TimeUnit.DAYS); // 32天 TTL
```

### 5.2 配额查询接口

在 `AdminBatchController` 中追加：

```java
/**
 * Pexels API 配额使用情况
 * GET /api/admin/pexels/quota
 */
@GetMapping("/pexels/quota")
public BaseResponse<Map<String, Object>> getPexelsQuota() {
    Map<String, Object> quota = new HashMap<>();
    String hourlyKey = String.format(RedisKeyConstants.PEXELS_HOURLY_COUNT,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
    String monthlyKey = String.format(RedisKeyConstants.PEXELS_MONTHLY_COUNT,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")));

    quota.put("hourlyUsed", getMetricValue(hourlyKey));
    quota.put("hourlyLimit", 200);
    quota.put("monthlyUsed", getMetricValue(monthlyKey));
    quota.put("monthlyLimit", 20000);

    return ResultUtils.success(quota);
}
```

---

## 6. WebSocket 推送集成

### 6.1 秒杀结果推送

利用项目已有的 WebSocket 基础设施（`BatchWebSocketHandler`），在 MQ 消费者成功处理后推送：

```java
// 在 SeckillConsumer.handleSeckillGrab 中，成功后推送
try {
    String wsMessage = JSONUtil.toJsonStr(Map.of(
            "type", "SECKILL_RESULT",
            "data", Map.of(
                    "orderNo", order.getOrderNo(),
                    "success", true,
                    "couponCode", coupon.getCode()
            )
    ));
    // 通过 SSE 或 WebSocket 推送给用户
    // webSocketService.sendToUser(msg.getUserId(), wsMessage);
} catch (Exception e) {
    log.warn("WebSocket推送失败, userId={}", msg.getUserId(), e);
}
```

### 6.2 批量获取图片完成推送

在 `BatchPictureConsumer` 中已有的完成逻辑中，追加 VIP 批量获取场景的推送消息。

---

## 验证清单

- [ ] 签名链接在 30 秒内有效，过期后拒绝
- [ ] 签名篡改后被拒绝
- [ ] 设备指纹限流：同一设备 24 小时内最多参与 3 次
- [ ] 秒杀提醒预约成功，活动开始前 5 分钟收到通知
- [ ] 秒杀数据埋点正确记录请求数、成功数、失败数
- [ ] 运营指标 API 返回正确的实时数据
- [ ] Pexels API 配额达上限后批量获取被拒绝
- [ ] 配额查询接口返回正确的使用量
- [ ] WebSocket 推送秒杀结果成功到达客户端
