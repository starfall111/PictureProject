create database if not exists picture;

use picture;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPhone    varchar(16) comment '手机号',
    userEmail    varchar(256) comment '邮箱',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    UNIQUE KEY uk_userPhone (userPhone),
    UNIQUE KEY uk_userEmail (userEmail),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    categoryId   bigint                             null comment '分类 id',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_categoryId (categoryId),     -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url',
    ADD COLUMN originUrl    varchar(512) NULL COMMENT '缩略图 url';
-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

-- 添加热度分数字段（推荐系统持久化）
ALTER TABLE picture
    ADD COLUMN hotScore DOUBLE DEFAULT 0 NOT NULL COMMENT '热度分数（推荐算法计算）';

-- 创建热度分数索引（热度降级查询）
CREATE INDEX idx_hotScore ON picture (hotScore);


create table if not exists category
(
    id         bigint auto_increment comment 'id' primary key,
    name       varchar(128)                       not null comment '分类名称',
    count      int      default 0,
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name) -- 提升基于分类名称的查询性能
) comment '分类' collate = utf8mb4_unicode_ci;

create table if not exists tag
(
    id         bigint auto_increment comment 'id' primary key,
    name       varchar(128)                       not null comment 'tag名称',
    count      int      default 0,
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name) -- 提升基于tag名称的查询性能
) comment '标签' collate = utf8mb4_unicode_ci;

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;


-- 社交功能表（点赞、收藏、分享、统计）

-- 点赞表
CREATE TABLE IF NOT EXISTS picture_like
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    pictureId  BIGINT NOT NULL,
    userId     BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_picture_user (pictureId, userId),
    KEY idx_picture_id (pictureId),
    KEY idx_user_id (userId)
) COMMENT '图片点赞' COLLATE = utf8mb4_unicode_ci;

-- 收藏表
CREATE TABLE IF NOT EXISTS picture_favorite
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    pictureId  BIGINT NOT NULL,
    userId     BIGINT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_picture_user (pictureId, userId),
    KEY idx_picture_id (pictureId),
    KEY idx_user_id (userId)
) COMMENT '图片收藏' COLLATE = utf8mb4_unicode_ci;

-- 图片统计表
CREATE TABLE IF NOT EXISTS picture_statistics
(
    pictureId     BIGINT PRIMARY KEY,
    likeCount     INT DEFAULT 0,
    favoriteCount INT DEFAULT 0,
    shareCount    INT DEFAULT 0,
    viewCount     int default 0 null,
    downloadCount int default 0 null,
    KEY idx_like_count (likeCount),
    KEY idx_favorite_count (favoriteCount)
) COMMENT '图片统计' COLLATE = utf8mb4_unicode_ci;

-- 系统消息表
CREATE TABLE IF NOT EXISTS systemMessage
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    title               VARCHAR(255)  NOT NULL COMMENT '标题',
    content             VARCHAR(2048) NOT NULL COMMENT '内容',
    sendMode            VARCHAR(20)   NOT NULL DEFAULT 'DIRECT' COMMENT '发送模式: DIRECT=直接批量插入 / RABBITMQ=消息队列',
    targetType          VARCHAR(20)   NOT NULL DEFAULT 'ALL' COMMENT '目标类型: ALL=全体用户 / FILTER=条件筛选',
    filterRole          VARCHAR(20)            DEFAULT NULL COMMENT '按角色筛选: user / admin',
    filterSpaceLevel    INT                    DEFAULT NULL COMMENT '按空间等级筛选: 0=普通 1=专业 2=旗舰',
    filterRegisterStart DATETIME               DEFAULT NULL COMMENT '注册时间起始',
    filterRegisterEnd   DATETIME               DEFAULT NULL COMMENT '注册时间截止',
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0=草稿 1=已发布 2=已下架',
    publisherId         BIGINT        NOT NULL COMMENT '发布管理员ID',
    publishTime         DATETIME               DEFAULT NULL COMMENT '发布时间',
    createTime          DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime          DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete            TINYINT(1)             DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status, isDelete)
) COMMENT '系统消息' COLLATE = utf8mb4_unicode_ci;


