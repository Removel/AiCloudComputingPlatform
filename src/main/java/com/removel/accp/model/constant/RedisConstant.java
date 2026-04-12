package com.removel.accp.model.constant;

import java.security.PublicKey;
import java.util.UUID;

public class RedisConstant {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;

    public static final String REGISTER_EMAIL_KEY = "register:email:";
    public static final Long REGISTER_CODE_TTL = 30L;

    public static final String LOCK_MACHINE_KEY = "lock:machine:";

    public static final String CACHE_MACHINE_KEY = "cache:machine:";
    public static final Long CACHE_MACHINE_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final String SECKILL_ORDER_QUEUE_KEY = "stream:seckill:order:";
    public static final String CONSUMER_GROUP = "seckill-consumer-group";
    public static final String CONSUMER_NAME = "consumer-"+ UUID.randomUUID().toString();
    public static volatile boolean RUNNING = true;
    public static final String SECKILL_ORDER_DEAD_QUEUE_KEY = "stream:seckill:order:dead:";

    public static final String CHAT_SESSION_KEY = "chat:session:";

    public static final String CHAT_MESSAGE_QUEUE_KEY = "list:chat:message:";
    public static final Long CHAT_MESSAGE_QUEUE_TTL = 10L;

    public static final String CACHE_LIST_NULL_VAL = "null_list";
}
