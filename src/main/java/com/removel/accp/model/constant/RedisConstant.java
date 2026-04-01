package com.removel.accp.model.constant;

public class RedisConstant {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;

    public static final String REGISTER_EMAIL_KEY = "register:email:";
    public static final Long REGISTER_CODE_TTL = 30L;

    public static final String LOCK_MACHINE_KEY = "lock:machine:";

    public static final String CACHE_MACHINE_KEY = "cache:machine:";
    public static final Long CACHE_MACHINE_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;
}
