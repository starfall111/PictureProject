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
CREATE TABLE IF NOT EXISTS user_follow (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    followerId  BIGINT NOT NULL COMMENT '关注者用户ID',
    followeeId  BIGINT NOT NULL COMMENT '被关注者用户ID',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follower_followee (followerId, followeeId),
    KEY idx_follower (followerId),
    KEY idx_followee (followeeId)
) COMMENT '用户关注关系' COLLATE = utf8mb4_unicode_ci;
