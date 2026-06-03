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
}
