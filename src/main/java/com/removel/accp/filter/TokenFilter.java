package com.removel.accp.filter;


import com.removel.accp.exception.AuthException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
@Slf4j
@WebFilter("/*")
public class TokenFilter implements Filter {

    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // TODO: 1、强转参数
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // TODO: 2、检测路径
        String requestURI = request.getRequestURI();
        log.info("请求路径：requestURI:{}", requestURI);
        // 如果为登录注册接口，则放行
        if (requestURI.contains("/login") || requestURI.contains("/register")) {
            log.info("放行登录注册接口");
            filterChain.doFilter(request, response);
        }
        // TODO: 3、从请求头中获取token
        String authorization = request.getHeader("Authorization");
        log.info("请求头中Authorization：authorization:{}", authorization);
        // 3.1如果authorization为空，则抛出异常
        if(authorization.isBlank()) {
            log.error("未在authorization头中检测到token");
            throw new AuthException("token不能为空");
        }
        // TODO: 4、放行
        log.info("token为：{}", authorization);
        log.info("tokenFilter放行");
        filterChain.doFilter(request, response);
    }

}
