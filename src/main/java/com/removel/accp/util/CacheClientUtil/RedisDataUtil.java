package com.removel.accp.util.CacheClientUtil;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
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
        // 将对象包装成RedisData对象，并设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        redisData.setValue(value);
        //将RedisData对象存入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData),expireTime,timeUnit);
    }

    // 通过给定key简单获取redis中的对象
    public <R> R query(String key,Class<R> type){
        String value = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(value)){
            log.warn("未查询到相关数据");
            return null;
        }
        return JSONUtil.toBean(value,type);
    }

    public void delete(String key){
        stringRedisTemplate.delete(key);
    }

}
