-- ============================================================
-- 秒杀编码券系统建表脚本
-- ============================================================

-- 1. 编码券发放批次
CREATE TABLE IF NOT EXISTS code_coupon_batch (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no        VARCHAR(32) UNIQUE NOT NULL COMMENT '批次号',
    name            VARCHAR(128) NOT NULL COMMENT '批次名称',
    type            TINYINT NOT NULL COMMENT '券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP',
    total_stock     INT NOT NULL COMMENT '总库存',
    current_stock   INT NOT NULL DEFAULT 0 COMMENT '当前剩余库存',
    start_time      DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time        DATETIME DEFAULT NULL COMMENT '秒杀结束时间',
    status          TINYINT DEFAULT 0 COMMENT '0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    editTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete        TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status_time (status, start_time)
) COMMENT '编码券发放批次' COLLATE = utf8mb4_unicode_ci;

-- 2. 编码券
CREATE TABLE IF NOT EXISTS code_coupon (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id        BIGINT NOT NULL COMMENT '批次ID',
    user_id         BIGINT DEFAULT NULL COMMENT '用户ID',
    code            VARCHAR(32) UNIQUE NOT NULL COMMENT '唯一编码券码',
    type            TINYINT NOT NULL COMMENT '3/7/30天',
    status          TINYINT DEFAULT 0 COMMENT '0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款',
    issued_at       DATETIME DEFAULT NULL COMMENT '领取时间',
    activated_at    DATETIME DEFAULT NULL COMMENT '激活时间',
    expire_at       DATETIME NOT NULL COMMENT '过期时间',
    version         INT DEFAULT 0,
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete        TINYINT DEFAULT 0 NOT NULL,
    UNIQUE KEY uk_code (code),
    KEY idx_batch_user (batch_id, user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_status_expire (status, expire_at)
) COMMENT '编码券' COLLATE = utf8mb4_unicode_ci;

-- 3. 秒杀订单（防重）
CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    coupon_id       BIGINT DEFAULT NULL COMMENT '编码券ID（异步写入后回填）',
    order_no        VARCHAR(32) UNIQUE NOT NULL,
    status          TINYINT DEFAULT 0 COMMENT '0-处理中, 1-成功, 2-失败',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_batch (user_id, batch_id),
    KEY idx_order_no (order_no)
) COMMENT '秒杀订单' COLLATE = utf8mb4_unicode_ci;

-- 4. 用户表添加 VIP 字段
ALTER TABLE `user`
    ADD COLUMN `vipType`          TINYINT    NOT NULL DEFAULT 0 COMMENT '0-普通, 1-VIP' AFTER `userRole`,
    ADD COLUMN `vipExpireTime`    DATETIME   DEFAULT NULL COMMENT 'VIP到期时间' AFTER `vipType`,
    ADD COLUMN `vipActivatedAt`   DATETIME   DEFAULT NULL COMMENT '最近激活VIP时间' AFTER `vipExpireTime`,
    ADD COLUMN `vipTotalDays`     INT        NOT NULL DEFAULT 0 COMMENT 'VIP累计总天数' AFTER `vipActivatedAt`,
    ADD INDEX `idx_vip_status` (`vipType`, `vipExpireTime`);
