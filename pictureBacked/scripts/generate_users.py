"""
生成 10000 条用户测试数据的 SQL INSERT 脚本。

输出文件: insert_users.sql
批量 INSERT: 每 500 条一个 INSERT 语句

字段说明:
  - id: 雪花算法风格的 19 位数字 ID
  - banStatus: 使用项目实际的枚举值 NONE / BAN_TEMP_3 / BAN_TEMP_7 / BAN_TEMP_30 / BAN_PERMANENT
  - vipType: 0=普通用户, 1=月度VIP, 2=年度VIP
"""

import random
import time
import os

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------
TOTAL_USERS = 10000
BATCH_SIZE = 500
OUTPUT_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "insert_users.sql")

# 固定 BCrypt 密码 (对应明文 "12345678")
BCRYPT_PASSWORD = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

# 已生成的账号集合，保证唯一
_used_accounts: set[str] = set()

# 雪花算法 ID 参数（模拟）
EPOCH = 1704067200000  # 2024-01-01 00:00:00 UTC 毫秒
MACHINE_ID = 1
SEQUENCE = 0

# ---------------------------------------------------------------------------
# 辅助函数
# ---------------------------------------------------------------------------

def generate_snowflake_id(index: int) -> int:
    """
    生成一个 19 位雪花算法风格的 ID。
    使用递增的 timestamp 部分保证唯一性。
    """
    timestamp = EPOCH + index * 5
    # Snowflake 结构: 1(符号) + 41(时间戳) + 5(数据中心) + 5(机器) + 12(序列号)
    snowflake_id = ((timestamp - 1288834974657) << 22) | (MACHINE_ID << 12) | (index & 0xFFF)
    return snowflake_id


def random_phone() -> str:
    """生成中国大陆格式手机号 (11位)。"""
    prefixes = [
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "153", "155", "156", "157", "158", "159",
        "170", "171", "172", "173", "175", "176", "177", "178",
        "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
        "191", "198", "199",
    ]
    return random.choice(prefixes) + "".join([str(random.randint(0, 9)) for _ in range(8)])


def random_email() -> str:
    """生成随机邮箱。"""
    domains = [
        "qq.com", "163.com", "126.com", "gmail.com", "outlook.com",
        "sina.com", "foxmail.com", "hotmail.com", "yahoo.com", "yeah.net",
    ]
    # 用户名部分 6-12 位小写字母+数字
    length = random.randint(6, 12)
    chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    username = "".join(random.choice(chars) for _ in range(length))
    return f"{username}@{random.choice(domains)}"


def random_nickname() -> str:
    """生成随机中文昵称。"""
    adjectives = [
        "快乐", "阳光", "梦想", "自由", "勇敢", "温柔", "可爱", "帅气",
        "聪明", "淡定", "幽默", "文艺", "清新", "帅气", "酷炫", "优雅",
        "甜蜜", "浪漫", "洒脱", "豪迈", "飘逸", "灵动", "活泼", "安静",
    ]
    nouns = [
        "小熊", "猫咪", "兔子", "小鸟", "小鱼", "老虎", "狮子", "企鹅",
        "星辰", "月亮", "太阳", "云朵", "彩虹", "花火", "清风", "海浪",
        "少年", "旅人", "画师", "歌者", "舞者", "追梦", "飞鸟", "浪子",
        "侠客", "骑士", "精灵", "天使", "勇士", "冒险", "旅者", "幻想",
    ]
    return random.choice(adjectives) + random.choice(nouns) + str(random.randint(1, 999))


def random_account(index: int) -> str:
    """生成唯一用户账号。"""
    while True:
        styles = [
            lambda: f"user_{index:06d}",
            lambda: f"pic_{random.randint(100000, 999999)}",
            lambda: f"art_{random.randint(10000, 99999)}_{random.choice(['cn', 'com', 'io', 'cc'])}",
        ]
        account = random.choice(styles)()
        if account not in _used_accounts:
            _used_accounts.add(account)
            return account


def random_datetime_within_year() -> str:
    """生成过去一年内的随机 DATETIME 字符串。"""
    now = int(time.time())
    one_year_ago = now - 365 * 24 * 3600
    ts = random.randint(one_year_ago, now)
    time_struct = time.localtime(ts)
    return time.strftime("%Y-%m-%d %H:%M:%S", time_struct)


