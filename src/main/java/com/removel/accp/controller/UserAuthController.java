package com.removel.accp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.removel.accp.annotation.LogOperation;
import com.removel.accp.exception.AuthException;
import com.removel.accp.model.Result;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;
import com.removel.accp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class UserAuthController {

    // 构造器方法注入字段
    private StringRedisTemplate stringRedisTemplate;
    private IUserService iUserService;
    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate, IUserService iUserService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.iUserService = iUserService;
    }

    //登陆接口
    @LogOperation
    @PostMapping("/api/login")
    public Result<?> login(@RequestBody LoginRequest loginRequest){
        log.info("登录请求，登录方式为：{}",loginRequest.getLoginType());
        User user = iUserService.login(loginRequest);
        log.info("登录成功，登录用户信息为：{}",user);
        return Result.success();
    }
    //注册接口
    @LogOperation
    @PostMapping("/api/register")
    public Result<?> register(@RequestBody RegisterRequest registerRequest){
        log.info("注册请求，注册信息为：{}",registerRequest);
        iUserService.register(registerRequest);
        return Result.success();
    }
    //登出接口

}
