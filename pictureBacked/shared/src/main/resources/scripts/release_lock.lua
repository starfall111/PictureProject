-- 原子释放分布式锁：仅当锁的值等于预期值时才删除
-- KEYS[1]: lock key
-- ARGV[1]: expected lock value (UUID)
-- 返回: 1=删除成功, 0=值不匹配(锁已被他人持有)
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