def random_future_datetime(max_days: int = 180) -> str:
    """生成未来若干天内的随机 DATETIME 字符串。"""
    now = int(time.time())
    ts = now + random.randint(1, max_days * 24 * 3600)
    time_struct = time.localtime(ts)
    return time.strftime("%Y-%m-%d %H:%M:%S", time_struct)


def random_past_datetime(max_days_ago: int = 30) -> str:
    """生成过去若干天内的随机 DATETIME 字符串。"""
    now = int(time.time())
    ts = now - random.randint(1, max_days_ago * 24 * 3600)
    time_struct = time.localtime(ts)
    return time.strftime("%Y-%m-%d %H:%M:%S", time_struct)


def random_profile() -> str:
    """生成随机个人简介（可为空）。"""
    profiles = [
        "", "", "",  # 大概率为空
        "这个人很懒，什么都没留下。",
        "热爱摄影，记录生活中的美好瞬间。",
        "用镜头发现世界的另一面。",
        "风景摄影师，行走在路上。",
        "美食与旅行，缺一不可。",
        "喜欢绘画和设计。",
        "生活不止眼前的苟且，还有诗和远方。",
        "每天进步一点点。",
        "记录美好，分享快乐。",
    ]
    return random.choice(profiles)


def escape_sql_string(s: str) -> str:
    """转义 SQL 字符串中的特殊字符。"""
    return s.replace("\\", "\\\\").replace("'", "\\'")


def generate_user(index: int) -> dict:
    """生成一条用户数据的所有字段值。"""

    # 确定 userRole
    if index < 5:
        user_role = "admin"
    else:
        user_role = "user"

    # 确定 vipType
    vip_roll = random.random()
    if vip_roll < 0.70:
        vip_type = 0
    elif vip_roll < 0.88:
        vip_type = 1  # 月度 VIP
    else:
        vip_type = 2  # 年度 VIP

    # VIP 时间字段
    if vip_type > 0:
        vip_activated_at = random_past_datetime(90)
        vip_expire_time = random_future_datetime(180 if vip_type == 2 else 30)
        vip_total_days = random.randint(1, 365)
    else:
        vip_activated_at = "NULL"
        vip_expire_time = "NULL"
        vip_total_days = 0

    # 确定 banStatus
    ban_roll = random.random()
    if ban_roll < 0.96:
        ban_status = "NONE"
        ban_end_time = "NULL"
        violation_count = 0
        last_violation_time = "NULL"
    elif ban_roll < 0.975:
        ban_status = "BAN_TEMP_3"
        ban_end_time = random_future_datetime(3)
        violation_count = random.randint(1, 2)
        last_violation_time = random_past_datetime(7)
    elif ban_roll < 0.985:
        ban_status = "BAN_TEMP_7"
        ban_end_time = random_future_datetime(7)
        violation_count = random.randint(2, 3)
        last_violation_time = random_past_datetime(7)
    elif ban_roll < 0.993:
        ban_status = "BAN_TEMP_30"
        ban_end_time = random_future_datetime(30)
        violation_count = random.randint(3, 5)
        last_violation_time = random_past_datetime(14)
    else:
        ban_status = "BAN_PERMANENT"
        ban_end_time = "NULL"
        violation_count = random.randint(5, 10)
        last_violation_time = random_past_datetime(30)

    # 时间字段
    create_time = random_datetime_within_year()
    update_time = create_time  # updateTime 与 createTime 初始一致
    edit_time = create_time

    # 头像: 大部分为空，少部分使用 picsum 占位图
    avatar_roll = random.random()
    if avatar_roll < 0.7:
        user_avatar = ""
    else:
        seed = random.randint(1, 10000)
        user_avatar = f"https://picsum.photos/seed/{seed}/200/200"

    return {
        "id": generate_snowflake_id(index),
        "userAccount": random_account(index),
        "userPhone": random_phone(),
        "userEmail": random_email(),
        "userPassword": BCRYPT_PASSWORD,
        "userName": random_nickname(),
        "userAvatar": user_avatar,
        "userProfile": random_profile(),
        "userRole": user_role,
        "vipType": vip_type,
        "vipExpireTime": vip_expire_time,
        "vipActivatedAt": vip_activated_at,
        "vipTotalDays": vip_total_days,
        "banStatus": ban_status,
        "banEndTime": ban_end_time,
        "violationCount": violation_count,
        "lastViolationTime": last_violation_time,
        "editTime": edit_time,
        "createTime": create_time,
        "updateTime": update_time,
        "isDelete": 0,
    }


