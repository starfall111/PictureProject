-- 批量获取图片任务表
CREATE TABLE IF NOT EXISTS `batch_task` (
  `id`           BIGINT NOT NULL COMMENT '主键（雪花算法）',
  `userId`       BIGINT NOT NULL COMMENT '发起用户ID',
  `spaceId`      BIGINT DEFAULT NULL COMMENT '目标空间ID',
  `searchText`   VARCHAR(200) NOT NULL COMMENT '搜索关键词',
  `searchSource` VARCHAR(20) NOT NULL DEFAULT 'pexels' COMMENT '搜索来源: pexels/bing',
  `totalCount`   INT NOT NULL DEFAULT 0 COMMENT '计划处理总数',
  `successCount` INT NOT NULL DEFAULT 0 COMMENT '成功数量',
  `failCount`    INT NOT NULL DEFAULT 0 COMMENT '失败数量',
  `status`       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
  `categoryId`   BIGINT DEFAULT NULL COMMENT '分类ID',
  `namePrefix`   VARCHAR(100) DEFAULT NULL COMMENT '图片名称前缀',
  `tags`         TEXT DEFAULT NULL COMMENT '标签JSON数组',
  `errorMessage` TEXT DEFAULT NULL COMMENT '整体错误信息',
  `createTime`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `finishTime`   DATETIME DEFAULT NULL COMMENT '任务完成时间',
  `isDelete`     TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_user` (`userId`, `isDelete`),
  KEY `idx_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量获取图片任务表';
