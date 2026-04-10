package com.removel.accp.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;

//通过hutool下的雪花算法实现全局唯一雪花id生成器
@Component
public class SnowflakeIdGenerator {

    private Snowflake snowflake;

    @PostConstruct
    public void init(){
        // 使用 MAC 地址，比 IP 更稳定
        String mac = NetUtil.getLocalMacAddress();
        long workerId = mac.hashCode() & 1023;
        // 数据中心 ID（个人项目固定 0 即可）
        long dataCenterId = 0;
        snowflake = IdUtil.getSnowflake(workerId,dataCenterId);
    }

    //获取全局唯一id
    public long nextId(){
        return snowflake.nextId();
    }

}
