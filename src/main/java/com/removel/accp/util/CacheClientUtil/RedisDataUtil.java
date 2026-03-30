package com.removel.accp.util.CacheClientUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class RedisDataUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 通过给定任意对象存入redis，物理过期存储
    public void setWithPhysicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),expireTime,timeUnit);
    }

    // 逻辑过期存入任意对象，逻辑过期存储
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit){
        // 将对象包装成RedisData对象，并设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        redisData.setValue(value);
        //将RedisData对象存入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
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

    /**
     *
     * @param keyPrefix key前缀，用于redis查询
     * @param id id，查询对象的唯一身份标识
     * @param time 过期时间ttl，仅数字
     * @param timeUnit 过期时间ttl类型
     * @param dbFallback 从数据库查询的方法传入
     * @return 返回对象为R泛型
     * @param <R> 泛型定义R
     * @param <ID> 泛型定义ID
     */
    //解决缓存穿透，缓存穿透查询
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id,Class <R> type, Long time, TimeUnit timeUnit, Function<ID,R> dbFallback){
        // TODO:1、拼接字符串前缀
        String key = keyPrefix+id;
        // TODO:2、从redis查询缓存
        String resultJson = stringRedisTemplate.opsForValue().get(key);
        // TODO:3、判断redis存在情况
        // 3.1:存在，返回结果
        if(StrUtil.isNotBlank(resultJson)){
            return JSONUtil.toBean(resultJson,type);
        }
        else if (resultJson != null) {
            // 空值缓存命中，返回null
            log.debug("空值缓存命中，id：{}", id);
            return null;
        }
        // 3.2:不存在：
        // 3.2.1:返回错误信息
        else log.debug("查询的对象在redis中不存在，缓存未命中，种类是：{}，id为：{}",type,id);
        // 3.2.2:进入步骤4
        // TODO:4、不存在，查库
        R result = dbFallback.apply(id);
        // TODO:5、判断库存在情况
        // 5.1:存在:
        if(BeanUtil.isNotEmpty(result)){
            // 5.1.1:写入redis
            this.setWithPhysicalExpire(key,result,time,timeUnit);
            // 5.1.2:返回数据
            return result;
        }
        // 5.2:不存在:
        else {
            // 5.2.1:redis存空值
            this.setWithPhysicalExpire(key,"",time,timeUnit);
            // 5.2.2:返回错误信息
            log.warn("在数据库中未查询到相关信息，对象种类为：{}，id为：{}",type,id);
            return null;
        }
    }

}
