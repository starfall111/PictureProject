-- 滑动窗口限流算法
-- KEYS[1]: 限流 key (Sorted Set, score = timestamp)
-- ARGV[1]: 窗口大小（毫秒）
-- ARGV[2]: 最大允许次数
-- ARGV[3]: 当前时间戳（毫秒）
-- ARGV[4]: key 过期时间（秒）
--
-- 返回值: -1 表示被限流; >=0 表示通过，值为剩余次数

-- 1. 移除窗口外的过期记录
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[3] - ARGV[1])

-- 2. 统计窗口内请求数
local count = redis.call('ZCARD', KEYS[1])

-- 3. 判断是否超过限制
local maxAttempts = tonumber(ARGV[2])
if count >= maxAttempts then
    return -1
end

-- 4. 添加当前请求记录（成员值加随机后缀防重复覆盖）
redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3] .. '_' .. math.random(100000))

-- 5. 设置过期时间
redis.call('EXPIRE', KEYS[1], ARGV[4])

-- 6. 返回剩余次数
return maxAttempts - count - 1
