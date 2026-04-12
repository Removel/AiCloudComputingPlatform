package com.removel.accp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.exception.ParamValidationException;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.SecKillCouponMapper;
import com.removel.accp.mapper.ServerMachineMapper;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.entity.SecKillCoupon;
import com.removel.accp.util.SeckKillOrderUtil.SeckillOrderMessage;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.enums.UserRole;
import com.removel.accp.service.ISecKillCouponService;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ISecKillCouponImpl extends ServiceImpl<SecKillCouponMapper, SecKillCoupon> implements ISecKillCouponService {

    private final RedisDataUtil redisDataUtil;
    private final SecKillCouponMapper secKillCouponMapper;
    private final ServerMachineMapper serverMachineMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public ISecKillCouponImpl(ServerMachineMapper serverMachineMapper,
                              RedisDataUtil redisDataUtil,
                              SecKillCouponMapper secKillCouponMapper,
                              StringRedisTemplate stringRedisTemplate) {
        this.redisDataUtil = redisDataUtil;
        this.secKillCouponMapper = secKillCouponMapper;
        this.serverMachineMapper = serverMachineMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    @Override
    public void addSecKillCoupon(SecKillCoupon secKillCoupon) {
        // TODO:1.验证用户身份
        User nowUser = UserHolder.getUser();
        if(nowUser.getRole()!= UserRole.ADMIN&&nowUser.getStatus()!= Status.NORMAL){
            log.info("用户权限不足，无法添加秒杀券，用户为：{}",nowUser.getName());
            throw new AuthException("权限不足",403);
        }
        // TODO:2.验证优惠券信息
        // 2.1:是否存在该设备
        ServerMachine exist = serverMachineMapper.selectById(secKillCoupon.getServerMachineId());
        if(exist==null){
            throw new ResourceMissingException("设备不存在",404);
        }
        // 2.2:验证设备状态是否正常
        if(exist.getStatus()!=Status.NORMAL){
            throw new BusinessException("设备状态异常",400);
        }
        // 2.3:验证商品名称
        if(StrUtil.isBlank(secKillCoupon.getProductName())){
            throw new ParamValidationException("商品名称不能为空",400);
        }
        // 2.4:id不能给定
        if(secKillCoupon.getId()!=null){
            throw new ParamValidationException("id不能给定",400);
        }
        //...太多了懒得写了
        // TODO:3.添加秒杀券信息
        // 这里并不需要立刻加入到redis中，只需要在快要开始的时候预热时加入即可
        // 只需要添加到数据库当中
        int res = secKillCouponMapper.insert(secKillCoupon);
        if(res!=1) {
            log.error("添加秒杀券失败");
            throw new BusinessException("添加失败", 400);
        }
        log.info("添加秒杀券成功，秒杀券信息为：{}",secKillCoupon);
    }

    //定义lua脚本成员
    private static final DefaultRedisScript<Long> CHECK_SECKILL_SCRIPT;
    //初始化成员属性
    static {
        CHECK_SECKILL_SCRIPT = new DefaultRedisScript<>();  //创建lua脚本对象
        CHECK_SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua")); //指定路径
        CHECK_SECKILL_SCRIPT.setResultType(Long.class);     //指定返回结果类型
    }

    @Override
    public void buySecKillCoupon(Long seckillCouponId) {
        // TODO:1.执行lua脚本验证库存、用户一人一单
        // 1.1:获取用户id
        Integer userId = UserHolder.getUser().getId();
        // 1.2:执行lua脚本
        Long result = stringRedisTemplate.execute(
                CHECK_SECKILL_SCRIPT,
                Collections.emptyList(),
                seckillCouponId.toString(),
                userId.toString()
        );
        // 1.3:判断lua脚本执行结果
        if(result==null){
            log.error("未知原因导致服务器使用lua脚本失败，请查看日志");
            throw new RuntimeException("秒杀失败");
        }
        else if(result==1L){
            log.error("秒杀失败，库存不足");
            throw new BusinessException("秒杀失败，库存不足",400);
        }
        else if(result==2L){
            log.error("秒杀失败，一人一单");
            throw new BusinessException("秒杀失败，一人只能一单",400);
        }
        // TODO:2.将order放进队列当中
        // 2.1:创建消息实体类
        SeckillOrderMessage seckillOrderMessage = SeckillOrderMessage.builder()
                .seckillCouponId(seckillCouponId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
        // 2.2:将消息放入队列
        addToMessageQueue(seckillOrderMessage);
    }

    @Override
    public void addToMessageQueue(SeckillOrderMessage seckillOrderMessage) {
        // TODO:1.将放入的消息类转化为要放入的消息
        Map<String,String> message = new HashMap<>();
        message.put("seckillCouponId",seckillOrderMessage.getSeckillCouponId().toString());
        message.put("userId",seckillOrderMessage.getUserId().toString());
        message.put("createTime",seckillOrderMessage.getCreateTime().toString());
        message.put("retryCount",seckillOrderMessage.getRetryCount().toString());
        // TODO:2.将消息放入队列
        stringRedisTemplate.opsForStream().add(
                RedisConstant.SECKILL_ORDER_QUEUE_KEY,  //队列名称
                message     // 放入队列的消息
        );
    }

    @Transactional
    @Override
    public boolean updateSecKillCouponSurplusInventory(Long seckillCouponId,int amount) {
        //带乐观锁
        LambdaUpdateWrapper<SecKillCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SecKillCoupon::getId, seckillCouponId)
                .gt(SecKillCoupon::getSurplusInventory, 0)
                .setSql("surplus_inventory = surplus_inventory - " + amount);
        return this.update(wrapper);
    }
}

