# PictureProject 接口限流整体架构方案

> 版本：v1.0 | 日期：2026-06-13 | 作者：后端架构师

---

## 目录

- [1. 背景与现状](#1-背景与现状)
- [2. 设计目标](#2-设计目标)
- [3. 整体架构](#3-整体架构)
- [4. 详细设计](#4-详细设计)
  - [4.1 `@RateLimit` 注解 + AOP 设计](#41-ratelimit-注解--aop-设计)
  - [4.2 限流维度抽象](#42-限流维度抽象)
  - [4.3 阈值分级与配置化](#43-阈值分级与配置化)
  - [4.4 统一响应处理](#44-统一响应处理)
  - [4.5 故障兜底与降级策略](#45-故障兜底与降级策略)
  - [4.6 黑白名单](#46-黑白名单)
  - [4.7 热点接口清单](#47-热点接口清单)
  - [4.8 监控与可观测](#48-监控与可观测)
  - [4.9 与现有散落限流的迁移路径](#49-与现有散落限流的迁移路径)
  - [4.10 分阶段实施计划](#410-分阶段实施计划)
- [5. 风险与权衡](#5-风险与权衡)
- [6. 附录](#6-附录)

---

## 1. 背景与现状

### 1.1 项目模块结构

| 模块 | 职责 |
|------|------|
| `common` | 通用工具、注解、常量、异常、上下文 |
| `pojo` | 实体、DTO、VO |
| `server` | Controller、Service、AOP、Config、Mapper |

### 1.2 已有底层基础设施（复用，不推翻重写）

| 组件 | 路径 | 说明 |
|------|------|------|
| `RateLimitUtil` | `common/src/main/java/org/example/common/util/RateLimitUtil.java` | Redis Sorted Set + Lua 滑动窗口限流器，返回 `Result(allowed, remaining, resetMs)`，Redis 故障时放行 |
| `rate_limit_sliding_window.lua` | `server/src/main/resources/scripts/rate_limit_sliding_window.lua` | ZREMRANGEBYSCORE + ZCARD + ZADD 原子脚本 |
| `RedisConfig` | `server/src/main/java/org/example/server/config/RedisConfig.java` | 已注册 `rateLimitScript` Bean |
| `RedisKeyConstants` | `common/src/main/java/org/example/common/constants/RedisKeyConstants.java` | 限流 key 前缀和部分模块常量（散落） |
| `@RedisTimed` + `RedisTimedAop` | `common/.../annotation/RedisTimed.java` + `server/.../aop/RedisTimedAop.java` | 已有"注解 + @Around AOP"范式，限流注解应完全沿用 |

### 1.3 核心痛点

1. **点状硬编码**：`RateLimitUtil` 仅被 3 处调用（`UserServiceImpl.userLogin`、`SeckillServiceImpl.grab`、`CouponServiceImpl.activateCoupon`），无全局架构
2. **实现风格混乱**：`REPORT_RATE_LIMIT_KEY`(60s/3次)、`BATCH_TASK_RATE_LIMIT_KEY`(60s/1次)、`COUPON_ACTIVATE_RATE_LIMIT_KEY`(60s/5次) 用 INCR+EXPIRE，`LOGIN_RATE_LIMIT_*`(5min/5次) 用滑动窗口，风格不统一
3. **无接口级统一限流**：26 个 Controller 全部接口无统一限流入口
4. **无 `@RateLimit` 注解 + AOP**：每个限流点手写代码
5. **无统一 429 响应处理**：无 `Retry-After` 头、无剩余次数透传
6. **无限流维度组合**：无 USER/IP/API/USER+API/IP+API/GLOBAL 等维度
7. **配置硬编码**：阈值写死在常量类，不能热更新、不能按环境差异化
8. **无黑白名单**：管理员/内网 IP 放行、恶意 IP 拉黑均无机制
9. **无限流监控**：命中率、拒绝率、热点接口排行均无

### 1.4 现有关键类确认

| 类名 | 包路径 | 确认状态 |
|------|--------|----------|
| `BaseResponse<T>` | `org.example.common.result` | `{code, data, message}` 三字段结构 |
| `ResultUtils` | `org.example.common.result` | `success(data)` / `error(code, message)` |
| `ErrorCode` | `org.example.common.exception` | 枚举，`{code, message}` |
| `BusinessException` | `org.example.common.exception` | `{code, message}`，继承 RuntimeException |
| `GlobalExceptionHandler` | `org.example.common.exception` | `@RestControllerAdvice`，处理 `BusinessException` + `RuntimeException` |
| `UserContext` | `org.example.common.context` | ThreadLocal<User>，`get()` 返回当前登录用户 |
| `Actuator/Micrometer` | 无 | **项目未引入** Micrometer/Actuator |
| IP 获取工具 | 无独立工具类 | 仅 `SeckillController` 内有 `getClientIP()` 私有方法 |

---

## 2. 设计目标

| 目标 | 衡量标准 |
|------|----------|
| 统一限流入口 | 所有接口可通过 `@RateLimit` 注解声明式接入限流 |
| 维度组合灵活 | 支持 USER / IP / API / USER+API / IP+API / GLOBAL / CUSTOM(SpEL) |
| 配置可覆盖 | 三级优先级：注解 > yml > 全局默认 |
| 响应标准化 | HTTP 429 + `Retry-After` 头 + 结构化响应体 |
| 复用现有底层 | 不重写 `RateLimitUtil` 和 Lua 脚本 |
| 可观测 | 限流命中/拒绝日志 + 未来可接入 Micrometer |
| 渐进迁移 | 不一刀切，先建新设施再迁旧点 |

---

## 3. 整体架构

### 3.1 架构总览图

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP 请求入口                             │
│                     (DispatcherServlet)                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Filter 层（可选 - Phase 3）                    │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │  白名单检查  │  │  黑名单检查   │  │  全局 QPS 兜底限流     │  │
│  │ (admin/内网) │  │ (恶意IP/User) │  │  (保护性全局阈值)      │  │
│  └─────────────┘  └──────────────┘  └───────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Interceptor 层（现有）                         │
│           LoginInterceptor → 设置 UserContext                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AOP 层（本方案核心）                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              @RateLimit AOP 切面                         │   │
│  │                                                         │   │
│  │  1. 解析注解（资源名/维度/窗口/阈值/兜底策略）            │   │
│  │  2. 提取限流 key（userId / IP / 接口路径 / SpEL）        │   │
│  │  3. 优先级合并（注解 > yml 配置 > 全局默认）              │   │
│  │  4. 白名单检查（跳过）                                    │   │
│  │  5. 调用 RateLimitUtil.checkRateLimit()                  │   │
│  │  6. 被限流 → 抛 RateLimitException                        │   │
│  │     放行 → proceed()                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Controller 方法                               │
│                 (@PostMapping / @GetMapping ...)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              GlobalExceptionHandler 统一异常处理                   │
│  RateLimitException → HTTP 429 + Retry-After + BaseResponse      │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 请求处理时序

```
Client ──HTTP──> Filter(黑白名单) ──> Interceptor(UserContext)
  ──> AOP(@RateLimit 检查) ──> Controller ──> Service
       │
       ├─ 允许 ──> proceed()
       └─ 拒绝 ──> throw RateLimitException
                       │
                       ▼
              GlobalExceptionHandler
                       │
                       ▼
              HTTP 429 + Retry-After + JSON Body
```

---

## 4. 详细设计

### 4.1 `@RateLimit` 注解 + AOP 设计

#### 4.1.1 注解定义

**放置模块**：`common/src/main/java/org/example/common/annotation/RateLimit.java`

**理由**：
- 与 `@RedisTimed` 同层（`common/src/main/java/org/example/common/annotation/RedisTimed.java`），保持一致性
- common 模块被 pojo 和 server 共同依赖，注解需要在 Controller 层（server）使用，放在 common 保证可见性
- 注解本身不含 Spring 依赖，只是元数据标记，符合 common 定位

**注解字段设计**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 资源名（限流 key 的一部分）
     * 为空时默认按 "类简名.方法名" 自动生成
     */
    String resource() default "";

    /**
     * 限流维度组合（决定 key 的构成方式）
     */
    RateLimitDimension[] dimensions() default {RateLimitDimension.USER_OR_IP};

    /**
     * 滑动窗口大小（秒）
     */
    int windowSeconds() default 60;

    /**
     * 窗口内最大允许次数
     */
    int maxAttempts() default 100;

    /**
     * 故障兜底策略
     * FAIL_OPEN  - Redis 故障时放行（默认，与 RateLimitUtil 现有策略一致）
     * FAIL_CLOSE - Redis 故障时拒绝（适用于关键安全接口）
     */
    FallbackStrategy fallback() default FallbackStrategy.FAIL_OPEN;

    /**
     * 自定义 key 的 SpEL 表达式（当 dimensions 含 CUSTOM 时生效）
     * 例: "#dto.targetType + ':' + #dto.targetId"
     */
    String spelKey() default "";
}
```

**关联枚举**（同放 `common/src/main/java/org/example/common/annotation/` 或 `common/.../enums/`）：

```java
public enum RateLimitDimension {
    USER,       // 按登录用户 userId
    IP,         // 按客户端 IP
    API,        // 按接口路径（全局限流）
    GLOBAL,     // 全局（所有请求共享一个窗口）
    CUSTOM      // 按 SpEL 表达式解析
}

public enum FallbackStrategy {
    FAIL_OPEN,   // 放行
    FAIL_CLOSE   // 拒绝
}
```

> **补充维度组合**：注解中 `dimensions` 为数组，支持多维度叠加，如 `{USER, API}` 表示 "按用户+接口组合限流"。切面拼接时按固定顺序 JOIN。也提供一个语义化速记维度 `USER_OR_IP`（已登录用 userId，未登录降级为 IP），这是最常用的默认维度。

#### 4.1.2 AOP 切面

**放置模块**：`server/src/main/java/org/example/server/aop/RateLimitAop.java`

**理由**：
- 与 `RedisTimedAop`（`server/src/main/java/org/example/server/aop/RedisTimedAop.java`）同层
- 切面依赖 `RateLimitUtil`（common）、`UserContext`（common）、`HttpServletRequest`（servlet），server 模块依赖最全

**切点表达式**：

```java
@Around("@annotation(rateLimit)")
```

与 `RedisTimedAop` 使用 `@annotation(redisTimed)` 完全一致的范式，拦截标注 `@RateLimit` 的方法。

**切面执行流程**：

```
1. 从 ProceedingJoinPoint 获取 Method 和注解元数据
2. 解析资源名：注解 resource() 为空 → "类简名.方法名"
3. 提取限流 key 组成部分：
   - userId: UserContext.get()?.getId()（未登录为 null）
   - IP:     RequestContextHolder 获取 HttpServletRequest，调用 IpUtils
   - API:    request.getMethod() + ":" + request.getRequestURI()
   - SpEL:   解析 spelKey() 表达式，从方法参数中取值
4. 按维度组合拼接 key：
   - 单维度: 该维度值
   - 多维度: 按固定顺序用 ":" JOIN（如 userId + ":" + apiPath）
   - USER_OR_IP: userId != null ? userId : ip
5. 三级优先级合并窗口/阈值：
   注解值 > yml 配置（resource 匹配）> 全局默认
6. 白名单检查（Phase 3）：管理员/内网 IP → 直接放行
7. 调用 RateLimitUtil.checkRateLimit(resource, composedKey, window, max)
8. 判断结果：
   - allowed == true → joinPoint.proceed()
   - allowed == false → throw new RateLimitException(...)
9. 异常处理（RateLimitUtil 内部 Redis 故障时）：
   - fallback == FAIL_OPEN → 放行 proceed()
   - fallback == FAIL_CLOSE → throw RateLimitException
```

#### 4.1.3 userId / IP 提取方式

**userId**：
```java
User user = UserContext.get();
Long userId = (user != null) ? user.getId() : null;
```
- 来源：`LoginInterceptor` 在请求到达 Controller 前已设置 `UserContext`
- 未登录用户：userId 为 null，需降级处理

**IP**：
- 需要新建 `IpUtils`（放置 `common/src/main/java/org/example/common/util/IpUtils.java`）
- 当前 `SeckillController.getClientIP()` 私有方法已实现核心逻辑，应提取为公共工具类
- 提取链路：`X-Forwarded-For` → `X-Real-IP` → `Proxy-Client-IP` → `WL-Proxy-Client-IP` → `request.getRemoteAddr()`
- 多级代理：`X-Forwarded-For` 取第一个非 `unknown` 的 IP
- **防伪造**：需配置可信代理 IP 列表（Phase 3 增强），Phase 1 先按标准链路解析

---

### 4.2 限流维度抽象

#### 4.2.1 维度组合与 key 拼装规则

| 维度 | Redis key 格式 | 适用场景 | 未登录处理 |
|------|---------------|----------|------------|
| `USER` | `rate_limit:{resource}:u:{userId}` | 登录用户的写操作（发布、删除） | 降级为 IP |
| `IP` | `rate_limit:{resource}:ip:{ip}` | 公开接口、登录/注册 | 直接使用 |
| `API` | `rate_limit:{resource}:api:{method}:{uri}` | 接口级全局限流（保护后端总并发） | 无需登录 |
| `USER+API` | `rate_limit:{resource}:u:{userId}:{method}:{uri}` | 用户+接口组合（最精准） | 降级为 IP+API |
| `IP+API` | `rate_limit:{resource}:ip:{ip}:{method}:{uri}` | IP+接口组合（防爬虫按接口刷） | 直接使用 |
| `GLOBAL` | `rate_limit:{resource}:global` | 全局总量保护（如秒杀总入口） | 无需登录 |
| `USER_OR_IP` | `rate_limit:{resource}:uip:{userId或ip}` | 通用默认（登录用 userId，未登录用 IP） | 自动降级 IP |
| `CUSTOM` | `rate_limit:{resource}:c:{spelResult}` | 业务自定义（如按 targetType:targetId） | 取决于 SpEL |

#### 4.2.2 未登录降级策略

```java
// USER_OR_IP 维度（推荐作为默认）
String keyPart;
if (userId != null) {
    keyPart = "u:" + userId;
} else {
    keyPart = "ip:" + ip;
}

// USER 维度（严格）
if (userId == null) {
    // 未登录直接跳过限流（因为该接口本身要求登录，LoginInterceptor 会拦截）
    return joinPoint.proceed();
}

// USER+API 维度
String keyPart = (userId != null ? "u:" + userId : "ip:" + ip) + ":" + apiPath;
```

#### 4.2.3 IP 获取链路（防伪造）

```
X-Forwarded-For: client, proxy1, proxy2
                ↓ 取第一个非 unknown 的值
X-Real-IP: client_ip（Nginx 直连配置）
                ↓
Proxy-Client-IP / WL-Proxy-Client-IP（Apache/WebLogic）
                ↓
request.getRemoteAddr()（最终兜底）
```

**Phase 1**：按标准链路解析，`X-Forwarded-For` 第一个值
**Phase 3 增强**：引入可信代理 IP 白名单，仅当 `RemoteAddr` 在可信代理列表中时才信任 `X-Forwarded-For`

---

### 4.3 阈值分级与配置化

#### 4.3.1 三级优先级

```
优先级（从高到低）：
┌─────────────────────────────────────────────────────┐
│ 1. 方法注解 @RateLimit(windowSeconds=60, maxAttempts=5)  │
│    → 最高优先级，开发者显式指定                        │
├─────────────────────────────────────────────────────┤
│ 2. application.yml 配置（按 resource 匹配）           │
│    → 运维可调整，不改动代码                           │
├─────────────────────────────────────────────────────┤
│ 3. 全局默认值                                         │
│    → 兜底保护，防止遗漏                               │
└─────────────────────────────────────────────────────┘
```

**合并规则**：`windowSeconds` 和 `maxAttempts` 独立合并。如果 yml 中配置了该 resource 的值，则覆盖注解默认值（注解显式指定的值不会被 yml 覆盖）。

> **精确定义**：注解字段值为"非默认值"时视为"显式指定"，yml 不覆盖。注解字段值为默认值时，yml 可覆盖。实现方式：注解增加 `windowSecondsSet` / `maxAttemptsSet` 标记，或使用 `-1` 表示未设置。

**推荐方案**：使用 `-1` 哨兵值表示"未显式设置"，AOP 判断时优先取注解有效值，否则取 yml，否则取全局默认。

#### 4.3.2 application.yml 配置结构

在 `server/src/main/resources/application.yml` 新增：

```yaml
ratelimit:
  # 全局默认值
  default-window-seconds: 60
  default-max-attempts: 100
  # 全局故障兜底策略
  default-fallback: FAIL_OPEN

  # 按 resource 覆盖（resource 名对应注解 resource 或自动生成的 "类简名.方法名"）
  rules:
    - resource: "login"
      window-seconds: 300
      max-attempts: 5
    - resource: "PictureController.upload"
      window-seconds: 60
      max-attempts: 10
    - resource: "ReportController.submitReport"
      window-seconds: 60
      max-attempts: 3
    - resource: "SeckillController.grab"
      window-seconds: 10
      max-attempts: 2
```

**对应配置类**：`server/src/main/java/org/example/server/config/RateLimitProperties.java`（`@ConfigurationProperties(prefix = "ratelimit")`）

#### 4.3.3 热更新方案

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **A. 不做热更新**（重启生效） | 实现零成本，简单 | 改阈值需重启 | MVP 够用 |
| **B. Redis 配置中心**（限流规则存 Redis Hash） | 实时生效，无需重启 | 增加 Redis 依赖，读取有延迟 | 推荐 Phase 3 |
| **C. @RefreshScope**（需 Spring Cloud Config） | 框架原生 | 引入 Spring Cloud 全家桶，过重 | 不推荐 |
| **D. 自定义监听器**（监听 yml 变更或 HTTP 端点触发刷新） | 灵活 | 实现复杂，需要维护线程 | 不推荐 |

**推荐**：Phase 1-2 用方案 A（yml 配置 + 重启），Phase 3 引入方案 B（Redis 存限流规则，AOP 每次从 Redis 读取并本地缓存 30s）。

---

### 4.4 统一响应处理

#### 4.4.1 RateLimitException

**新增异常类**：`common/src/main/java/org/example/common/exception/RateLimitException.java`

```java
public class RateLimitException extends RuntimeException {
    private final int remaining;      // 剩余次数
    private final long resetMs;       // 重置时间（毫秒时间戳）
    private final int retryAfterSec;  // 建议重试等待秒数

    // 构造方法...
}
```

> 不继承 `BusinessException`，因为 `BusinessException` 的 `code` 字段语义偏向"业务错误码"，限流是"控制平面"决策，语义不同。但为了复用 `GlobalExceptionHandler`，可以继承 `BusinessException` 并设置 `code = 42900`。两种方案均可，**推荐继承 BusinessException**（减少异常处理器的分支）。

#### 4.4.2 ErrorCode 新增

在 `common/src/main/java/org/example/common/exception/ErrorCode.java` 新增：

```java
RATE_LIMIT_EXCEEDED(42900, "请求过于频繁，请稍后再试"),
```

#### 4.4.3 GlobalExceptionHandler 增强

在 `common/src/main/java/org/example/common/exception/GlobalExceptionHandler.java` 新增：

```java
@ExceptionHandler(RateLimitException.class)
public ResponseEntity<BaseResponse<?>> rateLimitExceptionHandler(RateLimitException e) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Retry-After", String.valueOf(e.getRetryAfterSec()));
    headers.set("X-RateLimit-Remaining", "0");
    headers.set("X-RateLimit-Reset", String.valueOf(e.getResetMs()));

    BaseResponse<?> body = new BaseResponse<>(42900, null, e.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(body);
}
```

> **注意**：`GlobalExceptionHandler` 当前在 `common` 模块，但 `ResponseEntity` 是 Spring Web 依赖，common 模块的 pom 是否已包含？需确认。如果 common 不依赖 spring-web，则该异常处理器需放在 `server` 模块新建一个 `@RestControllerAdvice`。**推荐**：新建 `server/src/main/java/org/example/server/config/RateLimitExceptionHandler.java`，避免 common 引入 web 依赖。

#### 4.4.4 前端收到的响应

**HTTP 响应头**：
```
HTTP/1.1 429 Too Many Requests
Retry-After: 58
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1718299980000
Content-Type: application/json
```

**响应体**：
```json
{
  "code": 42900,
  "data": null,
  "message": "请求过于频繁，请稍后再试"
}
```

---

### 4.5 故障兜底与降级策略

#### 4.5.1 复用 RateLimitUtil 现有策略

`RateLimitUtil.checkRateLimit()` 在 Redis 异常时已实现"放行请求"逻辑：

```java
// RateLimitUtil.java:86-90（现有代码）
} catch (Exception e) {
    log.error("限流检查异常，放行请求: key={}", redisKey, e);
    return Result.allowed(maxAttempts - 1, nowMs + windowMs);
}
```

#### 4.5.2 注解级覆盖

AOP 层根据 `@RateLimit(fallback = FAIL_CLOSE)` 处理：

```java
// AOP 中伪逻辑
Result result = rateLimitUtil.checkRateLimit(resource, key, window, max);

if (result == null || (result.allowed() == true && redisWasDown)) {
    // RateLimitUtil 内部 Redis 异常时返回 allowed
    // 但 AOP 无法区分"正常允许"和"故障放行"
    // → 解决方案：RateLimitUtil.Result 增加 status 字段（Phase 2 增强）
    // Phase 1：保持 RateLimitUtil 原样，FAIL_CLOSE 仅在 result==null 时拒绝
}
```

**推荐方案（Phase 1）**：
- `RateLimitUtil` 保持不变（故障放行）
- AOP 层在捕获到 RateLimitUtil 返回的 Result 后：
  - 如果 `result.allowed()` 为 true → 正常 proceed
  - 如果为 false → 检查 fallback：
    - FAIL_OPEN → proceed（但 RateLimitUtil 已保证故障时返回 allowed=true）
    - FAIL_CLOSE → throw RateLimitException

**Phase 2 增强**：`RateLimitUtil.Result` record 新增 `Status` 字段（`ALLOWED` / `REJECTED` / `DEGRADED`），AOP 根据 `DEGRADED` + `fallback` 决定放行还是拒绝。

#### 4.5.3 Redis 不可用时影响面评估

| 场景 | 影响 | 严重度 |
|------|------|--------|
| Redis 完全不可用 | 所有 `@RateLimit` 注解失效（FAIL_OPEN）→ 请求全放行 | 中（业务可用但无保护） |
| Redis 不可用 + FAIL_CLOSE 接口 | 关键接口直接拒绝所有请求 | 高（需确保仅极少数接口用 FAIL_CLOSE） |
| Redis 网络抖动（偶发超时） | 偶发请求被放行 | 低 |

**结论**：FAIL_OPEN 为默认策略，仅"秒杀抢券"等极少数接口考虑 FAIL_CLOSE。建议 Phase 1 统一 FAIL_OPEN，Phase 2 再引入 FAIL_CLOSE 选项。

---

### 4.6 黑白名单

#### 4.6.1 白名单（放行）

| 类型 | 判断条件 | 配置位置（推荐） |
|------|----------|-----------------|
| 管理员用户 | `UserContext.get().getUserRole() == "admin"` | AOP 硬编码（角色判断不常变） |
| 内网 IP | IP 在 `10.0.0.0/8` / `172.16.0.0/12` / `192.168.0.0/16` | yml `ratelimit.whitelist.ip-range` |
| 健康检查/Actuator | URI 前缀匹配 `/actuator/**` | AOP 硬编码 |
| Swagger/Knife4j | URI 前缀匹配 `/swagger-*/**`、`/v3/api-docs/**` | AOP 硬编码 |

#### 4.6.2 黑名单（拒绝）

| 类型 | 判断条件 | 配置位置（推荐） |
|------|----------|-----------------|
| 恶意 IP | IP 在 Redis Set `ratelimit:blacklist:ip` | Redis（运维动态添加） |
| 封禁用户 | 已有 `BAN_BLACKLIST_ZSET`（`RedisKeyConstants.java:433`） | 复用现有封禁体系 |

#### 4.6.3 配置位置权衡

| 方案 | 灵活性 | 复杂度 | 推荐度 |
|------|--------|--------|--------|
| **yml 配置** | 低（需重启） | 低 | 白名单静态规则（IP 段、URI 前缀） |
| **Redis 配置** | 高（实时） | 中 | 黑名单（动态添加恶意 IP） |
| **数据库** | 中 | 高 | 不推荐（增加 DB 负担） |

**推荐**：
- 白名单 → yml（静态规则）+ AOP 硬编码（管理员/Actuator）
- 黑名单 → Redis Set（动态管理）
- 复用现有 `BAN_BLACKLIST_ZSET` 做封禁用户检查（不重复造轮子）

#### 4.6.4 与现有 BAN 封禁体系的关系

现有封禁体系（`RedisKeyConstants.BAN_BLACKLIST_ZSET`）是**业务封禁**（违规用户封禁），与限流黑名单（恶意 IP 临时拉黑）语义不同：

| 体系 | 触发条件 | 时效 | 位置 |
|------|----------|------|------|
| BAN 封禁 | 违规行为（举报成立等） | 3天/7天/30天/永久 | 数据库 + Redis ZSET |
| 限流黑名单 | 接口恶意请求 | 分钟级~小时级 | Redis Set |

**建议**：两者独立维护，但 AOP 检查时**同时检查**——封禁用户直接拒绝（在 Interceptor 层已处理），限流黑名单 IP 在 AOP 层拒绝。

---

### 4.7 热点接口清单

基于对以下 Controller 的端点审查，给出建议限流配置。

#### 4.7.1 UserController（`server/.../controller/UserController.java`）

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| `POST /user/register` | `register` | 60s / 3次 | IP | 防注册机批量注册 |
| `POST /user/login` | `login` | 5min / 5次 | USER_OR_IP（account） | 防撞库爆破（现有逻辑已实现，需迁移） |
| `GET /user/{id}` | `getUserById` | 60s / 60次 | USER | 防爬虫遍历用户信息 |
| `POST /user/update` | `updateUser` | 60s / 10次 | USER | 防频繁修改 |
| `POST /user/avatar/upload` | `uploadAvatar` | 60s / 5次 | USER | 防频繁上传头像（带宽成本） |

> **特别注意**：登录限流的限流 key 是**账号**（`account` 参数），不是 userId（因为登录前无 userId）。当前 `UserServiceImpl.userLogin` 直接以 `account` 作为 key 调用 `RateLimitUtil`。迁移时需用 `CUSTOM` 维度 + SpEL `#dto.account`。

#### 4.7.2 PictureController（`server/.../controller/PictureController.java`）

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| `POST /picture/upload` (文件) | `upload(MultipartFile)` | 60s / 10次 | USER | OSS 带宽/存储成本，防恶意上传 |
| `POST /picture/upload/url` | `upload(FileDTO)` | 60s / 10次 | USER | 同上 |
| `GET /picture/download` | `download` | 60s / 20次 | USER | 带宽成本，防批量爬取 |
| `POST /picture/user/query` | `queryPictureUser` | 60s / 60次 | USER | 热点读，防爬虫 |
| `POST /picture/admin/query` | `queryPictureAdmin` | 60s / 30次 | USER（admin） | 管理端，低频 |
| `POST /picture/update` | `updatePicture` | 60s / 20次 | USER | 写操作 |
| `DELETE /picture/delete` | `deletePicture` | 60s / 10次 | USER | 敏感操作 |

#### 4.7.3 SeckillController（`server/.../controller/SeckillController.java`）

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| `GET /seckill/token` | `getToken` | 10s / 2次 | USER+API | 防频繁获取令牌 |
| `POST /seckill/grab` | `grab` | 10s / 1次 | USER+API | 防重复提交（Service 层已有券级限流，接口层再加一层） |
| `GET /seckill/result` | `getResult` | 5s / 10次 | USER+API | 防频繁轮询 |
| `GET /seckill/batch/{id}` | `getBatchInfo` | 60s / 30次 | IP | 公开接口，防爬虫 |
| `GET /seckill/batch/list` | `listBatches` | 60s / 30次 | IP | 公开接口，防爬虫 |

> **注意**：秒杀场景 Service 层（`SeckillServiceImpl.grab`）已有 `RateLimitUtil` 调用做券级别限流。接口层 `@RateLimit` 作为**第一道防线**拦截高频请求，减少 Redis Lua 脚本执行压力。

#### 4.7.4 ReportController（`server/.../controller/ReportController.java`）

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| `POST /report/submit` | `submitReport` | 60s / 3次 | USER | 防恶意刷举报（现有 `REPORT_RATE_LIMIT_KEY` 需迁移） |
| `GET /report/my-list` | `getMyReportList` | 60s / 20次 | USER | 读操作 |
| `GET /report/detail/{id}` | `getReportDetail` | 60s / 30次 | USER | 读操作 |
| `POST /report/cancel` | `cancelReport` | 60s / 10次 | USER | 写操作 |

#### 4.7.5 FeedbackController（`server/.../controller/FeedbackController.java`）

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| `POST /feedback/upload` | `uploadAttachment` | 60s / 10次 | USER | 附件上传成本 |
| `POST /feedback/submit` | `submitFeedback` | 24h / 5次 | USER | 防刷反馈（现有 `FEEDBACK_SUBMIT_COUNT` 按天限流，需迁移） |
| `DELETE /feedback/{id}/withdraw` | `withdrawFeedback` | 60s / 10次 | USER | 写操作 |
| `GET /feedback/list` | `getMyFeedbackList` | 60s / 30次 | USER | 读操作 |
| `GET /feedback/{id}` | `getFeedbackDetail` | 60s / 30次 | USER | 读操作 |
| `POST /feedback/{id}/reply` | `replyFeedback` | 60s / 10次 | USER | 写操作 |
| `POST /feedback/{id}/reopen` | `reopenFeedback` | 60s / 5次 | USER | 敏感操作（限 2 次已有业务校验） |
| `POST /feedback/{id}/confirm` | `confirmFeedback` | 60s / 10次 | USER | 写操作 |
| `GET /feedback/stats` | `getMyFeedbackStats` | 60s / 30次 | USER | 读操作 |

#### 4.7.6 BatchTaskController

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| 批量任务提交 | `submit` | 60s / 1次 | USER | 重计算成本高（现有 `BATCH_TASK_RATE_LIMIT_*` 需迁移） |
| 进度查询 | `getProgress` | 5s / 5次 | USER | 防频繁轮询 |
| 任务列表 | `list` | 60s / 20次 | USER | 读操作 |

#### 4.7.7 CouponController

| 端点 | 方法 | 建议 | 维度 | 理由 |
|------|------|------|------|------|
| 券激活 | `activateCoupon` | 60s / 5次 | USER | 防频繁激活（现有 `COUPON_ACTIVATE_RATE_LIMIT_*` 需迁移） |

#### 4.7.8 其他 Controller（建议配置）

| Controller | 建议 | 理由 |
|------------|------|------|
| `FollowController` | 关注操作 60s/20次，取关 60s/20次 | 防批量关注/取关 |
| `FeedController` | 列表 60s/60次 | 读操作 |
| `RecommendController` | 推荐 60s/60次 | 读操作 |
| `NotificationController` | 列表 60s/60次 | 读操作 |
| `NoticeController` | 发送验证码 60s/1次 | 防短信轰炸（**重点**） |
| `CategoryController` / `TagController` | 不限流 | 低频管理操作 |
| `SpaceController` | 60s/30次 | 读操作 |
| `MainController` | 不限流 | 健康检查 |
| **admin/ 下管理端 Controller（9个）** | **加入白名单**，不限流 | 管理端操作低频，管理员已通过角色认证 |

#### 4.7.9 管理端是否限流

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| **不限流（白名单）** | 实现简单，管理端信任度高 | 管理员账号被盗时有风险 | 推荐 Phase 1 |
| 限流但阈值放宽 | 有基本保护 | 需额外配置 | Phase 3 考虑 |

**推荐**：Phase 1 将管理员角色加入白名单，管理端接口不限流。Phase 3 可选择性添加宽松阈值（如 300次/分钟）作为保险。

---

### 4.8 监控与可观测

#### 4.8.1 当前状态

项目**未引入** Micrometer / Actuator，无法使用标准的 metrics 暴露。

#### 4.8.2 Phase 1-2：日志埋点

沿用 `@RedisTimed` 的日志思路，AOP 切面内打点：

```java
// 限流通过
log.info("[RATE-LIMIT] PASS | resource={} | key={} | remaining={} | elapsed={}ms",
         resource, key, result.remaining(), elapsed);

// 限流拒绝
log.warn("[RATE-LIMIT] REJECT | resource={} | key={} | window={}s | max={}",
         resource, key, windowSeconds, maxAttempts);

// Redis 故障降级
log.error("[RATE-LIMIT] DEGRADED | resource={} | key={} | fallback={}",
          resource, key, fallbackStrategy);
```

**可观测指标**（通过日志聚合分析）：
- 限流命中率 = REJECT / (PASS + REJECT)
- 各接口 REJECT 量排行
- Redis 降级次数

#### 4.8.3 Phase 3：Micrometer 接入

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| 引入 `spring-boot-starter-actuator` + Micrometer | 标准化，可接 Prometheus/Grafana | 增加依赖 | 推荐 Phase 3 |
| 自定义 `/admin/metrics` 端点 | 轻量 | 需自行维护 | 不推荐 |

**推荐**：Phase 3 引入 `spring-boot-starter-actuator`，在 AOP 中使用 `MeterRegistry` 注册 Counter/Timer：

```java
// 伪代码
Counter.builder("ratelimit.requests")
    .tag("resource", resource)
    .tag("result", "pass/reject/degraded")
    .register(meterRegistry)
    .increment();
```

---

### 4.9 与现有散落限流的迁移路径

#### 4.9.1 现有限流点清单

| 模块 | 位置 | 实现方式 | 常量 |
|------|------|----------|------|
| 登录限流 | `UserServiceImpl.userLogin` (L189-208) | `RateLimitUtil` 滑动窗口，account 维度 | `LOGIN_RATE_LIMIT_WINDOW`(300s) / `LOGIN_RATE_LIMIT_MAX`(5) + 30min 硬锁 |
| 举报限流 | `ReportServiceImpl` | INCR + EXPIRE，userId 维度 | `REPORT_RATE_LIMIT_KEY`(60s) / `REPORT_RATE_LIMIT_MAX`(3) |
| 批量任务限流 | `BatchTaskServiceImpl` | INCR + EXPIRE，userId 维度 | `BATCH_TASK_RATE_LIMIT_KEY`(60s) / `BATCH_TASK_RATE_LIMIT_MAX`(1) |
| 券激活限流 | `CouponServiceImpl` | INCR + EXPIRE，userId 维度 | `COUPON_ACTIVATE_RATE_LIMIT_KEY`(60s) / `COUPON_ACTIVATE_RATE_LIMIT_MAX`(5) |
| 反馈限流 | `FeedbackServiceImpl` | INCR + EXPIRE，userId 维度 | `FEEDBACK_SUBMIT_COUNT`(24h / N次) |

#### 4.9.2 迁移原则

> **先建新设施、再迁旧点、最后清理。不一刀切。**

1. 新建 `@RateLimit` + AOP + 统一 429 响应，不影响现有代码
2. 逐个迁移旧限流点到注解方式
3. 确认新方式工作正常后，删除旧常量和 INCR 代码
4. 每个迁移点独立提交，可单独回滚

#### 4.9.3 迁移注意点

- **登录限流特殊**：登录限流 key 是 `account`（非 userId），且含"登录成功后清除限流记录"逻辑，迁移时需用 `CUSTOM` 维度 + SpEL `#dto.account`，并在 Service 层保留成功清除逻辑
- **INCR → 滑动窗口**：旧实现用 INCR+EXPIRE（固定窗口），新实现用滑动窗口，语义略有差异（滑动窗口更严格），需告知团队
- **反馈每日限流**：现有反馈限流是"每天 N 次"（按自然日），滑动窗口是"滚动 N 秒"，语义不同。保留现有每日限流逻辑不变，仅迁移到 `@RateLimit` 注解声明

---

### 4.10 分阶段实施计划

#### Phase 1：MVP（注解 + AOP + 统一 429 + 全局默认）

**产出物**：
| 文件 | 说明 |
|------|------|
| `common/.../annotation/RateLimit.java` | 限流注解 |
| `common/.../annotation/RateLimitDimension.java` | 维度枚举 |
| `common/.../annotation/FallbackStrategy.java` | 兜底策略枚举 |
| `common/.../exception/RateLimitException.java` | 限流异常 |
| `common/.../exception/ErrorCode.java` | 新增 `RATE_LIMIT_EXCEEDED(42900)` |
| `common/.../util/IpUtils.java` | IP 提取工具类（从 SeckillController 提取） |
| `server/.../aop/RateLimitAop.java` | 限流切面 |
| `server/.../config/RateLimitExceptionHandler.java` | 429 响应处理器 |
| `server/.../config/RateLimitProperties.java` | yml 配置绑定 |
| `server/src/main/resources/application.yml` | 新增 `ratelimit` 配置段 |

**验收标准**：
- 在 `ReportController.submitReport` 上添加 `@RateLimit(resource="report", dimensions={USER}, windowSeconds=60, maxAttempts=3)`，60 秒内第 4 次请求返回 HTTP 429 + `Retry-After` 头
- Redis 关停后请求正常放行（FAIL_OPEN 验证）

**风险点**：
- AOP 与 `LoginInterceptor` 执行顺序（需确保 Interceptor 先执行，UserContext 已设置）
- SpEL 解析的异常处理（表达式错误不应导致请求失败）

#### Phase 2：维度组合 + 配置化

**产出物**：
- `RateLimitAop` 增强：支持多维度组合 key 拼装
- `RateLimitProperties` 完善：支持按 resource 配置覆盖
- `RateLimitUtil.Result` 增强：新增 `Status` 枚举（ALLOWED/REJECTED/DEGRADED）
- `application.yml` 补充各接口的 `ratelimit.rules` 配置

**验收标准**：
- `@RateLimit(dimensions={USER, API})` 正确生成 `u:123:POST:/api/picture/upload` 格式 key
- yml 中修改某 resource 的阈值后重启生效
- `RateLimitUtil.Result.Status.DEGRADED` 正确识别 Redis 故障

**风险点**：
- 多维度组合 key 拼装顺序的一致性（需严格固定为 USER > IP > API > CUSTOM）
- yml 配置与注解默认值的合并逻辑复杂度

#### Phase 3：黑白名单 + 监控

**产出物**：
- `server/.../config/RateLimitWhitelistChecker.java` — 白名单检查器
- `server/.../config/RateLimitBlacklistChecker.java` — 黑名单检查器（Redis Set）
- 管理端 API：动态添加/移除黑名单 IP
- AOP 切面集成白名单/黑名单检查
- 引入 `spring-boot-starter-actuator`，AOP 接入 Micrometer Counter
- 可选：Redis 存储限流规则实现热更新

**验收标准**：
- 管理员用户请求被白名单跳过限流
- 恶意 IP 加入 Redis Set 后立即被拒绝
- `/actuator/metrics/ratelimit.requests` 可查看限流命中数据

**风险点**：
- 白名单/黑名单检查增加每次请求的 AOP 耗时（预期 < 1ms）
- 引入 Actuator 需要配置安全策略（不暴露敏感端点）

#### Phase 4：旧点迁移清理

**产出物**：
- 迁移 `UserServiceImpl.userLogin` 登录限流（含成功清除逻辑）
- 迁移 `ReportServiceImpl` 举报限流
- 迁移 `BatchTaskServiceImpl` 批量任务限流
- 迁移 `CouponServiceImpl` 券激活限流
- 迁移 `FeedbackServiceImpl` 反馈每日限流
- 删除 `RedisKeyConstants` 中散落的旧限流常量
- 删除各 Service 中的 INCR + EXPIRE 手写限流代码
- 将限流逻辑从 Service 层上移到 Controller 层（通过注解）

**验收标准**：
- 所有旧限流常量和代码被清理
- 功能测试：各接口限流行为与迁移前一致（或更严格）
- `RedisKeyConstants` 中仅保留 `RATE_LIMIT_KEY_PREFIX` 和 `RATE_LIMIT_RESOURCE_LOGIN` 等仍需引用的常量

**风险点**：
- 登录限流迁移最复杂（含硬锁逻辑、成功清除逻辑）
- 反馈"每日 N 次"语义与滑动窗口不同，需特殊处理（可能保留原有每日计数逻辑不变）

---

## 5. 风险与权衡

### 5.1 AOP 执行顺序

**风险**：`RateLimitAop` 与 `CheckAuthAop`（`server/.../aop/CheckAuthAop.java`）执行顺序不确定，可能导致限流检查在认证之前执行。

**应对**：使用 `@Order` 注解确保 `RateLimitAop` 在认证 AOP 之后执行（认证优先，限流次之）。或确保 `LoginInterceptor` 已在 AOP 前设置 `UserContext`。

### 5.2 滑动窗口 vs 固定窗口（INCR+EXPIRE）

| 方面 | 滑动窗口（RateLimitUtil） | 固定窗口（INCR+EXPIRE） |
|------|--------------------------|------------------------|
| 精度 | 高（真实滑动窗口） | 低（窗口边界突刺） |
| Redis 开销 | ZSET（内存更高） | STRING（内存低） |
| 原子性 | Lua 脚本 | INCR+EXPIRE 非原子（需 Lua 或 pipeline） |

**决策**：统一使用滑动窗口（RateLimitUtil），一致性优先。性能差异在当前规模可忽略。

### 5.3 限流放在 AOP 层 vs Filter 层

| 方案 | 优点 | 缺点 |
|------|------|------|
| **AOP 层（推荐）** | 可获取 UserContext、方法参数、注解元数据 | 仅能拦截 Controller 方法，无法拦截静态资源 |
| Filter 层 | 最先执行，拦截一切请求 | 无法获取 UserContext（Interceptor 未执行）、无注解支持 |

**决策**：AOP 层为主（`@RateLimit` 注解），Filter 层为辅（Phase 3 全局 QPS 兜底，保护性限流）。

### 5.4 全局限流 key 碰撞

**风险**：不同 Controller 的同名方法（如多个 `get(id)`）自动生成的 resource 名可能碰撞。

**应对**：resource 自动生成时使用**全限定类名 + 方法名**（`UserController.login`），而非简单类名。注解 `resource()` 显式指定时优先使用注解值。

### 5.5 异步接口 / WebSocket

**风险**：`RequestContextHolder` 在异步线程中不可用。

**应对**：Phase 1 标注"仅支持同步 Controller 请求"。如项目后续有 WebSocket 限流需求，需在 WebSocket Handler 层单独处理。

---

## 6. 附录

### 6.1 关键类路径速查表

| 类/文件 | 路径 | 角色 |
|---------|------|------|
| `RateLimitUtil` | `common/src/main/java/org/example/common/util/RateLimitUtil.java` | 底层限流器（复用） |
| `rate_limit_sliding_window.lua` | `server/src/main/resources/scripts/rate_limit_sliding_window.lua` | Lua 脚本（复用） |
| `RedisConfig` | `server/src/main/java/org/example/server/config/RedisConfig.java` | Redis 配置（已注册 rateLimitScript Bean） |
| `RedisKeyConstants` | `common/src/main/java/org/example/common/constants/RedisKeyConstants.java` | Redis Key 常量（待清理） |
| `BaseResponse<T>` | `common/src/main/java/org/example/common/result/BaseResponse.java` | 统一响应体 |
| `ResultUtils` | `common/src/main/java/org/example/common/result/ResultUtils.java` | 响应工具类 |
| `ErrorCode` | `common/src/main/java/org/example/common/exception/ErrorCode.java` | 错误码枚举（待新增 42900） |
| `BusinessException` | `common/src/main/java/org/example/common/exception/BusinessException.java` | 业务异常 |
| `GlobalExceptionHandler` | `common/src/main/java/org/example/common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `UserContext` | `common/src/main/java/org/example/common/context/UserContext.java` | 用户上下文 ThreadLocal |
| `UserEnum` | `common/src/main/java/org/example/common/enums/UserEnum.java` | 用户角色枚举（admin/user） |
| `RedisTimed` | `common/src/main/java/org/example/common/annotation/RedisTimed.java` | Redis 耗时监控注解（参考范式） |
| `RedisTimedAop` | `server/src/main/java/org/example/server/aop/RedisTimedAop.java` | Redis 耗时监控切面（参考范式） |
| `application.yml` | `server/src/main/resources/application.yml` | 应用配置 |

### 6.2 现有限流常量待清理清单

| 常量 | 位置 | 当前值 | 迁移目标 |
|------|------|--------|----------|
| `RATE_LIMIT_KEY_PREFIX` | `RedisKeyConstants.java:164` | `"rate_limit"` | 保留（RateLimitUtil 内部使用） |
| `RATE_LIMIT_RESOURCE_LOGIN` | `RedisKeyConstants.java:167` | `"login"` | 保留 |
| `LOGIN_RATE_LIMIT_WINDOW` | `RedisKeyConstants.java:170` | `300`(5min) | 迁移到 yml |
| `LOGIN_RATE_LIMIT_MAX` | `RedisKeyConstants.java:173` | `5` | 迁移到 yml |
| `LOGIN_LOCK_WINDOW` | `RedisKeyConstants.java:176` | `1800`(30min) | 迁移到 yml |
| `REPORT_RATE_LIMIT_KEY` | `RedisKeyConstants.java:383` | `rate_limit:report:{userId}` | 删除（注解替代） |
| `REPORT_RATE_LIMIT_WINDOW` | `RedisKeyConstants.java:452` | `60` | 删除 |
| `REPORT_RATE_LIMIT_MAX` | `RedisKeyConstants.java:455` | `3` | 删除 |
| `BATCH_TASK_RATE_LIMIT_KEY` | `RedisKeyConstants.java:487` | `rate_limit:batch:{userId}` | 删除 |
| `BATCH_TASK_RATE_LIMIT_WINDOW` | `RedisKeyConstants.java:489` | `60` | 删除 |
| `BATCH_TASK_RATE_LIMIT_MAX` | `RedisKeyConstants.java:491` | `1` | 删除 |
| `COUPON_ACTIVATE_RATE_LIMIT_KEY` | `RedisKeyConstants.java:594` | `rate_limit:coupon:activate:{userId}` | 删除 |
| `COUPON_ACTIVATE_RATE_LIMIT_WINDOW` | `RedisKeyConstants.java:596` | `60` | 删除 |
| `COUPON_ACTIVATE_RATE_LIMIT_MAX` | `RedisKeyConstants.java:598` | `5` | 删除 |
| `FEEDBACK_SUBMIT_COUNT` | `RedisKeyConstants.java:328` | `feedback:submit:count:` | 保留（每日限流语义不同） |

### 6.3 新增文件清单

| 文件 | 模块 | Phase |
|------|------|-------|
| `common/.../annotation/RateLimit.java` | common | 1 |
| `common/.../annotation/RateLimitDimension.java` | common | 1 |
| `common/.../annotation/FallbackStrategy.java` | common | 1 |
| `common/.../exception/RateLimitException.java` | common | 1 |
| `common/.../util/IpUtils.java` | common | 1 |
| `server/.../aop/RateLimitAop.java` | server | 1 |
| `server/.../config/RateLimitExceptionHandler.java` | server | 1 |
| `server/.../config/RateLimitProperties.java` | server | 1 |
| `server/.../config/RateLimitWhitelistChecker.java` | server | 3 |
| `server/.../config/RateLimitBlacklistChecker.java` | server | 3 |

---

> 本方案为架构设计文档，不包含实现代码。实施时请按 Phase 顺序逐步推进，每个 Phase 完成后验收通过再进入下一阶段。