-- 站内通知表
CREATE TABLE IF NOT EXISTS notification
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiverId   BIGINT       NOT NULL COMMENT '接收者用户ID',
    senderId     BIGINT       DEFAULT NULL COMMENT '触发者用户ID',
    senderName   VARCHAR(128) DEFAULT NULL COMMENT '触发者昵称(冗余)',
    senderAvatar VARCHAR(512) DEFAULT NULL COMMENT '触发者头像(冗余)',
    type         VARCHAR(20)  NOT NULL COMMENT 'LIKE/FAVORITE/COMMENT/FOLLOW/SYSTEM',
    title        VARCHAR(255) NOT NULL COMMENT '通知标题',
    content      VARCHAR(512) DEFAULT NULL COMMENT '通知内容',
    resourceId   BIGINT       DEFAULT NULL COMMENT '关联资源ID',
    resourceUrl  VARCHAR(512) DEFAULT NULL COMMENT '关联资源链接',
    isRead       TINYINT(1)   DEFAULT 0 COMMENT '0=未读 1=已读',
    createTime   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    isDelete     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_receiver_read (receiverId, isRead, isDelete),
    INDEX idx_receiver_type (receiverId, type, isDelete)
) COMMENT '站内通知' COLLATE = utf8mb4_unicode_ci;

-- 用户关注表
CREATE TABLE IF NOT EXISTS user_follow
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    followerId BIGINT NOT NULL COMMENT '关注者用户ID',
    followeeId BIGINT NOT NULL COMMENT '被关注者用户ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follower_followee (followerId, followeeId),
    KEY idx_follower (followerId),
    KEY idx_followee (followeeId)
) COMMENT '用户关注关系' COLLATE = utf8mb4_unicode_ci;

