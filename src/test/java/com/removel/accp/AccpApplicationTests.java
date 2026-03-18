package com.removel.accp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class AccpApplicationTests {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void redisConnectionTest(){
        String key = "key";
        String name = "Removel";
        stringRedisTemplate.opsForValue().set(key,name);
        String result = stringRedisTemplate.opsForValue().get(key);
        System.out.println(result);
    }
}
