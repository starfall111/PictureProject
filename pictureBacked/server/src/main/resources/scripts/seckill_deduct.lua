local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))
local version = tonumber(redis.call('HGET', KEYS[1], 'version'))
if stock == nil or stock <= 0 then return -1 end
if version ~= tonumber(ARGV[2]) then return -2 end
redis.call('HINCRBY', KEYS[1], 'stock', -tonumber(ARGV[1]))
redis.call('HINCRBY', KEYS[1], 'version', 1)
return 1
