package com.removel.accp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogAspect {

    // 用于计算一个方法的信息与耗时
    @Around("@annotation(com.removel.accp.annotation.LogOperation)")
    public Object LogAround(ProceedingJoinPoint pjp) throws Throwable {
        Object target = pjp.getTarget();    // 获取目标对象
        String className = target.getClass().getSimpleName();   //获取目标类
        String methodName = pjp.getSignature().getName();   //获取目标方法
        Object[] args = pjp.getArgs();  //获取目标参数
        log.info("当前请求的目标类为：{}",className);
        log.info("当前请求的目标方法为：{}",methodName);
        log.info("当前请求的目标参数为：{}",args);
        log.info("当前请求的目标对象为：{}", target);
        Long startTime = System.currentTimeMillis();
        Object result = pjp.proceed();
        Long endTime = System.currentTimeMillis();
        log.info("该请求使用的时间为：{}ms",endTime-startTime);
        log.info("当前请求的结果为：{}",result);
        return result;
    }

}
