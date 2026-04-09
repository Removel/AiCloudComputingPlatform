package com.removel.accp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
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
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
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
    public String login(LoginRequest loginRequest) {
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
        user.setPassword(null); //将密码置空，隐藏敏感信息
        String token = UUID.randomUUID().toString();
        redisDataUtil.setWithPhysicalExpire(RedisConstant.LOGIN_CODE_KEY+token,user,RedisConstant.LOGIN_CODE_TTL, TimeUnit.DAYS);
        // TODO: 5、返回用户信息token
        return token;
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
        redisDataUtil.setWithPhysicalExpire(RedisConstant.REGISTER_EMAIL_KEY+email,code,RedisConstant.REGISTER_CODE_TTL, TimeUnit.MINUTES);
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
        if(user.getId().equals(id)) {
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

    @Override
    public User getUserInfo(String token) {
        // TODO: 1、验证token是否符合最基本要求（但是一般拦截器和过滤器就会处理掉）
        if(StrUtil.isBlank(token)){
            throw new ParamValidationException("token不能为空",400);
        }
        // TODO: 2、从UserHolder当中查询用户信息
        User user = UserHolder.getUser();
        // TODO: 3、验证用户信息查询结果
        // 3.1：是否存在
        if(user == null||BeanUtil.isEmpty(user)){
            log.error("token无效或者已经过期，当前token为：{}",token);
            throw new ParamValidationException("token无效或者已经过期",400);
        }
        // TODO: 4、返回用户信息
        return user;
    }

    @Transactional
    @Override
    public void deleteUser(Integer id) {
        // TODO: 1、从UserHolder中获取到当前正在操作的用户
        User currentUser = UserHolder.getUser();
        log.info("当前正在操作的用户为：{}",currentUser);
        // TODO: 2、权限判断
        // 逻辑：不是自己并且不是管理员且管理员状态不对的无法操作
        if (currentUser.getId().equals(id) &&
                (!currentUser.getRole().equals(UserRole.ADMIN) || !currentUser.getStatus().equals(Status.NORMAL))){
            log.error("当前用户删除权限不足，当前用户id为：{}，想要操作的id为：{}",currentUser.getId(),id);
            throw new AuthException("权限不足",403);
        }
        // TODO: 3、删除用户，修改状态实现逻辑删除
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        // 设置 id 等于传入的 id
        updateWrapper.eq(User::getId, id)
                // 设置status为cancelled，注销
                .set(User::getStatus, Status.CANCELLED)
                // 设置更新时间
                .set(User::getUpdateTime,LocalDateTime.now());
        // TODO: 4、执行更新
        userMapper.update(null, updateWrapper);
        log.info("更新完成");
    }

    @Transactional
    @Override
    public void updateUser(User user) {
        // TODO: 1、从UserHolder中获取到当前正在操作的用户
        User currentUser = UserHolder.getUser();
        if(currentUser == null){
            log.error("当前操作用户不存在");
            throw new AuthException("当前操作用户不存在",403);
        }
        log.info("当前正在执行更新操作的用户为：{}",currentUser);

        // TODO: 2、校验传入参数
        if(user == null||BeanUtil.isEmpty(user)){
            log.error("传入参数:user 为空");
            throw new ParamValidationException("参数为空",400);
        }

        // TODO: 3、判断权限：
        boolean isSelf = currentUser.getId().equals(user.getId());
        boolean isAdmin = UserRole.ADMIN.equals(currentUser.getRole());
        boolean isAdminNormal = isAdmin && Status.NORMAL.equals(currentUser.getStatus());

        // 3.1 修改他人：必须是状态正常的管理员
        if (!isSelf) {
            if (!isAdminNormal) {
                log.error("权限不足：用户{}尝试修改用户{}的信息，isAdmin={}, statusNormal={}",
                        currentUser.getId(), user.getId(), isAdmin,
                        Status.NORMAL.equals(currentUser.getStatus()));
                throw new AuthException("权限不足，无法修改他人信息", 403);
            }
            log.info("管理员{}正在修改用户{}的信息", currentUser.getId(), user.getId());
        }
        // 3.2 修改自己：保护敏感字段
        else {
            // 非管理员不能修改自己的角色和状态和余额和创建时间
            if (!isAdmin) {
                if (!Objects.equals(currentUser.getRole(), user.getRole()) ||
                        !Objects.equals(currentUser.getStatus(), user.getStatus())||
                        !Objects.equals(currentUser.getCreateTime(), user.getCreateTime()) ||
                        !Objects.equals(currentUser.getRemainingComputePower(), user.getRemainingComputePower())) {
                    log.warn("普通用户{}尝试修改自己的敏感字段，role:{}=>{}, status:{}=>{},createTime:{}=>{},remainComputePower:{}=>{}",
                            currentUser.getId(),
                            currentUser.getRole(), user.getRole(),
                            currentUser.getStatus(), user.getStatus(),
                            currentUser.getCreateTime(), user.getCreateTime(),
                            currentUser.getRemainingComputePower(), user.getRemainingComputePower());
                    // 强制使用原值
                    user.setRole(currentUser.getRole());
                    user.setStatus(currentUser.getStatus());
                    user.setCreateTime(currentUser.getCreateTime());
                    user.setRemainingComputePower(currentUser.getRemainingComputePower());
                }
            }
            // 状态异常的管理员也不能修改自己的角色和状态和余额
            else if (!Status.NORMAL.equals(currentUser.getStatus())) {
                if (!Objects.equals(currentUser.getRole(), user.getRole()) ||
                        !Objects.equals(currentUser.getStatus(), user.getStatus())||
                        !Objects.equals(currentUser.getCreateTime(), user.getCreateTime()) ||
                        !Objects.equals(currentUser.getRemainingComputePower(), user.getRemainingComputePower())) {
                    log.warn("状态异常的管理员用户{}尝试修改自己的敏感字段，role:{}=>{}, status:{}=>{},createTime:{}=>{},remainComputePower:{}=>{}",
                            currentUser.getId(),
                            currentUser.getRole(), user.getRole(),
                            currentUser.getStatus(), user.getStatus(),
                            currentUser.getCreateTime(), user.getCreateTime(),
                            currentUser.getRemainingComputePower(), user.getRemainingComputePower());
                    // 强制使用原值
                    user.setRole(currentUser.getRole());
                    user.setStatus(currentUser.getStatus());
                    user.setCreateTime(currentUser.getCreateTime());
                    user.setRemainingComputePower(currentUser.getRemainingComputePower());
                }
            }
            log.info("用户{}正在修改自己的信息", currentUser.getId());
        }

        // TODO: 4、补全更新用户信息
        user.setUpdateTime(LocalDateTime.now());

        // TODO: 5、执行更新，并检查是否成功更新
        int updateCount = userMapper.updateById(user);
        if(updateCount != 1){
            log.error("更新失败，尝试更新用户id为：{}",user.getId());
            throw new BusinessException("更新失败",400);
        }
        log.info("用户{}成功更新了用户{}的信息", currentUser.getId(), user.getId());
    }

    @Transactional
    @Override
    public void deductRemainingComputePower(Integer userId, Integer cost) {
        //TODO:1:使用悲观锁,锁住用户
        User user =  this.lambdaQuery()
                .eq(User::getId, userId)
                .last("FOR UPDATE")
                .one();
        //TODO:2:判断余额是否足够
        if (user.getRemainingComputePower() < cost) {
            throw new BusinessException("余额不足", 402);
        }
        //TODO:3:扣减余额
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(User::getRemainingComputePower, user.getRemainingComputePower() - cost)
                .eq(User::getId, userId);
        boolean result = this.update(updateWrapper);
        //TODO:4:判断扣减是否成功
        if (!result) {
            log.error("扣减余额失败，尝试扣减用户id为：{}的余额，扣减金额为：{}", userId, cost);
            throw new BusinessException("扣减余额失败", 500);
        }
        log.info("扣减余额成功，用户id为：{}，扣减金额为：{}", userId, cost);
    }
}
