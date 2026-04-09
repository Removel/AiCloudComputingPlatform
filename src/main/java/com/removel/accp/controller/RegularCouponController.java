package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.RegularCouponRequest;
import com.removel.accp.service.IRegularCouponService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/regular")
public class RegularCouponController {

    //这些只是平常购买，不是秒杀，当作秒杀前的基础练手了

    private final IRegularCouponService iRegularCouponService;
    @Autowired
    public RegularCouponController(IRegularCouponService regularCouponService) {
        this.iRegularCouponService = regularCouponService;
    }

    //购买普通优惠券
    @PostMapping("/buy")
    public Result<?> buyRegularCoupon(@RequestBody RegularCouponRequest regularCouponRequest){
        log.info("用户{}尝试购买id为{}的优惠券",UserHolder.getUser().getId(),regularCouponRequest.getToBuyRegularCouponId());
        iRegularCouponService.buyRegularCoupon(regularCouponRequest.getToBuyRegularCouponId());
        log.info("购买普通优惠券成功");
        return Result.success();
    }

    //购买多个普通优惠券

    //使用普通优惠券（即核销）

    //获取普通优惠券列表（多个）

    //获取普通优惠券详情（单个）

    //对购买的优惠券退货（单个）

}
