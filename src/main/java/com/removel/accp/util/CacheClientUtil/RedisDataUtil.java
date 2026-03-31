package com.removel.accp.util.CacheClientUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.removel.accp.model.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.Event;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class RedisDataUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Autowired
    public RedisDataUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
            this.setWithPhysicalExpire(key,"",RedisConstant.CACHE_NULL_TTL,TimeUnit.MINUTES);
            // 5.2.2:返回错误信息
            log.warn("在数据库中未查询到相关信息，对象种类为：{}，id为：{}",type,id);
            return null;
        }
    }

    //私有方法：尝试获取锁，返回标识成功的布尔值
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //私有方法：释放锁
    private void unlock(String key){
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
    //解决缓存击穿，使用逻辑过期查询()
    public <R,ID> R queryWithLogicalExpire(String keyPrefix,ID id, Class<R> type, Long time, TimeUnit timeUnit, Function<ID,R> dbFallback){
        // TODO:1、拼接字符串前缀
        String key = keyPrefix+id;
        // TODO:2、从redis查询缓存
        String resultJson = stringRedisTemplate.opsForValue().get(key);
        //2.1:不存在，直接返回null
        if(StrUtil.isBlank(resultJson)){
            log.warn("未查询到对应数据，key:{}",key);
            return null;
        }
        // TODO:3、存在，解析缓存数据
        RedisData redisData = JSONUtil.toBean(resultJson,RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getValue(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // TODO:4、判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 4.1:未过期，直接返回
            return r;
        }
        // 4.2:过期，执行缓存重建
        // TODO:5、执行缓存重建
        //5.1：尝试获取锁
        String lockKey = RedisConstant.LOCK_SHOP_KEY + id;
        //5.2：得到获取锁的情况
        boolean isLock = tryLock(lockKey);
        //5.3：如果获取成功：
        if(isLock){
            //成功:开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try{
                    // TODO:6、查询数据库
                    R result = dbFallback.apply(id);
                    // TODO:7、判断数据库对象是否存在
                    if(BeanUtil.isNotEmpty(result)){
                        // 7.1:存在，存入redis
                        this.setWithLogicalExpire(key,result,time,timeUnit);
                    }
                    // 7.2:不存在，存入空值
                    else {
                        this.setWithLogicalExpire(key,"",time,timeUnit);
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    // 最终要释放锁
                    unlock(lockKey);
                }
            });
        }
        //最终返回结果
        return r;
    }

    //使用互斥锁查询，解决缓存击穿的问题
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // TODO:1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // TODO:2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 2.1:存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 2.2:判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // TODO:3.实现缓存重建
        // 3.1:获取互斥锁
        String lockKey = RedisConstant.LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 3.2:判断是否获取成功
            if (!isLock) {
                // 3.2.1:获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 3.2.2:获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 3.3:不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", RedisConstant.CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // TODO:4.存在，写入redis
            this.setWithPhysicalExpire(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // TODO:5.释放锁
            unlock(lockKey);
        }
        // TODO:6.返回
        return r;
    }
}
