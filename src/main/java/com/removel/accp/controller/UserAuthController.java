package com.removel.accp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.removel.accp.annotation.LogOperation;
import com.removel.accp.exception.AuthException;
import com.removel.accp.model.Result;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;
import com.removel.accp.model.request.UserRequest;
import com.removel.accp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class UserAuthController {

    // 构造器方法注入字段
    private IUserService iUserService;
    @Autowired
    public void setStringRedisTemplate(IUserService iUserService) {
        this.iUserService = iUserService;
    }

    //登陆接口
    @LogOperation
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest loginRequest){
        log.info("登录请求，登录方式为：{}",loginRequest.getLoginType());
        User user = iUserService.login(loginRequest);
        log.info("登录成功，登录用户信息为：{}",user);
        return Result.success();
    }
    //注册接口
    @LogOperation
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest registerRequest){
        log.info("注册请求，注册信息为：{}",registerRequest);
        iUserService.register(registerRequest);
        log.info("注册成功");
        return Result.success();
    }

    //发送验证码接口
    @LogOperation
    @PostMapping("/register/code")
    public Result<?> sendCode(@RequestBody RegisterRequest registerRequest){
        log.info("发送验证码请求，注册信息为：{}",registerRequest);
        iUserService.sendCode(registerRequest);
        log.info("发送成功");
        return Result.success();
    }

    //登出接口
    @LogOperation
    @PostMapping("/logout/{id}")
    public Result<?> logout(@PathVariable Integer id,@RequestHeader("Authorization") String token){
        log.info("登出请求，登出用户id为：{}，登出用户token为：{}",id,token);
        iUserService.logout(id,token);
        log.info("登出成功");
        return Result.success();
    }
}
