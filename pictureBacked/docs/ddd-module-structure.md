# DDD 多模块结构说明

> 分支：`refactor/ddd-multi-module`，自 2026-09 起生效。

## 一、模块总览

| 模块 | 包根 | 职责 | 依赖 |
|------|------|------|------|
| `shared` | `org.example.shared` | 共享内核：统一响应/异常、横切注解、通用工具、发布语言契约 | 无（最底层） |
| `identity` | `org.example.identity` | 身份与认证：用户、登录注册、验证码、会话契约 | shared |
| `notification` | `org.example.notification` | 通知：站内通知、系统消息、SSE 推送 | shared, identity |
| `picture` | `org.example.picture` | 图片核心：core / social / moderation / space 四个子域 | shared, identity, notification |
| `marketing` | `org.example.marketing` | 营销：秒杀、优惠券、VIP | shared, identity, picture, notification |
| `bootstrap` | `org.example.bootstrap` | 启动装配：入口、横切配置、端口适配器 | 全部 |

**依赖方向（DAG，禁止反向/循环）：**

```
shared ← identity ← notification ← picture ← marketing ← bootstrap
```

## 二、子域包结构约定

每个上下文/子域内部统一四层：

```
org.example.<ctx>
├── api/            # 对外契约：UserContext/User 常量、端口接口、事件（仅 identity/notification 有）
├── interfaces/     # Controller + dto + vo（含 admin/、benchmark/）
├── application/    # 应用服务（impl/ 装饰器缓存）、scheduling/ 定时任务、messaging/ MQ 消费者
├── domain/         # 实体、领域枚举、常量
└── infrastructure/ # persistence/ Mapper+xml、外部网关、MQ/Redis 声明、端口适配器
```

picture 模块按子域再分一层：`org.example.picture.{core|social|moderation|space}.*`。

## 三、跨上下文协作规则

1. **只允许依赖下游模块**（按上表），编译期由 Maven 强制。
2. **跨上下文取数走端口（防腐层）**，禁止直接注入他域 Mapper：
   - `identity.api.port.UserContentStatsPort` ← `picture.core.infrastructure.adapter.UserContentStatsAdapter`
   - `identity.api.port.FollowStatsPort` ← `picture.social.infrastructure.adapter.FollowStatsAdapter`
   - `notification.api.port.UserLookupPort` ← `bootstrap.adapter.UserLookupAdapter`
3. **跨上下文通知走事件**：`NotificationEvent` + `NotificationTypeEnum` 位于 `shared.contract`（发布语言），由 `notification.application.event.NotificationEventListener` 统一消费（落库 + 未读数 + SSE 推送）。
4. **MQ 队列按归属声明**：
   - `system.message.*` → notification；`batch.picture.*` → picture.core；`social.action.*` → picture.social；`seckill.order.*` → marketing
   - 死信监听统一在 `bootstrap.mq.DeadLetterConsumer`
5. **事务边界**：事务内调用他域服务（如 PictureServiceImpl 事务内 save 通知）依赖同 JVM 直调，若未来拆微服务需引入 Outbox。

## 四、新增代码落位指南

- 新 Controller → 所属上下文 `interfaces/`；新表 → 实体放对应 `domain/model/`，Mapper 放 `infrastructure/persistence/` + XML 放该模块 `resources/mapper/`（启动类 `@MapperScan` 需同步补包）。
- 新定时任务 → 所属上下文 `application/scheduling/`。
- 他域数据需求 → 优先事件；实时性要求高则定义端口 + 适配器，勿反向加依赖。

## 五、已知债务（后续迭代）

- `RedisKeyConstants`（shared）未按域前缀拆分，36 处引用横跨全部上下文。
- User 聚合的 VIP 字段（vipType/vipExpireTime）写权仍在 marketing（CouponServiceImpl/VipScheduled 直改），应事件化。
- 领域模型仍是贫血 PO（MyBatis-Plus 注解直接标在实体上），充血模型/仓储接口按模块渐进改造。
- `application.yml` 数据库/邮箱凭据为明文，建议环境变量化。

## 六、构建备注

公司镜像 `repo.dtyunxi.cn` 不可达时，可临时指定公共镜像构建：

```bash
mvn -s <(见分支说明的临时 settings) clean package -DskipTests
```

（临时 settings 内容：mirror 指向 `https://maven.aliyun.com/repository/public`，mirrorOf 覆盖 dtyunxi 各仓库 id。）
