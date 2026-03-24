package com.removel.accp.util.CacheClientUtil;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private Object value;
    private LocalDateTime expireTime;
}
