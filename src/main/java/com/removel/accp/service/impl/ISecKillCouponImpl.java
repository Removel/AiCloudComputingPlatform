package com.removel.accp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.exception.ParamValidationException;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.SecKillCouponMapper;
import com.removel.accp.mapper.ServerMachineMapper;
import com.removel.accp.model.entity.SecKillCoupon;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.enums.UserRole;
import com.removel.accp.service.ISecKillCouponService;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ISecKillCouponImpl extends ServiceImpl<SecKillCouponMapper, SecKillCoupon> implements ISecKillCouponService {

    private final RedisDataUtil redisDataUtil;
    private final SecKillCouponMapper secKillCouponMapper;
    private final ServerMachineMapper serverMachineMapper;

    public ISecKillCouponImpl(ServerMachineMapper serverMachineMapper,RedisDataUtil redisDataUtil, SecKillCouponMapper secKillCouponMapper) {
        this.redisDataUtil = redisDataUtil;
        this.secKillCouponMapper = secKillCouponMapper;
        this.serverMachineMapper = serverMachineMapper;
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
}

