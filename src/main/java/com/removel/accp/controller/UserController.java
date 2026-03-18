package com.removel.accp.controller;

import com.removel.accp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    // 构造器方法注入字段
    private final StringRedisTemplate stringRedisTemplate;
    private final IUserService iUserService;
    @Autowired
    public UserController(IUserService iUserService,StringRedisTemplate stringRedisTemplate) {
        this.iUserService = iUserService;
        this.stringRedisTemplate = stringRedisTemplate;
    }



}
