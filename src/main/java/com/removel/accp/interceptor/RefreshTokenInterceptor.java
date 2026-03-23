package com.removel.accp.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.removel.accp.exception.AuthException;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 1、获取token与用户
        String token = request.getHeader("authorization");
        User user = UserHolder.getUser();
        // TODO: 2、校验token与用户
        if(token == null || StrUtil.isBlank(token) || user==null || user.getStatus() != Status.NORMAL) {
            log.error("token或者用户异常");
            log.error("token:{},user:{}",token,user);
            throw new AuthException("令牌或者用户异常");
        }
        // TODO: 3、刷新token
        String userJson = JSONUtil.toJsonStr(user);
        stringRedisTemplate.opsForValue().set(RedisConstant.LOGIN_CODE_KEY+token,userJson,RedisConstant.LOGIN_CODE_TTL, TimeUnit.DAYS);
        // TODO: 4、返回true
        return true;
    }
}
