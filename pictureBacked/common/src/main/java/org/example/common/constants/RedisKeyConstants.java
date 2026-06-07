package org.example.common.constants;

/**
 * Redis Key 常量定义
 *
 * @author Zou
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 计算 TTL 基础时间 + 随机抖动，防止缓存雪崩
     */
    public static int ttlWithJitter(int base, int jitter) {
        return base + cn.hutool.core.util.RandomUtil.randomInt(0, jitter);
    }


    // todo 列举缓存时机
    //  多级缓存使用场景 人们搜索、推荐图片、图片社交数据

    // ==================== 社交功能 Key ====================

    /**
     * 点赞状态标记
     * Value: STRING "1"
     */
    // todo 操作时数据库和缓存进行双写操作 ，缓存过期时间 7 天 定时任务同步到数据库
    public static final String SOCIAL_LIKE_KEY = "social:like:%d:%d";

    /**
     * 收藏状态标记
     * Value: STRING "1"
     */
    // todo 同点赞状态标记
    public static final String SOCIAL_FAV_KEY = "social:fav:%d:%d";

    /**
     * 统计计数器 Hash
     * Fields: likeCount, favoriteCount, shareCount, viewCount, downloadCount
     */
    // todo 用户进行操作时，直接更新，维护脏集合来绑定定时任务将数据回写至数据库
    public static final String SOCIAL_STATS_KEY = "social:stats:%d";

    /**
     * 待同步 DB 的 pictureId 集合（脏集合）
     * Type: SET
     */
    // todo 用户进行操作时，直接更新，维护脏集合来绑定定时任务将数据回写至数据库
    public static final String SOCIAL_STATS_DIRTY_KEY = "social:stats:dirty";

    /**
     * 社交操作分布式锁
     * Value: STRING, TTL 10s
     */
    public static final String SOCIAL_LOCK_KEY = "lock:social:%s:%d:%d";

    // ==================== 图片查询缓存 Key ====================

    /**
     * 热门图片列表查询缓存
     * Value: STRING (JSON), TTL 5-15min
     */
    // todo 管理员手动刷新缓存，定时任务重算热度分数再更新缓存
    public static final String PIC_QUERY_HOT_KEY = "pic:query:hot:%s";

    /**
     * 普通图片列表查询缓存
     * Value: STRING (JSON), TTL 5min
     */
    // todo 允许不及时展示数据，即使用户刚刚删除上传图片，但是再用户主页需要及时更新
    public static final String PIC_QUERY_NORMAL_KEY = "pic:query:normal:%s";

    /**
     * 图片详情缓存（PictureVO，不含 PictureSocialVO）
     * Value: STRING (JSON), TTL 15-30min
     */
    public static final String PIC_DETAIL_KEY = "pic:detail:%d";

    // ==================== 用户图片列表缓存 Key ====================

    /** 用户点赞列表缓存 */
    //  todo 用户点赞时失效
    public static final String LIST_LIKED_KEY = "list:liked:%d:%s";
    /** 用户收藏列表缓存 */
    //  todo 用户收藏时失效
    public static final String LIST_FAV_KEY = "list:fav:%d:%s";
    /** 用户上传列表缓存 */
    //  todo 用户上传时失效，管理员审批通过后失效
    public static final String LIST_UPLOADED_KEY = "list:uploaded:%d:%s";

    // ==================== 社交功能 TTL ====================

    /** 点赞/收藏状态缓存 TTL 基础时间（秒）= 7 天 */
    public static final int SOCIAL_STATUS_TTL_BASE = 7 * 24 * 3600;
    /** 点赞/收藏状态缓存 TTL 随机抖动上限（秒）= 60 分钟 */
    public static final int SOCIAL_STATUS_TTL_JITTER = 3600;
    /** 统计 Hash 缓存 TTL 基础时间（秒）= 7 天 */
    public static final int SOCIAL_STATS_TTL_BASE = 7 * 24 * 3600;
    /** 统计 Hash 缓存 TTL 随机抖动上限（秒）= 60 分钟 */
    public static final int SOCIAL_STATS_TTL_JITTER = 3600;

    // ==================== 统计同步锁 ====================

    /**
     * 统计同步任务分布式锁
     */
    public static final String STATS_SYNC_LOCK_KEY = "lock:stats:sync";

    /**
     * 用户档案信息缓存
     */
    public static final String USER_PROFILE_KEY = "user:profile:%d";
    /**
     * 用户个人信息缓存
     */
    public static final String USER_INFO_KEY = "user:info:%d";

    // ==================== 通知功能 Key ====================

    /**
     * 通知未读计数缓存
     * Value: STRING count
     */
    // todo 点赞、收藏、审批后、管理员发送系统通知更新 or 失效
    public static final String NOTIFICATION_UNREAD_KEY = "notify:unread:%d";

    /**
     * 通知未读计数 TTL（秒）= 30 分钟
     */
    public static final int NOTIFICATION_UNREAD_TTL = 30 * 60;

    // ==================== 限流 Key（滑动窗口） ====================

    /**
     * 通用限流 key 前缀
     * 格式: rate_limit:{resource}:{key}
     * 使用 RateLimitUtil 进行原子操作
     */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate_limit";

    /** 登录限流资源名 */
    public static final String RATE_LIMIT_RESOURCE_LOGIN = "login";

    /** 登录限流窗口（秒）= 5 分钟 */
    public static final int LOGIN_RATE_LIMIT_WINDOW = 5 * 60;

    /** 登录限流最大次数 */
    public static final int LOGIN_RATE_LIMIT_MAX = 5;

    /** 登录锁定窗口（秒）= 30 分钟（连续触发限流后，窗口变大） */
    public static final int LOGIN_LOCK_WINDOW = 30 * 60;

    // ==================== 系统消息 Key ====================

    /**
     * 系统消息发布分布式锁
     */
    public static final String SYSTEM_MSG_LOCK_KEY = "lock:sysmsg:publish:%d";

    // ==================== 推荐功能 Key ====================

    /**
     * 全局热度 ZSET
     * Value: ZSET member=pictureId, score=HotScore
     */
    public static final String REC_HOT_ZSET_KEY = "rec:hot:zset";

    /**
     * 分类热度 ZSET
     * Value: ZSET member=pictureId, score=HotScore
     */
    public static final String REC_HOT_CAT_ZSET_KEY = "rec:hot:cat:%d";

    /**
     * 用户偏好画像缓存
     * Value: STRING (JSON)
     */
    public static final String REC_PREF_KEY = "rec:pref:%d";

    /**
     * 推荐结果缓存
     * Value: STRING (JSON)
     * 格式: rec:result:{scene}:{categoryId}:{current}:{pageSize}
     * categoryId 为 0 表示全部分类
     */
    public static final String REC_RESULT_KEY = "rec:result:%s:%d:%d:%d";

    /**
     * 热度评分计算分布式锁
     */
    public static final String LOCK_REC_SCORE_KEY = "lock:rec:score";

    /**
     * 用户偏好计算分布式锁
     */
    public static final String LOCK_REC_PREF_KEY = "lock:rec:pref:%d";

    // ==================== 推荐功能 TTL ====================

    /** 热度 ZSET TTL（秒）= 2 天 */
    public static final int REC_HOT_ZSET_TTL = 2 * 24 * 3600;

    /** 推荐结果缓存 TTL 基础时间（秒）= 10 分钟 */
    public static final int REC_RESULT_TTL_BASE = 10 * 60;

    /** 推荐结果缓存 TTL 随机抖动上限（秒）= 5 分钟 */
    public static final int REC_RESULT_TTL_JITTER = 5 * 60;

    /** 用户偏好缓存 TTL（秒）= 24 小时 */
    public static final int REC_PREF_TTL = 24 * 3600;

    /**
     * 热度评分待重算 pictureId 集合（脏集合）
     * Type: SET
     */
    public static final String REC_HOT_DIRTY_KEY = "rec:hot:dirty";

    // ==================== 关注功能 Key ====================

    /**
     * 关注状态标记（是否关注某用户）
     * Value: STRING "1"(关注) 或 "0"(未关注)
     * 格式: follow:status:{followerId}:{followeeId}
     */
    public static final String FOLLOW_STATUS_KEY = "follow:status:%d:%d";

    /**
     * 关注数缓存
     * Value: STRING count
     * 格式: follow:count:{userId}:following
     */
    public static final String FOLLOW_COUNT_KEY = "follow:count:%d:following";

    /**
     * 粉丝数缓存
     * Value: STRING count
     * 格式: follow:count:{userId}:followers
     */
    public static final String FOLLOWER_COUNT_KEY = "follow:count:%d:followers";

    /**
     * 关注操作分布式锁
     * Value: STRING, TTL 10s
     */
    public static final String FOLLOW_LOCK_KEY = "lock:follow:%d:%d";

    /**
     * 关注计数同步分布式锁
     */
    public static final String FOLLOW_SYNC_LOCK_KEY = "lock:follow:sync";

    // ==================== 关注功能 TTL ====================

    /** 关注状态缓存 TTL 基础时间（秒）= 7 天 */
    public static final int FOLLOW_STATUS_TTL_BASE = 7 * 24 * 3600;
    /** 关注状态缓存 TTL 随机抖动上限（秒）= 60 分钟 */
    public static final int FOLLOW_STATUS_TTL_JITTER = 3600;
    /** 关注计数缓存 TTL 基础时间（秒）= 7 天 */
    public static final int FOLLOW_COUNT_TTL_BASE = 7 * 24 * 3600;
    /** 关注计数缓存 TTL 随机抖动上限（秒）= 60 分钟 */
    public static final int FOLLOW_COUNT_TTL_JITTER = 3600;

    // ==================== 动态功能 Key ====================

    /**
     * 动态数据缓存（含 items + unreadCount + latestTime + total）
     * Value: STRING (JSON)
     * 格式: feed:cache:{userId}
     */
    public static final String FEED_CACHE_KEY = "feed:cache:%d";

    /**
     * 动态阅读水位线（用户最后阅读时间戳）
     * Value: STRING (毫秒时间戳)
     * 格式: feed:watermark:{userId}
     */
    public static final String FEED_WATERMARK_KEY = "feed:watermark:%d";

    /**
     * 动态缓存分布式锁
     * Value: STRING, TTL 10s
     */
    public static final String FEED_LOCK_KEY = "lock:feed:%d";

    // ==================== 动态功能 TTL ====================

    /** 动态缓存 TTL 基础时间（秒）= 3 分钟 */
    public static final int FEED_CACHE_TTL_BASE = 3 * 60;

    /** 动态缓存 TTL 随机抖动上限（秒）= 3 分钟 */
    public static final int FEED_CACHE_TTL_JITTER = 3 * 60;

    /** 动态水位线 TTL（秒）= 30 天 */
    public static final int FEED_WATERMARK_TTL = 30 * 24 * 3600;

    // ==================== Feedback 反馈模块 Key ====================

    /**
     * 反馈提交限流计数
     * Value: STRING(INT), TTL 到当天23:59:59
     * 格式: feedback:submit:count:{userId}
     */
    public static final String FEEDBACK_SUBMIT_COUNT = "feedback:submit:count:";

    /**
     * 反馈每日统计缓存
     * Value: STRING(JSON), TTL 7天
     */
    public static final String FEEDBACK_STATS_DAILY = "feedback:stats:daily:";

    /**
     * 管理端待处理反馈计数
     * Value: STRING(INT), TTL 5分钟
     */
    public static final String FEEDBACK_ADMIN_PENDING_COUNT = "feedback:admin:pending:count";

    /**
     * 反馈详情缓存
     * Value: HASH, TTL 30分钟
     */
    public static final String FEEDBACK_DETAIL = "feedback:detail:";

    /**
     * 反馈提交防重锁
     * Value: STRING, TTL 10秒
     */
    public static final String LOCK_FEEDBACK_SUBMIT = "lock:feedback:submit:";

    /**
     * 反馈自动关闭检查队列
     * Value: ZSET (member=feedbackId, score=autoCloseTimestamp)
     */
    public static final String FEEDBACK_AUTO_CLOSE_CHECK = "feedback:auto:close:check";

    /**
     * 用户端反馈统计缓存
     * Value: STRING(JSON), TTL 5分钟
     * 格式: feedback:stats:user:{userId}
     */
    public static final String FEEDBACK_USER_STATS_KEY = "feedback:stats:user:%d";

    /**
     * 管理端反馈统计缓存
     * Value: STRING(JSON), TTL 5分钟
     */
    public static final String FEEDBACK_ADMIN_STATS_KEY = "feedback:stats:admin";

    /** 反馈统计缓存 TTL（秒）= 5 分钟 */
    public static final int FEEDBACK_STATS_TTL = 5 * 60;

    // ==================== Report 举报模块 Key ====================

    /**
     * 举报限流
     * Value: STRING(INT), TTL 60秒
     * 格式: rate_limit:report:{userId}
     */
    public static final String REPORT_RATE_LIMIT_KEY = "rate_limit:report:%d";

    /**
     * 举报防重复
     * Value: STRING, TTL 7天
     * 格式: report:dup:{reporterId}:{targetType}:{targetId}
     */
    public static final String REPORT_DUPLICATE_KEY = "report:dup:%d:%s:%d";

    /**
     * 举报人信誉分
     * Value: STRING(JSON), TTL 24小时
     * 格式: report:credit:{userId}
     */
    public static final String REPORT_CREDIT_KEY = "report:credit:%d";

    /**
     * 举报统计缓存
     * Value: STRING(JSON), TTL 5分钟
     */
    public static final String REPORT_STATS_KEY = "report:stats";

    /**
     * 今日新增举报脏集合
     * Type: SET, member=reportId
     */
    public static final String REPORT_TODAY_DIRTY_KEY = "report:stats:today:dirty";

    /**
     * 今日新增举报计数
     * Value: STRING(INT)
     */
    public static final String REPORT_TODAY_COUNT_KEY = "report:stats:today:count";

    /** 举报统计缓存 TTL（秒）= 5 分钟 */
    public static final int REPORT_STATS_TTL = 5 * 60;

    // ==================== Ban 封禁模块 Key ====================

    /**
     * 单用户封禁状态缓存
     * Value: STRING(JSON), TTL 30分钟
     * 格式: ban:status:{userId}
     */
    public static final String BAN_STATUS_KEY = "ban:status:%d";

    /**
     * 封禁黑名单 ZSET
     * Value: ZSET (member=userId, score=banEndTime timestamp)
     */
    public static final String BAN_BLACKLIST_ZSET = "ban:blacklist";

    // ==================== Feedback TTL ====================

    /** 反馈提交限流计数器 TTL（秒）= 24 小时 */
    public static final int FEEDBACK_SUBMIT_COUNT_TTL = 24 * 3600;

    /** 反馈详情缓存 TTL（秒）= 30 分钟 */
    public static final int FEEDBACK_DETAIL_TTL = 30 * 60;

    /** 反馈防重锁 TTL（秒）= 10 秒 */
    public static final int FEEDBACK_LOCK_TTL = 10;

    /** 管理端待处理计数 TTL（秒）= 5 分钟 */
    public static final int FEEDBACK_ADMIN_PENDING_TTL = 5 * 60;

    // ==================== Report TTL ====================

    /** 举报限流窗口（秒）= 60 秒 */
    public static final int REPORT_RATE_LIMIT_WINDOW = 60;

    /** 举报限流最大次数 */
    public static final int REPORT_RATE_LIMIT_MAX = 3;

    /** 举报防重复 TTL（秒）= 7 天 */
    public static final int REPORT_DUPLICATE_TTL = 7 * 24 * 3600;

    /** 举报人信誉分 TTL（秒）= 24 小时 */
    public static final int REPORT_CREDIT_TTL = 24 * 3600;

    // ==================== Ban TTL ====================

    /** 封禁状态缓存 TTL（秒）= 30 分钟 */
    public static final int BAN_STATUS_TTL = 30 * 60;

    /** 封禁自动解封扫描间隔（秒）= 5 分钟 */
    public static final int BAN_SCHEDULE_INTERVAL = 5 * 60;

    /** 封禁统计缓存 TTL（秒）= 5 分钟 */
    public static final int BAN_STATS_TTL = 5 * 60;

    /**
     * 封禁统计缓存
     * Value: STRING(JSON), TTL 5分钟
     */
    public static final String BAN_STATS_KEY = "ban:stats";

    // ==================== 批量任务 Key ====================

    /**
     * 批量任务提交限流
     * Value: STRING(INT), TTL 60秒
     * 格式: rate_limit:batch:{userId}
     */
    public static final String BATCH_TASK_RATE_LIMIT_KEY = "rate_limit:batch:%d";
    /** 批量任务限流窗口（秒）= 60 秒 */
    public static final int BATCH_TASK_RATE_LIMIT_WINDOW = 60;
    /** 批量任务限流最大次数 */
    public static final int BATCH_TASK_RATE_LIMIT_MAX = 1;

    /**
     * 批量任务进度缓存
     * Value: STRING(JSON), TTL 1小时
     * 格式: batch:progress:{taskId}
     */
    public static final String BATCH_TASK_PROGRESS_KEY = "batch:progress:%d";
    /** 批量任务进度缓存 TTL（秒）= 1 小时 */
    public static final int BATCH_TASK_PROGRESS_TTL = 60 * 60;

    /**
     * 批量任务管理端统计缓存
     * Value: STRING(JSON), TTL 5分钟
     */
    public static final String BATCH_TASK_STATS_KEY = "batch:stats:admin";

    /** 批量任务统计缓存 TTL（秒）= 5 分钟 */
    public static final int BATCH_TASK_STATS_TTL = 5 * 60;

    // ==================== 秒杀模块 Key ====================

    /**
     * 秒杀批次库存 Hash
     * Fields: stock, version
     * 格式: seckill:batch:stock:{batchId}
     */
    public static final String SECKILL_BATCH_STOCK = "seckill:batch:stock:%d";

    /**
     * 秒杀批次信息缓存
     * Value: STRING (JSON)
     * 格式: seckill:batch:info:{batchId}
     */
    public static final String SECKILL_BATCH_INFO = "seckill:batch:info:%d";

    /**
     * 秒杀令牌（防刷）
     * Value: STRING, TTL 300s
     * 格式: seckill:token:{token}
     */
    public static final String SECKILL_TOKEN = "seckill:token:%s";
    /** 秒杀令牌 TTL（秒）= 5 分钟 */
    public static final int SECKILL_TOKEN_TTL = 300;

    /**
     * 秒杀去重标记（防重复下单）
     * Value: STRING, TTL 86400s
     * 格式: seckill:dedupe:{userId}:{batchId}
     */
    public static final String SECKILL_DEDUPE = "seckill:dedupe:%d:%d";
    /** 秒杀去重 TTL（秒）= 24 小时 */
    public static final int SECKILL_DEDUPE_TTL = 86400;

    /**
     * 秒杀批次操作分布式锁
     * Value: STRING, TTL 10s
     * 格式: lock:seckill:batch:{batchId}
     */
    public static final String LOCK_SECKILL_BATCH = "lock:seckill:batch:%d";

    /**
     * 秒杀降级等级
     * Value: STRING(INT)
     */
    public static final String SECKILL_DEGRADE = "seckill:degrade:level";

    // ==================== VIP 会员模块 Key ====================

    /**
     * VIP 到期提醒标记
     * Value: STRING, TTL 到到期日
     * 格式: vip:expire:remind:{userId}
     */
    public static final String VIP_EXPIRE_REMIND_KEY = "vip:expire:remind:%d";

    /**
     * 编码券激活分布式锁
     * Value: STRING, TTL 10s
     * 格式: lock:coupon:activate:{userId}:{couponId}
     */
    public static final String COUPON_ACTIVATE_LOCK_KEY = "lock:coupon:activate:%d:%d";

    /**
     * 编码券激活限流计数
     * Value: STRING(INT), TTL 60s
     * 格式: rate_limit:coupon:activate:{userId}
     */
    public static final String COUPON_ACTIVATE_RATE_LIMIT_KEY = "rate_limit:coupon:activate:%d";
    /** 编码券激活限流窗口（秒）= 60 秒 */
    public static final int COUPON_ACTIVATE_RATE_LIMIT_WINDOW = 60;
    /** 编码券激活限流最大次数 */
    public static final int COUPON_ACTIVATE_RATE_LIMIT_MAX = 5;

    /**
     * VIP 到期处理分布式锁
     */
    public static final String LOCK_VIP_EXPIRE = "lock:vip:expire";

    /**
     * 秒杀对账分布式锁
     */
    public static final String LOCK_SECKILL_RECONCILE = "lock:seckill:reconcile";

    /**
     * Pexels API 每小时调用计数
     * Value: STRING(INT), TTL 1小时
     * 格式: pexels:quota:hourly:{dateHour}
     */
    public static final String PEXELS_HOURLY_COUNT = "pexels:quota:hourly:%s";

    /**
     * Pexels API 每月调用计数
     * Value: STRING(INT), TTL 到月底
     * 格式: pexels:quota:monthly:{yearMonth}
     */
    public static final String PEXELS_MONTHLY_COUNT = "pexels:quota:monthly:%s";
}
