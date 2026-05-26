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
    ADD COLUMN originUrl varchar(512) NULL COMMENT '缩略图 url';
-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);


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
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    picture_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_picture_user (picture_id, user_id),
    KEY idx_picture_id (picture_id),
    KEY idx_user_id (user_id)
) COMMENT '图片点赞' COLLATE = utf8mb4_unicode_ci;

-- 收藏表
CREATE TABLE IF NOT EXISTS picture_favorite
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    picture_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_picture_user (picture_id, user_id),
    KEY idx_picture_id (picture_id),
    KEY idx_user_id (user_id)
) COMMENT '图片收藏' COLLATE = utf8mb4_unicode_ci;

-- 分享记录表
CREATE TABLE IF NOT EXISTS picture_share
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    picture_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    platform    VARCHAR(20) NOT NULL COMMENT 'wechat/weibo/qq',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_picture_id (picture_id)
) COMMENT '图片分享记录' COLLATE = utf8mb4_unicode_ci;

-- 图片统计表
CREATE TABLE IF NOT EXISTS picture_statistics
(
    picture_id     BIGINT PRIMARY KEY,
    like_count     INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    share_count    INT DEFAULT 0,
    KEY idx_like_count (like_count),
    KEY idx_favorite_count (favorite_count)
) COMMENT '图片统计' COLLATE = utf8mb4_unicode_ci;