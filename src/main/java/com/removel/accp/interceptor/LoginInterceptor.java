package com.removel.accp.interceptor;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
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
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 1、获取token
        String token = request.getHeader("authorization");
        log.info("当前请求token: {}", token);
        // TODO: 2、校验token
        String result = stringRedisTemplate.opsForValue().get(RedisConstant.LOGIN_CODE_KEY + token);
        if (result==null || StrUtil.isBlank(result)) {
            log.error("token无效");
            throw new AuthException("token无效", 401);
        }
        // TODO: 3、从redis中获取用户信息，转化为用户类
        User user = JSONUtil.toBean(result, User.class);
        // TODO: 4、校验用户状态
        if (user==null||user.getStatus() != Status.NORMAL) {
            log.error("用户状态异常");
            throw new AuthException("用户状态异常", 401);
        }
        // TODO: 5、将用户信息放入Thread local保存用户信息
        UserHolder.setUser(user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // TODO: 清除用户信息防止内存泄漏
        UserHolder.removeUser();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
