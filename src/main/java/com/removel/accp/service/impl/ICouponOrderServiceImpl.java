package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.mapper.CouponOrderMapper;
import com.removel.accp.model.entity.CouponOrder;
import com.removel.accp.model.entity.SecKillCoupon;
import com.removel.accp.model.enums.Status;
import com.removel.accp.service.ICouponOrderService;
import com.removel.accp.service.ISecKillCouponService;
import jdk.jshell.Snippet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ICouponOrderServiceImpl extends ServiceImpl<CouponOrderMapper, CouponOrder> implements ICouponOrderService {

    private final CouponOrderMapper couponOrderMapper;
    private final ISecKillCouponService iSecKillCouponService;
    @Autowired
    public ICouponOrderServiceImpl(CouponOrderMapper couponOrderMapper,
                                   ISecKillCouponService iSecKillCouponService) {
        this.couponOrderMapper = couponOrderMapper;
        this.iSecKillCouponService = iSecKillCouponService;
    }

    @Override
    public List<CouponOrder> list(int userId) {
        log.info("正在查询");
        QueryWrapper<CouponOrder> listQuery = new QueryWrapper<>();
        listQuery.eq("user_id", userId);
        List<CouponOrder> result = couponOrderMapper.selectList(listQuery);
        if(result.isEmpty()){
            log.warn("查询结果为空");
        }
        log.info("查询结束");
        return result;
    }

    @Transactional
    @Override
    public void createCouponOrder(Integer userId, Long couponId,int type) {
        // TODO:1.幂等校验（这是什么？）
        LambdaQueryWrapper<CouponOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponOrder::getUserId, userId)
                .eq(CouponOrder::getCouponId, couponId); // 注意字段名
        long count = couponOrderMapper.selectCount(wrapper);
        if (count > 0) {
            log.info("订单已存在，幂等跳过: userId={}, couponId={}", userId, couponId);
            return; // 或直接返回，不抛异常
        }
        // TODO:2.扣减数据库库存
        boolean res = iSecKillCouponService.updateSecKillCouponSurplusInventory(couponId,1);
        if(!res){
            log.error("扣减库存失败,数据库库存不足");
            throw new BusinessException("数据库库存不足",500);
        }
        // TODO:3:创建订单
        // 3.1.对象实例化
        CouponOrder newCouponOrder = new CouponOrder();
        // 3.2.设置属性
        newCouponOrder.setUserId(userId);
        newCouponOrder.setCouponId(couponId);
        newCouponOrder.setCreateTime(LocalDateTime.now());
        newCouponOrder.setStatus(1);
        newCouponOrder.setUpdateTime(LocalDateTime.now());
        newCouponOrder.setCouponType(type);
        // 3.3.保存与验证
        int result =  couponOrderMapper.insert(newCouponOrder);
        if(result == 0){
            log.error("创建订单失败");
            throw new BusinessException("系统繁忙，创建订单失败",500);
        }
        log.info("创建订单成功");
    }
}
