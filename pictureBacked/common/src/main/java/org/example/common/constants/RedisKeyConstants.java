package org.example.common.constants;

/**
 * Redis Key 常量定义
 *
 * @author Zou
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // ==================== 社交功能 Key ====================

    /**
     * 点赞状态标记
     * Value: STRING "1"
     */
    public static final String SOCIAL_LIKE_KEY = "social:like:%d:%d";

    /**
     * 收藏状态标记
     * Value: STRING "1"
     */
    public static final String SOCIAL_FAV_KEY = "social:fav:%d:%d";

    /**
     * 统计计数器 Hash
     * Fields: likeCount, favoriteCount, shareCount, viewCount, downloadCount
     */
    public static final String SOCIAL_STATS_KEY = "social:stats:%d";

    /**
     * 待同步 DB 的 pictureId 集合（脏集合）
     * Type: SET
     */
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
    public static final String PIC_QUERY_HOT_KEY = "pic:query:hot:%s";

    /**
     * 普通图片列表查询缓存
     * Value: STRING (JSON), TTL 5min
     */
    public static final String PIC_QUERY_NORMAL_KEY = "pic:query:normal:%s";

    /**
     * 图片详情缓存（PictureVO，不含 PictureSocialVO）
     * Value: STRING (JSON), TTL 15-30min
     */
    public static final String PIC_DETAIL_KEY = "pic:detail:%d";

    // ==================== 用户图片列表缓存 Key ====================

    /** 用户点赞列表缓存 */
    public static final String LIST_LIKED_KEY = "list:liked:%d:%s";
    /** 用户收藏列表缓存 */
    public static final String LIST_FAV_KEY = "list:fav:%d:%s";
    /** 用户上传列表缓存 */
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
    public static final String NOTIFICATION_UNREAD_KEY = "notify:unread:%d";

    /**
     * 通知未读计数 TTL（秒）= 30 分钟
     */
    public static final int NOTIFICATION_UNREAD_TTL = 30 * 60;

    // ==================== 系统消息 Key ====================

    /**
     * 系统消息发布分布式锁
     */
    public static final String SYSTEM_MSG_LOCK_KEY = "lock:sysmsg:publish:%d";
}
