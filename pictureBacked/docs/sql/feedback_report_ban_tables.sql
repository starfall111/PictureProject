-- =============================================
-- 反馈-举报-封禁 建表脚本
-- =============================================

-- 1. 反馈主表
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT NOT NULL COMMENT '反馈ID（雪花算法）',
  `userId` BIGINT DEFAULT NULL COMMENT '提交者用户ID（匿名反馈为NULL）',
  `title` VARCHAR(100) NOT NULL COMMENT '反馈标题',
  `content` TEXT NOT NULL COMMENT '内容描述',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：BUG/FEATURE/ACCOUNT/EXPERIENCE/OTHER',
  `priority` VARCHAR(5) NOT NULL DEFAULT 'P3' COMMENT '优先级：P0/P1/P2/P3',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/REJECTED/REOPENED/CLOSED',
  `source` VARCHAR(20) NOT NULL DEFAULT 'APP' COMMENT '来源：APP/SYSTEM/ADMIN',
  `relatedPictureId` BIGINT DEFAULT NULL COMMENT '关联图片ID（可选）',
  `relatedUserId` BIGINT DEFAULT NULL COMMENT '关联用户ID（可选）',
  `handlerId` BIGINT DEFAULT NULL COMMENT '当前处理人管理员ID',
  `isAnonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名提交',
  `closeReason` VARCHAR(50) DEFAULT NULL COMMENT '关闭原因：USER_WITHDRAW/AUTO_CLOSE/ADMIN_CLOSE/USER_CONFIRM',
  `reopenCount` INT NOT NULL DEFAULT 0 COMMENT '重新打开次数，上限2',
  `convertedReportId` BIGINT DEFAULT NULL COMMENT '转为举报后的report.id',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_user` (`userId`, `isDelete`),
  KEY `idx_feedback_status` (`status`, `isDelete`),
  KEY `idx_feedback_handler` (`handlerId`, `status`, `isDelete`),
  KEY `idx_feedback_type_priority` (`type`, `priority`, `isDelete`),
  KEY `idx_feedback_create_time` (`createTime` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

-- 2. 反馈回复表
CREATE TABLE IF NOT EXISTS `feedback_reply` (
  `id` BIGINT NOT NULL COMMENT '回复ID（雪花算法）',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `userId` BIGINT NOT NULL COMMENT '回复者用户ID',
  `replyType` VARCHAR(20) NOT NULL COMMENT '回复类型：USER_REPLY/ADMIN_REPLY/INTERNAL_NOTE',
  `content` TEXT NOT NULL COMMENT '回复内容',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reply_feedback` (`feedbackId`, `createTime` ASC, `isDelete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈回复表';

-- 3. 反馈状态日志表
CREATE TABLE IF NOT EXISTS `feedback_status_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `fromStatus` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
  `toStatus` VARCHAR(20) NOT NULL COMMENT '变更后状态',
  `operatorId` BIGINT NOT NULL COMMENT '操作人ID',
  `operatorType` VARCHAR(10) NOT NULL COMMENT '操作人类型：USER/ADMIN/SYSTEM',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_log_feedback` (`feedbackId`, `createTime` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈状态日志表';

-- 4. 反馈附件表
CREATE TABLE IF NOT EXISTS `feedback_attachment` (
  `id` BIGINT NOT NULL COMMENT '附件ID',
  `feedbackId` BIGINT NOT NULL COMMENT '关联反馈ID',
  `fileUrl` VARCHAR(500) NOT NULL COMMENT 'OSS 文件 URL',
  `fileName` VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
  `fileSize` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `fileType` VARCHAR(20) DEFAULT NULL COMMENT '文件类型：PNG/JPG/JPEG/GIF',
  `sortOrder` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_attachment_feedback` (`feedbackId`, `sortOrder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈附件表';

-- 5. 举报表
CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL COMMENT '举报ID（雪花算法）',
  `reporterId` BIGINT NOT NULL COMMENT '举报人ID',
  `targetType` VARCHAR(20) NOT NULL COMMENT '举报对象类型：PICTURE/USER',
  `targetId` BIGINT NOT NULL COMMENT '举报目标ID',
  `reasonType` VARCHAR(20) NOT NULL COMMENT '举报原因：PORNOGRAPHY/VIOLENCE/SCAM/COPYRIGHT/SPAM/HARASSMENT/OTHER',
  `description` TEXT DEFAULT NULL COMMENT '补充描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/APPROVED/REJECTED/WARN_ONLY/REMOVE_ONLY',
  `handlerId` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handleResult` VARCHAR(50) DEFAULT NULL COMMENT '处理结果：WARN/BAN_TEMP_3/BAN_TEMP_7/BAN_TEMP_30/BAN_PERMANENT/REMOVE_PICTURE/REJECT',
  `handleReason` TEXT DEFAULT NULL COMMENT '处理理由',
  `handleTime` DATETIME DEFAULT NULL COMMENT '处理时间',
  `reportCount` INT NOT NULL DEFAULT 0 COMMENT '该目标累计被举报次数',
  `sourceFeedbackId` BIGINT DEFAULT NULL COMMENT '来源反馈ID（从反馈转来时填写）',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`targetType`, `targetId`),
  KEY `idx_status_count` (`status`, `reportCount` DESC, `createTime`),
  KEY `idx_reporter_dup` (`reporterId`, `targetType`, `targetId`),
  KEY `idx_target_count` (`targetType`, `targetId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 6. 封禁记录表
CREATE TABLE IF NOT EXISTS `ban_record` (
  `id` BIGINT NOT NULL COMMENT '封禁记录ID（雪花算法）',
  `userId` BIGINT NOT NULL COMMENT '被封禁用户ID',
  `banType` VARCHAR(20) NOT NULL COMMENT '封禁类型：TEMP/PERMANENT',
  `banDuration` INT DEFAULT NULL COMMENT '封禁天数（3/7/30）',
  `banStartTime` DATETIME NOT NULL COMMENT '封禁开始时间',
  `banEndTime` DATETIME DEFAULT NULL COMMENT '封禁结束时间（永久封禁为NULL）',
  `banReason` VARCHAR(200) NOT NULL COMMENT '封禁原因',
  `violationCount` INT NOT NULL DEFAULT 0 COMMENT '封禁时累计违规次数',
  `unbanned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封',
  `unbanTime` DATETIME DEFAULT NULL COMMENT '解封时间',
  `unbanReason` TEXT DEFAULT NULL COMMENT '解封理由',
  `unbanOperatorId` BIGINT DEFAULT NULL COMMENT '解封操作人ID',
  `banOperatorId` BIGINT NOT NULL COMMENT '封禁操作人ID',
  `banOperatorName` VARCHAR(100) NOT NULL COMMENT '封禁操作人姓名',
  `reportId` BIGINT DEFAULT NULL COMMENT '关联举报ID',
  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`userId`, `banStartTime` DESC),
  KEY `idx_unbanned_end` (`unbanned`, `banEndTime`),
  KEY `idx_ban_end` (`banEndTime`, `unbanned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封禁记录表';

-- 7. User 表扩展
ALTER TABLE `user`
  ADD COLUMN `banStatus` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '封禁状态：NONE/TEMP/PERMANENT' AFTER `userRole`,
  ADD COLUMN `banEndTime` DATETIME DEFAULT NULL COMMENT '封禁结束时间' AFTER `banStatus`,
  ADD COLUMN `violationCount` INT NOT NULL DEFAULT 0 COMMENT '累计违规次数（6个月滚动窗口）' AFTER `banEndTime`,
  ADD COLUMN `lastViolationTime` DATETIME DEFAULT NULL COMMENT '最近一次违规时间' AFTER `violationCount`,
  ADD INDEX `idx_ban_status` (`banStatus`, `banEndTime`);
