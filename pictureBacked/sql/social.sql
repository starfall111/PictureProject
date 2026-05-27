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
    view_count     INT DEFAULT 0,
    download_count INT DEFAULT 0,
    KEY idx_like_count (like_count),
    KEY idx_favorite_count (favorite_count)
) COMMENT '图片统计' COLLATE = utf8mb4_unicode_ci;

-- 迁移：添加浏览和下载计数列
ALTER TABLE picture_statistics
    ADD COLUMN viewCount     INT DEFAULT 0 AFTER shareCount,
    ADD COLUMN downloadCount INT DEFAULT 0 AFTER viewCount;
