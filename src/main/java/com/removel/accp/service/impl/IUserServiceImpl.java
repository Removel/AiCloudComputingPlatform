package com.removel.accp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.mapper.UserMapper;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;
import com.removel.accp.service.IUserService;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final RedisDataUtil redisDataUtil;
    private final UserMapper userMapper;
    @Autowired
    public IUserServiceImpl(UserMapper userMapper,RedisDataUtil redisDataUtil) {
        this.userMapper = userMapper;
        this.redisDataUtil = redisDataUtil;
    }

    @Override
    public User login(LoginRequest loginRequest) {
        // TODO: 1、获取登录请求方式，对应请求方式构造queryWrapper
        String loginType = loginRequest.getLoginType();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        switch (loginType){
            case "username"->{
                if(StrUtil.isBlank(loginRequest.getUsername())){
                    log.error("用户当前选择的登录方式是：{}，但是用户名为空",loginType);
                    throw new AuthException("用户名不能为空",400);
                }
                queryWrapper.eq("username",loginRequest.getUsername());
            }
            case "email"->{
                log.error("用户当前选择的登录方式是：{}，但是邮箱为空",loginType);
                if(StrUtil.isBlank(loginRequest.getEmail())){
                    throw new AuthException("用户邮箱不能为空",400);
                }
                queryWrapper.eq("email",loginRequest.getEmail());
            }
            case "phone"->{
                if(StrUtil.isBlank(loginRequest.getPhone().toString())){
                    log.error("用户当前选择的登录方式是：{}，但是用户电话为空",loginType);
                    throw new AuthException("用户电话不能为空",400);
                }
                queryWrapper.eq("phone",loginRequest.getPhone());
            }
            case "default"->{
                log.error("登陆方式错误，当前传入登陆方式为：{}",loginRequest.getLoginType());
                throw new AuthException("请求登陆方式错误",400);
            }
        }
        if(StrUtil.isBlank(loginRequest.getPassword())){
            log.error("用户当前选择的登录方式是：{}，但是密码为空",loginType);
            throw new AuthException("用户密码不能为空",400);
        }
        queryWrapper.eq("password",loginRequest.getPassword());
        // TODO: 2、根据queryWrapper查询用户信息
        User user = userMapper.selectOne(queryWrapper);
        // TODO: 3、判断用户信息是否有效，不存在则返回错误信息
        if (BeanUtil.isEmpty(user)){
            log.error("用户名或密码错误，登入请求为：{}",loginRequest);
            throw new AuthException("用户名或密码错误",401);
        }
        if(user.getStatus()!= Status.NORMAL){
            log.error("用户状态异常，当前用户名为：{}，当前用户状态为：{}",user.getName(),user.getStatus());
            throw new AuthException("用户状态异常",400);
        }
        // TODO：4、向redis存入用户信息
        String token = UUID.randomUUID().toString();
        redisDataUtil.set(RedisConstant.LOGIN_CODE_KEY+token,user,RedisConstant.LOGIN_CODE_TTL, TimeUnit.DAYS);
        // TODO: 5、返回用户信息
        return user;
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        // TODO: 1、判断注册信息是否合法
        // 1.1：判断用户名是否合法，如果不合法抛出异常，合法则继续
        QueryWrapper<User> queryWrapperByName = new QueryWrapper<>();
        queryWrapperByName.eq("username",registerRequest.getUsername());
        if(userMapper.exists(queryWrapperByName)){
            log.error("用户名已存在，当前注册用户名为：{}",registerRequest.getUsername());
            throw new AuthException("用户名已存在",400);
        }
        // 1.2：判断邮箱是否合法，如果不合法则抛出异常，合法则继续
        QueryWrapper<User> queryWrapperByEmail = new QueryWrapper<>();
        queryWrapperByEmail.eq("email",registerRequest.getEmail());
        if(userMapper.exists(queryWrapperByEmail)){
            log.error("邮箱已存在，当前注册邮箱为：{}",registerRequest.getEmail());
            throw new AuthException("邮箱已存在",400);
        }
        // TODO: 2、发送邮箱验证码
    }
}
