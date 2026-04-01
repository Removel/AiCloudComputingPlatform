package com.removel.accp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.exception.ParamValidationException;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.ServerMachineMapper;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.enums.UserRole;
import com.removel.accp.service.IServerMachineService;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IServerMachineServiceImpl extends ServiceImpl<ServerMachineMapper, ServerMachine> implements IServerMachineService{

    private final RedisDataUtil redisDataUtil;
    private final ServerMachineMapper serverMachineMapper;
    @Autowired
    public IServerMachineServiceImpl(ServerMachineMapper serverMachineMapper, RedisDataUtil redisDataUtil) {
        this.serverMachineMapper = serverMachineMapper;
        this.redisDataUtil = redisDataUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    // 锁了之后记得更新！！！
    public ServerMachine selectMachineByIdWithPessimisticLock(Long id) {
        return serverMachineMapper.selectByIdForUpdate(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addMachine(ServerMachine serverMachine) {
        log.info("添加服务器，添加对象为：{}",serverMachine);
        // TODO:1.判断用户是否合法
        User user = UserHolder.getUser();
        log.info("正在操作的用户为：{}，用户id为：{}",user.getName(),user.getId());
        if(user.getRole()!= UserRole.ADMIN&&user.getStatus()!=Status.NORMAL){
            log.error("用户不合法，无法添加服务器");
            throw new AuthException("权限不足", 403);
        }
        // TODO:2.验证服务器信息
        if(this.getById(serverMachine.getId())!=null){
            log.error("服务器id已存在，但是不应该对添加的服务器给定id，已存在id为：{}",serverMachine.getId());
            throw new ParamValidationException("服务器id已存在", 400);
        }
        if(serverMachine.getId()!=null){
            log.error("存在自己添加的服务器id，不能自己添加服务器id");
            throw new ParamValidationException("不能指定服务器id", 400);
        }
        if(StrUtil.isBlank(serverMachine.getServerName())){
            log.error("添加的服务器名称不能为空");
            throw new ParamValidationException("服务器名称不能为空", 400);
        }
        if(StrUtil.isBlank(serverMachine.getIp())){
            log.error("添加的服务器ip不能为空");
            throw new ParamValidationException("服务器ip不能为空", 400);
        }
        if(BeanUtil.isEmpty(serverMachine.getSpec())){
            log.error("添加的服务器规格不能为空");
            throw new ParamValidationException("服务器规格不能为空", 400);
        }
        if(serverMachine.getTotalComputePower()==0){
            log.error("添加的服务器总算力不能为0");
            throw new ParamValidationException("服务器总算力不能为0", 400);
        }
        // TODO:3:补全服务器信息
        serverMachine.setOccupiedComputePower(0);
        serverMachine.setStatus(Status.NORMAL);
        serverMachine.setCreateTime(LocalDateTime.now());
        serverMachine.setUpdateTime(LocalDateTime.now());
        // TODO:4:将新增的服务器存储在数据库中
        int result = serverMachineMapper.insert(serverMachine);
        if(result==0){
            log.error("服务器添加失败");
            throw new BusinessException("服务器添加失败");
        }
        log.info("服务器添加成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteMachine(Long id) {
        //不会真实删除，仅将状态修改
        log.info("删除服务器，删除对象id为：{}",id);
        // TODO:1.判断用户是否合法
        User user = UserHolder.getUser();
        log.info("正在进行服务器删除操作的用户为：{}，用户id为：{}",user.getName(),user.getId());
        if(user.getRole()!= UserRole.ADMIN&&user.getStatus()!=Status.NORMAL){
            log.error("用户尝试越权删除服务器，用户名为：{}，用户id为：{}",user.getName(),user.getId());
            throw new AuthException("权限不足", 403);
        }
        // TODO:2.使用带解决缓存穿透查询判断服务器是否存在，数据库使用悲观锁查询保持一致性
        ServerMachine exist = redisDataUtil.queryWithPassThrough(
                RedisConstant.CACHE_MACHINE_KEY,
                id,
                ServerMachine.class,
                RedisConstant.CACHE_MACHINE_TTL,
                TimeUnit.MINUTES,
                this::selectMachineByIdWithPessimisticLock);
        if(BeanUtil.isEmpty(exist)){
            log.error("要删除的服务器不存在，服务器id为：{}",id);
            throw new ResourceMissingException("服务器不存在",404);
        }
        // TODO:3.修改服务器状态
        exist.setStatus(Status.DISABLED);
        // TODO:4.更新服务器信息
        // 4.1.先写入数据库
        int result = serverMachineMapper.updateById(exist);
        if(result==0){
            log.error("服务器删除失败，服务器id为：{}",id);
            throw new BusinessException("服务器删除失败",400);
        }
        // 4.2.再更新缓存
        redisDataUtil.setWithPhysicalExpire(RedisConstant.CACHE_MACHINE_KEY+id,exist,RedisConstant.CACHE_MACHINE_TTL,TimeUnit.MINUTES);
        log.info("服务器删除成功，被删除的服务器id为：{}",id);
    }




    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateMachine(ServerMachine serverMachine) {
        log.info("更新服务器，更新对象为：{}",serverMachine);
        // TODO:1.判断用户是否合法
        User user = UserHolder.getUser();
        log.info("正在进行服务器更新操作的用户为：{}，用户id为：{}",user.getName(),user.getId());
        if(user.getRole()!= UserRole.ADMIN&&user.getStatus()!=Status.NORMAL){
            log.error("用户尝试越权更新服务器，用户名为：{}，用户id为：{}",user.getName(),user.getId());
            throw new AuthException("权限不足", 403);
        }
        // TODO:2.使用带解决缓存穿透查询，数据库使用悲观锁查询保持一致性
        ServerMachine exist = redisDataUtil.queryWithPassThrough(
                RedisConstant.CACHE_MACHINE_KEY,
                serverMachine.getId(),
                ServerMachine.class,
                RedisConstant.CACHE_MACHINE_TTL,
                TimeUnit.MINUTES,
                this::selectMachineByIdWithPessimisticLock);
        if(BeanUtil.isEmpty(exist)){
            log.error("要更新的服务器不存在，服务器id为：{}",serverMachine.getId());
            throw new ResourceMissingException("服务器不存在",404);
        }
        // TODO:3.验证更新服务器信息是否合理
        // 1.服务器新算力总额度不能为0
        if(serverMachine.getTotalComputePower()==0){
            log.error("更新的服务器算力总额度不能为0");
            throw new ParamValidationException("服务器算力总额度不能为0", 400);
        }
        // 2.已占用额度不能超过总额度
        if(serverMachine.getOccupiedComputePower()>serverMachine.getTotalComputePower()){
            log.error("更新的服务器已占用额度不能超过总额度");
            throw new ParamValidationException("服务器已占用额度不能超过总额度", 400);
        }
        // 3.服务器ip要符合规范
        if(     StrUtil.isNotBlank(serverMachine.getIp())
                &&!Validator.isIpv4(serverMachine.getIp())
                &&!Validator.isIpv6(serverMachine.getIp())){
            log.error("更新的服务器ip不符合规范");
            throw new ParamValidationException("服务器ip不符合规范", 400);
        }
        //6.服务器状态不能修改，要通过专门的api接口修改
        if(serverMachine.getStatus()!=exist.getStatus()){
            log.warn("服务器状态不能修改，要通过专门的api接口修改，用户{}尝试将服务器id：{}的状态修改为：{}",user.getName(),serverMachine.getId(),serverMachine.getStatus());
            serverMachine.setStatus(exist.getStatus());
        }
        // TODO:4.更新服务器信息
        // 4.1.先更新信息
        serverMachine.setUpdateTime(LocalDateTime.now());
        // 4.2.先写入数据库
        int result = serverMachineMapper.updateById(serverMachine);
        if(result==0){
            log.error("服务器更新失败，服务器id为：{}",serverMachine.getId());
            throw new BusinessException("服务器更新失败",400);
        }
        // 4.3.再更新缓存
        redisDataUtil.setWithPhysicalExpire(RedisConstant.CACHE_MACHINE_KEY+serverMachine.getId(),serverMachine,RedisConstant.CACHE_MACHINE_TTL,TimeUnit.MINUTES);
    }

    @Override
    public ServerMachine getMachineById(Long id) {
        log.info("查询服务器，服务器id为：{}",id);
        // TODO:1.使用解决缓存穿透查询，
        ServerMachine result = redisDataUtil.queryWithPassThrough(RedisConstant.CACHE_MACHINE_KEY,id,ServerMachine.class,RedisConstant.CACHE_MACHINE_TTL,TimeUnit.MINUTES,this::getById);
        // TODO:2.判断服务器是否存在
        if(BeanUtil.isEmpty(result)){
            log.warn("服务器不存在，服务器id为：{}",id);
            return null;
        }
        // TODO:3.返回结果
        log.info("查询到结果：{}",result.getServerName());
        return result;
    }

    @Override
    public List<ServerMachine> getMachineList(Integer page,
                                              Integer size,
                                              ServerMachine serverMachineTemplate) {
        // TODO:1. 分页参数安全处理（防止前端传 null）
        page = (page == null || page < 1) ? 1 : page;
        size = (size == null || size < 1) ? 10 : size;
        // 限制最大页大小，防止全表扫描
        size = Math.min(size, 100);
        // TODO:2. 构建分页对象
        Page<ServerMachine> pageParam = new Page<>(page, size);

        // TODO:3. 用模板对象构建查询条件（非空字段自动拼接）
        QueryWrapper<ServerMachine> wrapper = new QueryWrapper<>(serverMachineTemplate);

        // TODO:4. 直接查询数据库（不经过 Redis）
        IPage<ServerMachine> machinePage = serverMachineMapper.selectPage(pageParam, wrapper);

        // TODO:5. 判断查询结果是否为空
        if (machinePage.getRecords() == null || machinePage.getRecords().isEmpty()) {
            log.warn("机器列表查询无匹配数据，查询条件：{}", serverMachineTemplate);
        }
        // TODO:6. 返回结果
        return machinePage.getRecords();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateMachineStatus(Long id, Status status) {
        log.info("更新服务器状态，服务器id为：{}，状态为：{}",id,status);
        // TODO:1.判断用户是否合法
        User user = UserHolder.getUser();
        log.info("正在进行服务器状态更新操作的用户为：{}，用户id为：{}",user.getName(),user.getId());
        if(user.getRole()!= UserRole.ADMIN&&user.getStatus()!=Status.NORMAL){
            log.error("用户尝试越权更新服务器状态，用户名为：{}，用户id为：{}",user.getName(),user.getId());
            throw new AuthException("权限不足", 403);
        }
        // TODO:2.使用带解决缓存穿透查询，使用悲观锁查询保证一致性
        ServerMachine exist = redisDataUtil.queryWithPassThrough(
                RedisConstant.CACHE_MACHINE_KEY,
                id,
                ServerMachine.class,
                RedisConstant.CACHE_MACHINE_TTL,
                TimeUnit.MINUTES,
                this::selectMachineByIdWithPessimisticLock);
        // TODO:3.判断服务器是否存在
        if(BeanUtil.isEmpty(exist)){
            log.error("要更新状态的服务器不存在，服务器id为：{}",id);
            throw new ResourceMissingException("服务器不存在",404);
        }
        // TODO:4.设定服务器新状态
        exist.setStatus(status);
        exist.setUpdateTime(LocalDateTime.now());
        // TODO:5.更新服务器信息
        // 5.1.先写入数据库
        int result = serverMachineMapper.updateById(exist);
        if(result==0){
            log.error("服务器状态更新失败，服务器id为：{}",id);
            throw new BusinessException("服务器状态更新失败",400);
        }
        // 5.2.再写入缓存
        redisDataUtil.setWithPhysicalExpire(RedisConstant.CACHE_MACHINE_KEY+id,exist,RedisConstant.CACHE_MACHINE_TTL,TimeUnit.MINUTES);
    }
}