def build_value_clause(user: dict) -> str:
    """将一条用户数据转换为 SQL VALUES 子句中的一行。"""
    parts = []
    parts.append(str(user["id"]))
    parts.append(f"'{escape_sql_string(user['userAccount'])}'")
    parts.append(f"'{user['userPhone']}'")
    parts.append(f"'{user['userEmail']}'")
    parts.append(f"'{user['userPassword']}'")
    parts.append(f"'{escape_sql_string(user['userName'])}'")

    avatar = user["userAvatar"]
    parts.append(f"'{escape_sql_string(avatar)}'" if avatar else "NULL")

    profile = user["userProfile"]
    parts.append(f"'{escape_sql_string(profile)}'" if profile else "NULL")

    parts.append(f"'{user['userRole']}'")
    parts.append(str(user["vipType"]))

    parts.append(f"'{user['vipExpireTime']}'" if user["vipExpireTime"] != "NULL" else "NULL")
    parts.append(f"'{user['vipActivatedAt']}'" if user["vipActivatedAt"] != "NULL" else "NULL")
    parts.append(str(user["vipTotalDays"]))

    parts.append(f"'{user['banStatus']}'")
    parts.append(f"'{user['banEndTime']}'" if user["banEndTime"] != "NULL" else "NULL")
    parts.append(str(user["violationCount"]))
    parts.append(f"'{user['lastViolationTime']}'" if user["lastViolationTime"] != "NULL" else "NULL")

    parts.append(f"'{user['editTime']}'")

    parts.append(f"'{user['createTime']}'")
    parts.append(f"'{user['updateTime']}'")
    parts.append(str(user["isDelete"]))

    return "(" + ", ".join(parts) + ")"


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main():
    columns = [
        "id", "userAccount", "userPhone", "userEmail", "userPassword",
        "userName", "userAvatar", "userProfile", "userRole", "vipType",
        "vipExpireTime", "vipActivatedAt", "vipTotalDays", "banStatus",
        "banEndTime", "violationCount", "lastViolationTime", "editTime",
        "createTime", "updateTime", "isDelete",
    ]
    column_list = ", ".join(columns)

    print(f"开始生成 {TOTAL_USERS} 条用户数据...")

    # 固定随机种子，保证可复现
    random.seed(42)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        # 写入文件头注释
        f.write("-- ============================================\n")
        f.write("-- 用户表测试数据 (10000 条)\n")
        f.write("-- 生成时间: " + time.strftime("%Y-%m-%d %H:%M:%S") + "\n")
        f.write("-- 密码明文: 12345678 (BCrypt)\n")
        f.write("-- ============================================\n\n")

        # 如果有旧数据则先清空（可选）
        f.write("-- 清空旧数据（按需启用）\n")
        f.write("-- TRUNCATE TABLE `user`;\n\n")

        for batch_start in range(0, TOTAL_USERS, BATCH_SIZE):
            batch_end = min(batch_start + BATCH_SIZE, TOTAL_USERS)
            batch_num = batch_start // BATCH_SIZE + 1
            total_batches = (TOTAL_USERS + BATCH_SIZE - 1) // BATCH_SIZE

            f.write(f"-- 批次 {batch_num}/{total_batches} (用户 {batch_start + 1}-{batch_end})\n")
            f.write(f"INSERT INTO `user` ({column_list}) VALUES\n")

            values = []
            for i in range(batch_start, batch_end):
                user = generate_user(i)
                values.append(build_value_clause(user))

            f.write(",\n".join(values))
            f.write(";\n\n")

            if batch_num % 5 == 0:
                print(f"  已生成 {batch_end}/{TOTAL_USERS} 条...")

    file_size_mb = os.path.getsize(OUTPUT_FILE) / (1024 * 1024)
    print(f"\n完成! 文件已保存至: {OUTPUT_FILE}")
    print(f"文件大小: {file_size_mb:.2f} MB")


if __name__ == "__main__":
    main()