-- 批量获取图片任务表
CREATE TABLE IF NOT EXISTS `batch_task`
(
    `id`           BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `userId`       BIGINT       NOT NULL COMMENT '发起用户ID',
    `spaceId`      BIGINT                DEFAULT NULL COMMENT '目标空间ID',
    `searchText`   VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    `searchSource` VARCHAR(20)  NOT NULL DEFAULT 'pexels' COMMENT '搜索来源: pexels/bing',
    `totalCount`   INT          NOT NULL DEFAULT 0 COMMENT '计划处理总数',
    `successCount` INT          NOT NULL DEFAULT 0 COMMENT '成功数量',
    `failCount`    INT          NOT NULL DEFAULT 0 COMMENT '失败数量',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    `categoryId`   BIGINT                DEFAULT NULL COMMENT '分类ID',
    `namePrefix`   VARCHAR(100)          DEFAULT NULL COMMENT '图片名称前缀',
    `tags`         TEXT                  DEFAULT NULL COMMENT '标签JSON数组',
    `errorMessage` TEXT                  DEFAULT NULL COMMENT '整体错误信息',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `finishTime`   DATETIME              DEFAULT NULL COMMENT '任务完成时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_task_user` (`userId`, `isDelete`),
    KEY `idx_task_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='批量获取图片任务表';

-- =============================================
-- 反馈-举报-封禁 建表脚本
-- =============================================

-- 1. 反馈主表
CREATE TABLE IF NOT EXISTS `feedback`
(
    `id`                BIGINT       NOT NULL COMMENT '反馈ID（雪花算法）',
    `userId`            BIGINT                DEFAULT NULL COMMENT '提交者用户ID（匿名反馈为NULL）',
    `title`             VARCHAR(100) NOT NULL COMMENT '反馈标题',
    `content`           TEXT         NOT NULL COMMENT '内容描述',
    `type`              VARCHAR(20)  NOT NULL COMMENT '类型：BUG/FEATURE/ACCOUNT/EXPERIENCE/OTHER',
    `priority`          VARCHAR(5)   NOT NULL DEFAULT 'P3' COMMENT '优先级：P0/P1/P2/P3',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/REJECTED/REOPENED/CLOSED',
    `source`            VARCHAR(20)  NOT NULL DEFAULT 'APP' COMMENT '来源：APP/SYSTEM/ADMIN',
    `relatedPictureId`  BIGINT                DEFAULT NULL COMMENT '关联图片ID（可选）',
    `relatedUserId`     BIGINT                DEFAULT NULL COMMENT '关联用户ID（可选）',
    `handlerId`         BIGINT                DEFAULT NULL COMMENT '当前处理人管理员ID',
    `isAnonymous`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名提交',
    `closeReason`       VARCHAR(50)           DEFAULT NULL COMMENT '关闭原因：USER_WITHDRAW/AUTO_CLOSE/ADMIN_CLOSE/USER_CONFIRM',
    `reopenCount`       INT          NOT NULL DEFAULT 0 COMMENT '重新打开次数，上限2',
    `convertedReportId` BIGINT                DEFAULT NULL COMMENT '转为举报后的report.id',
    `createTime`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_feedback_user` (`userId`, `isDelete`),
    KEY `idx_feedback_status` (`status`, `isDelete`),
    KEY `idx_feedback_handler` (`handlerId`, `status`, `isDelete`),
    KEY `idx_feedback_type_priority` (`type`, `priority`, `isDelete`),
    KEY `idx_feedback_create_time` (`createTime` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='反馈表';

-- 2. 反馈回复表
CREATE TABLE IF NOT EXISTS `feedback_reply`
(
    `id`         BIGINT      NOT NULL COMMENT '回复ID（雪花算法）',
    `feedbackId` BIGINT      NOT NULL COMMENT '关联反馈ID',
    `userId`     BIGINT      NOT NULL COMMENT '回复者用户ID',
    `replyType`  VARCHAR(20) NOT NULL COMMENT '回复类型：USER_REPLY/ADMIN_REPLY/INTERNAL_NOTE',
    `content`    TEXT        NOT NULL COMMENT '回复内容',
    `createTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDelete`   TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_reply_feedback` (`feedbackId`, `createTime` ASC, `isDelete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='反馈回复表';

-- 3. 反馈状态日志表
CREATE TABLE IF NOT EXISTS `feedback_status_log`
(
    `id`           BIGINT      NOT NULL COMMENT '日志ID',
    `feedbackId`   BIGINT      NOT NULL COMMENT '关联反馈ID',
    `fromStatus`   VARCHAR(20)          DEFAULT NULL COMMENT '变更前状态',
    `toStatus`     VARCHAR(20) NOT NULL COMMENT '变更后状态',
    `operatorId`   BIGINT      NOT NULL COMMENT '操作人ID',
    `operatorType` VARCHAR(10) NOT NULL COMMENT '操作人类型：USER/ADMIN/SYSTEM',
    `remark`       VARCHAR(500)         DEFAULT NULL COMMENT '备注',
    `createTime`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_log_feedback` (`feedbackId`, `createTime` ASC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='反馈状态日志表';

-- 4. 反馈附件表
CREATE TABLE IF NOT EXISTS `feedback_attachment`
(
    `id`         BIGINT       NOT NULL COMMENT '附件ID',
    `feedbackId` BIGINT       NOT NULL COMMENT '关联反馈ID',
    `fileUrl`    VARCHAR(500) NOT NULL COMMENT 'OSS 文件 URL',
    `fileName`   VARCHAR(200)          DEFAULT NULL COMMENT '原始文件名',
    `fileSize`   BIGINT                DEFAULT NULL COMMENT '文件大小（字节）',
    `fileType`   VARCHAR(20)           DEFAULT NULL COMMENT '文件类型：PNG/JPG/JPEG/GIF',
    `sortOrder`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDelete`   TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_attachment_feedback` (`feedbackId`, `sortOrder`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='反馈附件表';

-- 5. 举报表
CREATE TABLE IF NOT EXISTS `report`
(
    `id`               BIGINT      NOT NULL COMMENT '举报ID（雪花算法）',
    `reporterId`       BIGINT      NOT NULL COMMENT '举报人ID',
    `targetType`       VARCHAR(20) NOT NULL COMMENT '举报对象类型：PICTURE/USER',
    `targetId`         BIGINT      NOT NULL COMMENT '举报目标ID',
    `reasonType`       VARCHAR(20) NOT NULL COMMENT '举报原因：PORNOGRAPHY/VIOLENCE/SCAM/COPYRIGHT/SPAM/HARASSMENT/OTHER',
    `description`      TEXT                 DEFAULT NULL COMMENT '补充描述',
    `status`           VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/APPROVED/REJECTED/WARN_ONLY/REMOVE_ONLY',
    `handlerId`        BIGINT               DEFAULT NULL COMMENT '处理人ID',
    `handleResult`     VARCHAR(50)          DEFAULT NULL COMMENT '处理结果：WARN/BAN_TEMP_3/BAN_TEMP_7/BAN_TEMP_30/BAN_PERMANENT/REMOVE_PICTURE/REJECT',
    `handleReason`     TEXT                 DEFAULT NULL COMMENT '处理理由',
    `handleTime`       DATETIME             DEFAULT NULL COMMENT '处理时间',
    `reportCount`      INT         NOT NULL DEFAULT 0 COMMENT '该目标累计被举报次数',
    `sourceFeedbackId` BIGINT               DEFAULT NULL COMMENT '来源反馈ID（从反馈转来时填写）',
    `createTime`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`targetType`, `targetId`),
    KEY `idx_status_count` (`status`, `reportCount` DESC, `createTime`),
    KEY `idx_reporter_dup` (`reporterId`, `targetType`, `targetId`),
    KEY `idx_target_count` (`targetType`, `targetId`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='举报表';

-- 6. 封禁记录表
CREATE TABLE IF NOT EXISTS `ban_record`
(
    `id`              BIGINT       NOT NULL COMMENT '封禁记录ID（雪花算法）',
    `userId`          BIGINT       NOT NULL COMMENT '被封禁用户ID',
    `banType`         VARCHAR(20)  NOT NULL COMMENT '封禁类型：TEMP/PERMANENT',
    `banDuration`     INT                   DEFAULT NULL COMMENT '封禁天数（3/7/30）',
    `banStartTime`    DATETIME     NOT NULL COMMENT '封禁开始时间',
    `banEndTime`      DATETIME              DEFAULT NULL COMMENT '封禁结束时间（永久封禁为NULL）',
    `banReason`       VARCHAR(200) NOT NULL COMMENT '封禁原因',
    `violationCount`  INT          NOT NULL DEFAULT 0 COMMENT '封禁时累计违规次数',
    `unbanned`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已解封',
    `unbanTime`       DATETIME              DEFAULT NULL COMMENT '解封时间',
    `unbanReason`     TEXT                  DEFAULT NULL COMMENT '解封理由',
    `unbanOperatorId` BIGINT                DEFAULT NULL COMMENT '解封操作人ID',
    `banOperatorId`   BIGINT       NOT NULL COMMENT '封禁操作人ID',
    `banOperatorName` VARCHAR(100) NOT NULL COMMENT '封禁操作人姓名',
    `reportId`        BIGINT                DEFAULT NULL COMMENT '关联举报ID',
    `createTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`userId`, `banStartTime` DESC),
    KEY `idx_unbanned_end` (`unbanned`, `banEndTime`),
    KEY `idx_ban_end` (`banEndTime`, `unbanned`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='封禁记录表';

-- 7. User 表扩展
ALTER TABLE `user`
    ADD COLUMN `banStatus`         VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '封禁状态：NONE/TEMP/PERMANENT' AFTER `userRole`,
    ADD COLUMN `banEndTime`        DATETIME DEFAULT NULL COMMENT '封禁结束时间' AFTER `banStatus`,
    ADD COLUMN `violationCount`    INT         NOT NULL DEFAULT 0 COMMENT '累计违规次数（6个月滚动窗口）' AFTER `banEndTime`,
    ADD COLUMN `lastViolationTime` DATETIME DEFAULT NULL COMMENT '最近一次违规时间' AFTER `violationCount`,
    ADD INDEX `idx_ban_status` (`banStatus`, `banEndTime`);


-- ============================================================
-- 秒杀编码券系统建表脚本
-- ============================================================

-- 1. 编码券发放批次
CREATE TABLE IF NOT EXISTS code_coupon_batch
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no      VARCHAR(32) UNIQUE                 NOT NULL COMMENT '批次号',
    name          VARCHAR(128)                       NOT NULL COMMENT '批次名称',
    type          TINYINT                            NOT NULL COMMENT '券类型: 3-3天VIP, 7-7天VIP, 30-30天VIP',
    total_stock   INT                                NOT NULL COMMENT '总库存',
    current_stock INT                                NOT NULL DEFAULT 0 COMMENT '当前剩余库存',
    start_time    DATETIME                           NOT NULL COMMENT '秒杀开始时间',
    end_time      DATETIME DEFAULT NULL COMMENT '秒杀结束时间',
    status        TINYINT  DEFAULT 0 COMMENT '0-草稿, 1-预热中, 2-进行中, 3-已结束, 4-已取消',
    version       INT      DEFAULT 0 COMMENT '乐观锁版本号',
    editTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete      TINYINT  DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status_time (status, start_time)
) COMMENT '编码券发放批次' COLLATE = utf8mb4_unicode_ci;

-- 2. 编码券
CREATE TABLE IF NOT EXISTS code_coupon
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id     BIGINT                             NOT NULL COMMENT '批次ID',
    user_id      BIGINT   DEFAULT NULL COMMENT '用户ID',
    code         VARCHAR(32) UNIQUE                 NOT NULL COMMENT '唯一编码券码',
    type         TINYINT                            NOT NULL COMMENT '3/7/30天',
    status       TINYINT  DEFAULT 0 COMMENT '0-未发放, 1-已领取未使用, 2-已激活使用中, 3-已过期, 4-已退款',
    issued_at    DATETIME DEFAULT NULL COMMENT '领取时间',
    activated_at DATETIME DEFAULT NULL COMMENT '激活时间',
    expire_at    DATETIME                           NOT NULL COMMENT '过期时间',
    version      INT      DEFAULT 0,
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT  DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_code (code),
    KEY idx_batch_user (batch_id, user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_status_expire (status, expire_at)
) COMMENT '编码券' COLLATE = utf8mb4_unicode_ci;

-- 3. 秒杀订单（防重）
CREATE TABLE IF NOT EXISTS seckill_order
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    userId    BIGINT                             NOT NULL,
    batchId   BIGINT                             NOT NULL,
    couponId  BIGINT   DEFAULT NULL COMMENT '编码券ID（异步写入后回填）',
    orderNo   VARCHAR(32) UNIQUE                 NOT NULL,
    status     TINYINT  DEFAULT 0 COMMENT '0-处理中, 1-成功, 2-失败',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_batch (userId, batchId),
    KEY idx_order_no (orderNo)
) COMMENT '秒杀订单' COLLATE = utf8mb4_unicode_ci;

-- 4. 用户表添加 VIP 字段
ALTER TABLE `user`
    ADD COLUMN `vipType`        TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通, 1-VIP' AFTER `userRole`,
    ADD COLUMN `vipExpireTime`  DATETIME DEFAULT NULL COMMENT 'VIP到期时间' AFTER `vipType`,
    ADD COLUMN `vipActivatedAt` DATETIME DEFAULT NULL COMMENT '最近激活VIP时间' AFTER `vipExpireTime`,
    ADD COLUMN `vipTotalDays`   INT     NOT NULL DEFAULT 0 COMMENT 'VIP累计总天数' AFTER `vipActivatedAt`,
    ADD INDEX `idx_vip_status` (`vipType`, `vipExpireTime`);
