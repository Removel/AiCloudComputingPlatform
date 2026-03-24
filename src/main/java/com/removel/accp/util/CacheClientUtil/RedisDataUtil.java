package com.removel.accp.util.CacheClientUtil;

import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDataUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 通过给定任意对象存入redis
    public void set(String key, Object value, Long expireTime, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),expireTime,timeUnit);
    }

    // 逻辑过期存入任意对象
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        redisData.setValue(value);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData),expireTime,timeUnit);
    }

}
