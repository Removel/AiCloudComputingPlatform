package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.mapper.CouponOrderMapper;
import com.removel.accp.model.entity.CouponOrder;
import com.removel.accp.model.enums.Status;
import com.removel.accp.service.ICouponOrderService;
import jdk.jshell.Snippet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ICouponOrderServiceImpl extends ServiceImpl<CouponOrderMapper, CouponOrder> implements ICouponOrderService {

    private final CouponOrderMapper couponOrderMapper;
    @Autowired
    public ICouponOrderServiceImpl(CouponOrderMapper couponOrderMapper) {
        this.couponOrderMapper = couponOrderMapper;
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

    @Override
    public void createCouponOrder(Integer userId, Long couponId) {
        // TODO:1:创建对象实例化
        CouponOrder newCouponOrder = new CouponOrder();
        // TODO:2:设置属性
        newCouponOrder.setUserId(userId);
        newCouponOrder.setCouponId(couponId);
        newCouponOrder.setCreateTime(LocalDateTime.now());
        newCouponOrder.setStatus(1);
        newCouponOrder.setUpdateTime(LocalDateTime.now());
        // TODO:3:保存与验证
        int result =  couponOrderMapper.insert(newCouponOrder);
        if(result == 0){
            log.error("创建订单失败");
            throw new BusinessException("系统繁忙，创建订单失败",500);
        }
        log.info("创建订单成功");
    }
}
