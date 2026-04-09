package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.CouponOrderMapper;
import com.removel.accp.mapper.RegularCouponMapper;
import com.removel.accp.model.entity.RegularCoupon;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.response.CouponOrderResponse;
import com.removel.accp.service.ICouponOrderService;
import com.removel.accp.service.IRegularCouponService;
import com.removel.accp.service.IUserService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.MissingResourceException;

@Slf4j
@Service
public class IRegularCouponImpl extends ServiceImpl<RegularCouponMapper, RegularCoupon> implements IRegularCouponService {

    private final RegularCouponMapper regularCouponMapper;
    private final ICouponOrderService iCouponOrderService;
    private final IUserService iUserService;
    @Autowired
    public IRegularCouponImpl(RegularCouponMapper regularCouponMapper,
                              ICouponOrderService iCouponOrderService,
                              IUserService iUserService) {
        this.regularCouponMapper = regularCouponMapper;
        this.iCouponOrderService = iCouponOrderService;
        this.iUserService = iUserService;
    }

    @Transactional
    @Override
    public void buyRegularCoupon(Long couponId) {
        Integer userId = UserHolder.getUser().getId();
        log.info("用户{}购买普通优惠券，券id为{}",userId,couponId);
        // TODO:1:业务逻辑校验
        // 1.1:查看券是否存在
        RegularCoupon exist = regularCouponMapper.selectById(couponId);
        if (exist == null) {
            log.error("用户{}购买普通优惠券，券id为{}，券不存在",userId,couponId);
            throw new ResourceMissingException("券不存在",404);
        }
        // 1.2:查看券是否有效
        if(exist.getStatus()!= Status.NORMAL) {
            log.error("用户{}购买普通优惠券，券id为{}，券无效",userId,couponId);
            throw new ResourceMissingException("券无效",410);
        }
        // 1.3:查看用户是否有权限（状态合法）
        if(UserHolder.getUser().getStatus()!=Status.NORMAL) {
            log.error("用户{}购买普通优惠券，券id为{}，用户状态不合法",userId,couponId);
            throw new AuthException("用户状态不合法",403);
        }
        // TODO:2:尝试扣减用户余额
        iUserService.deductRemainingComputePower(userId,exist.getPrice());
        // TODO:3:更新当前用户余额
        UserHolder.getUser().setRemainingComputePower(UserHolder.getUser().getRemainingComputePower()-exist.getPrice());
        // TODO:4:尝试创建订单
        iCouponOrderService.createCouponOrder(userId,couponId,0);
        // TODO:5:返回结果
        log.info("购买成功");
    }


}
