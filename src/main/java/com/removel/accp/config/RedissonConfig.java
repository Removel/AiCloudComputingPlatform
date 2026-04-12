package com.removel.accp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机模式
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);

        // 集群模式
        // config.useClusterServers()
        //     .addNodeAddress("redis://127.0.0.1:7000", "redis://127.0.0.1:7001")
        //     .setPassword("your-password");

        return Redisson.create(config);
    }

}
