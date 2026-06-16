-- ============================================================
-- 导出用户信息和图片信息为 CSV 文件
-- 数据库：picture (MySQL)
-- 使用方式：
--   方式一：在 MySQL 客户端中执行本 SQL 文件
--          mysql -u root -p picture < export_to_csv.sql
--   方式二：使用命令行直接导出（见下方说明）
-- ============================================================

-- 注意：INTO OUTFILE 的文件路径是 MySQL 服务端的路径，需要 FILE 权限。
-- Windows 下默认输出到 MySQL 数据目录（如 C:/ProgramData/MySQL/...），
-- 也可以指定自定义路径。请根据实际环境修改输出路径。

-- ============================================================
-- 1. 导出用户信息 (排除密码和删除标记)
-- ============================================================
SELECT
    id,
    userAccount,
    userPhone,
    userEmail,
    DATE_FORMAT(createTime, '%Y-%m-%d %H:%i:%s') AS createTime,
    DATE_FORMAT(updateTime, '%Y-%m-%d %H:%i:%s') AS updateTime
FROM user
WHERE isDelete = 0
INTO OUTFILE 'E:/export/users.csv'
    FIELDS TERMINATED BY ','
    OPTIONALLY ENCLOSED BY '"'
    ESCAPED BY '\\'
    LINES TERMINATED BY '\r\n';



# 导出图片信息为 JSON
mysql -u root -p125801 --default-character-set=utf8mb4 -e "SELECT
  JSON_ARRAYAGG(JSON_OBJECT('id',id)) FROM picture WHERE isDelete=0" picture > E:/export/pictures.json

# 导出用户ID 为csv文件
mysql -u root -p125801 --default-character-set=utf8mb4 -B -e "SELECT id FROM user WHERE isDelete=0" picture > E:/export/userId.csv
-- ============================================================
-- 2. 导出图片信息
-- ============================================================
SELECT
    id,
    categoryId,
    spaceId,
    tags,
    userId,
    DATE_FORMAT(reviewTime, '%Y-%m-%d %H:%i:%s') AS reviewTime,
    DATE_FORMAT(createTime, '%Y-%m-%d %H:%i:%s') AS createTime,
    DATE_FORMAT(editTime, '%Y-%m-%d %H:%i:%s') AS editTime,
    DATE_FORMAT(updateTime, '%Y-%m-%d %H:%i:%s') AS updateTime
FROM picture
WHERE isDelete = 0
INTO OUTFILE 'E:/export/pictures.csv'
    FIELDS TERMINATED BY ','
    OPTIONALLY ENCLOSED BY '"'
    ESCAPED BY '\\'
    LINES TERMINATED BY '\r\n';

# 数据导入
mysql -u root -p125801 --default-character-set=utf8mb4 picture < scripts\insert_users.sql