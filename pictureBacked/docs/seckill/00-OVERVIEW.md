# 秒杀编码券系统 — 分段实施总览

> 基于 PLAN-SECKILL.md + PLAN-SECKILL-MEMBERSHIP-SUPPLEMENT.md，拆分为可独立执行的分段实施文档。

---

## 阶段总览

| 阶段 | 文档 | 优先级 | 依赖 | 核心产出 |
|------|------|--------|------|---------|
| Phase 1 | [01-infrastructure.md](01-infrastructure.md) | P0 | 无 | 数据库建表、Entity/Mapper/Service 骨架、Redis Key、Lua 脚本、MQ 配置 |
| Phase 2 | [02-membership-foundation.md](02-membership-foundation.md) | P0 | Phase 1 | User 会员字段、枚举类、CheckAuth 扩展、空间权限改造、VIP 工具方法 |
| Phase 3 | [03-seckill-core.md](03-seckill-core.md) | P0 | Phase 1 | 预热服务、秒杀抢购接口、MQ 消费者、结果查询、降级策略 |
| Phase 4 | [04-coupon-membership-lifecycle.md](04-coupon-membership-lifecycle.md) | P0 | Phase 2 + Phase 3 | 券包管理、激活接口、VIP 状态查询、退款、过期降级、到期提醒、库存对账 |
| Phase 5 | [05-admin-operations.md](05-admin-operations.md) | P1 | Phase 4 | 管理后台批次/券/VIP 管理、数据看板 |
| Phase 6 | [06-optimization-hardening.md](06-optimization-hardening.md) | P2 | Phase 4 | 动态链接签名、设备指纹、活动预约、数据埋点 |

---

## 依赖关系图

```
Phase 1 (基础设施)
   ├── Phase 2 (会员基础) ──┐
   └── Phase 3 (秒杀核心) ──┤
                            ├── Phase 4 (券+会员生命周期)
                            │      └── Phase 5 (管理后台)
                            │             └── Phase 6 (优化加固)
```

**Phase 2 和 Phase 3 可并行开发**，Phase 2 改造的是现有代码（User/CheckAuth/Space），Phase 3 是新增的秒杀代码，两者互不冲突。

---

## 各阶段产出清单

### Phase 1 — 基础设施
- `sql/seckill_tables.sql` — 3 张新表 + User 表 ALTER
- `pojo/entity/CodeCouponBatch.java` — 批次实体
- `pojo/entity/CodeCoupon.java` — 编码券实体
- `pojo/entity/SeckillOrder.java` — 秒杀订单实体
- `server/mapper/CodeCouponBatchMapper.java` + XML
- `server/mapper/CodeCouponMapper.java` + XML
- `server/mapper/SeckillOrderMapper.java` + XML
- `server/service/SeckillService.java` — 秒杀服务接口
- `server/service/CouponService.java` — 编码券服务接口
- `common/constants/RedisKeyConstants.java` — 新增秒杀模块 Key
- `resources/lua/seckill_deduct.lua` — 扣库存脚本
- `resources/lua/seckill_rollback.lua` — 库存归还脚本
- `server/config/RabbitMQConfig.java` — 新增秒杀队列配置

### Phase 2 — 会员基础
- `common/enums/VipTypeEnum.java` — VIP 类型枚举
- `common/enums/CouponStatusEnum.java` — 券状态枚举
- `pojo/entity/User.java` — 新增 4 个 VIP 字段
- `pojo/vo/LoginUserVO.java` — 新增 VIP 字段
- `pojo/vo/UserProfileVO.java` — 新增 VIP 字段
- `common/annotation/CheckAuth.java` — 新增 requireVip 属性
- `server/aop/CheckAuthAop.java` — 新增 VIP 校验逻辑
- `common/util/VipUtil.java` — VIP 状态判断工具方法
- `server/service/impl/SpaceServiceImpl.java` — 权限逻辑改造
- `server/resources/mapper/UserMapper.xml` — Base_Column_List 更新

### Phase 3 — 秒杀核心
- `server/service/impl/SeckillServiceImpl.java` — 秒杀核心逻辑
- `server/controller/SeckillController.java` — 秒杀抢购 API
- `server/service/mq/SeckillConsumer.java` — MQ 消费者
- `server/service/SeckillDegradationService.java` — 降级服务
- `server/scheduled/SeckillScheduled.java` — 预热 + 对账定时任务
- `pojo/dto/seckill/SeckillGrabDTO.java` — 抢购请求 DTO
- `pojo/vo/seckill/SeckillResultVO.java` — 抢购结果 VO
- `pojo/dto/seckill/SeckillMessage.java` — MQ 消息 DTO

### Phase 4 — 券 + 会员生命周期
- `server/service/impl/CouponServiceImpl.java` — 券管理 + 激活实现
- `server/controller/CouponController.java` — 券管理 API
- `server/controller/UserController.java` — VIP 状态查询 API
- `pojo/vo/CouponVO.java` — 券列表 VO
- `pojo/vo/CouponActivateVO.java` — 激活结果 VO
- `pojo/vo/VipStatusVO.java` — VIP 状态 VO
- `server/scheduled/VipScheduled.java` — VIP 过期 + 提醒定时任务
- `server/scheduled/CouponExpireScheduled.java` — 券过期 + 库存归还

### Phase 5 — 管理后台
- `server/controller/admin/AdminBatchController.java` — 批次管理
- `server/controller/admin/AdminCouponController.java` — 券管理
- `server/controller/admin/AdminVipController.java` — 会员管理
- `pojo/dto/seckill/BatchCreateDTO.java` — 创建批次 DTO
- `pojo/vo/seckill/SeckillStatsVO.java` — 秒杀统计 VO
- `pojo/vo/VipStatsVO.java` — 会员统计 VO

### Phase 6 — 优化加固
- 秒杀链接动态签名
- 设备指纹防刷
- 活动预约/提醒
- 数据埋点与运营指标

---

## 开发原则

1. **每个 Phase 完成后可独立部署验证**，不依赖后续 Phase
2. **Phase 2 和 Phase 3 并行开发**，各自在不同包/文件中工作，合并无冲突
3. **优先保证 Phase 1-4 完成**（P0），Phase 5-6 可迭代
4. **所有 SQL 变更放入 `sql/` 目录**，标注 Phase 和顺序
5. **遵循现有项目规范**：Entity 用 MyBatis-Plus 注解、Service 分接口/实现、VO 不暴露敏感字段
