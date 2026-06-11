local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))
if stock then
    redis.call('HINCRBY', KEYS[1], 'stock', tonumber(ARGV[1]))
    redis.call('HINCRBY', KEYS[1], 'version', 1)
    return 1
end
return 0
