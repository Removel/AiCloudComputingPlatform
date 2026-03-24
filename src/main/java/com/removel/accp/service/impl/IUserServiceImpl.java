package com.removel.accp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.intern.InternUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.ParamValidationException;
import com.removel.accp.mapper.UserMapper;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.enums.UserRole;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;
import com.removel.accp.service.IUserService;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import com.removel.accp.util.EmailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final RedisDataUtil redisDataUtil;
    private final UserMapper userMapper;
    private final EmailUtil  emailUtil;
    @Autowired
    public IUserServiceImpl(UserMapper userMapper, RedisDataUtil redisDataUtil, EmailUtil emailUtil) {
        this.userMapper = userMapper;
        this.redisDataUtil = redisDataUtil;
        this.emailUtil = emailUtil;
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
                if(StrUtil.isBlank(loginRequest.getEmail())){
                    log.error("用户当前选择的登录方式是：{}，但是邮箱为空",loginType);
                    throw new AuthException("用户邮箱不能为空",400);
                }
                queryWrapper.eq("email",loginRequest.getEmail());
            }
            case "phone"->{
                if(loginRequest.getPhone()==null||StrUtil.isBlank(loginRequest.getPhone().toString())){
                    log.error("用户当前选择的登录方式是：{}，但是用户电话为空",loginType);
                    throw new AuthException("用户电话不能为空",400);
                }
                queryWrapper.eq("phone",loginRequest.getPhone());
            }
            default->{
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
    public void sendCode(RegisterRequest registerRequest) {
        // TODO: 1、验证邮箱形式是否正确
        String email = registerRequest.getEmail();
        log.info("当前注册邮箱为：{}",email);
        // 1.1:判断邮箱是否为空
        if(StrUtil.isBlank(email)){
            log.error("邮箱为空，当前注册邮箱为：{}",email);
            throw new ParamValidationException("邮箱不能为空",400);
            }
        // 1.2:判断邮箱是否符合邮箱格式
        if(!email.matches("^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")){
            log.error("邮箱格式不正确，当前注册邮箱为：{}",email);
            throw new ParamValidationException("邮箱格式不正确",400);
        }
        // TODO: 2、生成邮箱验证码，将其存储在redis中，并设置过期时间
        String code = UUID.randomUUID().toString();
        redisDataUtil.set(RedisConstant.REGISTER_EMAIL_KEY+email,code,RedisConstant.REGISTER_CODE_TTL, TimeUnit.MINUTES);
        // TODO: 3、发送邮件，包含验证码
        emailUtil.sendEmail(email,"ACCP注册账号验证码","您的验证码为："+code);
        // TODO: 4、返回成功信息
        log.info("验证码发送成功,验证码为：{}",code);
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        // TODO: 1、验证注册验证码是否正确
        // 1.1:获取两者的验证码与邮箱
        String emailFromRequest = registerRequest.getEmail();
        String codeFromRequest = registerRequest.getCode();
        String codeFromRedis = redisDataUtil.query(RedisConstant.REGISTER_EMAIL_KEY+emailFromRequest,String.class);
        // 1.2:基础参数验证
        // 1.2.1:传入邮箱不能为空
        if(StrUtil.isBlank(emailFromRequest)){
            log.error("邮箱参数为空");
            throw new ParamValidationException("邮箱不能为空",400);
        }
        // 1.2.2:传入的验证码不能为空
        if(!emailFromRequest.equals(codeFromRequest)){
            log.error("验证码参数为空");
            throw new ParamValidationException("验证码不能为空",400);
        }
        // 1.2.3:获取的验证码不能为空
        if(StrUtil.isBlank(codeFromRedis)){
            log.error("验证码已过期，当前注册邮箱为：{}",emailFromRequest);
            throw new ParamValidationException("验证码已过期",400);
        }
        // 1.2.4:验证码必须相同
        if(!codeFromRequest.equals(codeFromRedis)){
            log.error("验证码错误，当前注册邮箱为：{}",emailFromRequest);
            throw new ParamValidationException("验证码错误",400);
        }
        log.info("注册验证码验证完毕");
        // TODO: 2、判断注册信息是否合法
        // 2.1：判断用户名是否合法，如果不合法抛出异常，合法则继续
        QueryWrapper<User> queryWrapperByName = new QueryWrapper<>();
        queryWrapperByName.eq("username",registerRequest.getUsername());
        if(userMapper.exists(queryWrapperByName)){
            log.error("用户名已存在，当前注册用户名为：{}",registerRequest.getUsername());
            throw new AuthException("用户名已存在",400);
        }
        // 2.2：判断邮箱是否合法，如果不合法则抛出异常，合法则继续
        QueryWrapper<User> queryWrapperByEmail = new QueryWrapper<>();
        queryWrapperByEmail.eq("email",registerRequest.getEmail());
        if(userMapper.exists(queryWrapperByEmail)){
            log.error("邮箱已存在，当前注册邮箱为：{}",registerRequest.getEmail());
            throw new AuthException("邮箱已存在",400);
        }
        log.info("注册信息验证完成");
        // TODO: 3、创建新的用户实例并设置用户属性
        User user = new User();
        user.setName(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setStatus(Status.NORMAL);
        user.setRole(UserRole.COMMON);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setRemainingComputePower(10);
        log.info("用户信息设置完成，当前用户信息为：{}",user);
        // TODO: 4、将用户信息存入数据库
        userMapper.insert(user);
        log.info("用户信息存入数据库成功");
    }

    @Override
    public void logout(Integer id,String token) {
        // TODO: 1、解析token（如果token无效/过期，直接返回成功，让前端清除本地状态即可）
        User user = redisDataUtil.query(RedisConstant.LOGIN_CODE_KEY+token,User.class);
        // 1.1:token无效或者已经过期导致redis不能查到对应数据
        if(user == null||BeanUtil.isEmpty(user)){
            log.info("token无效或者已经过期，当前token为：{}",token);
            return ;
        }
        // 1.2:token有效，判断用户id是否一致，防止被盗用下线
        if(user.getId()!=id) {
            log.error("用户id与通过token获取的id不一致，当前用户id为：{}，token获取的用户id为：{}",id,user.getId());
            throw new ParamValidationException("用户id与token不匹配",400);
        }
        // TODO: 2、验证用户状态是否合法
        if(user.getStatus()!=Status.NORMAL){
            log.error("用户状态异常，当前用户id为：{}，状态为：{}",id,user.getStatus());
            throw new AuthException("用户状态异常",403);
        }
        // TODO: 3、将用户信息从redis中删除
        redisDataUtil.delete(RedisConstant.LOGIN_CODE_KEY+token);
    }
}
