local couponId = ARGV[1] -- 秒杀券id
local userId = ARGV[2]  -- 用户id

local inventoryKey = "seckill:inventory:"..couponId -- 秒杀券库存key
local orderKey = "seckill:order:"..couponId -- 秒杀券订单key

-- 判断库存是否充足
if(tonumber(redis.call('get', inventoryKey)) <= 0) then
    return 1
end

-- 判断用户是否已经抢购过
if(redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 扣减库存
redis.call('incrby', inventoryKey, -1)
-- 记录用户
redis.call('sadd', orderKey, userId)
-- 返回结果
return 0

