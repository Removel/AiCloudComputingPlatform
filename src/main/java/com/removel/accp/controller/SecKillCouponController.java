package com.removel.accp.controller;

import com.removel.accp.annotation.LogOperation;
import com.removel.accp.mapper.SecKillCouponMapper;
import com.removel.accp.model.Result;
import com.removel.accp.model.request.SecKillCouponRequest;
import com.removel.accp.service.ISecKillCouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/seckill")
public class SecKillCouponController {

    private final ISecKillCouponService iSecKillCouponService;
    @Autowired
    public SecKillCouponController(ISecKillCouponService iSecKillCouponService){
        this.iSecKillCouponService = iSecKillCouponService;
    }

    @LogOperation
    @PostMapping()
    public Result<?> addSecKillCoupon(SecKillCouponRequest secKillCouponRequest){
        log.info("添加秒杀优惠券");
        iSecKillCouponService.addSecKillCoupon(secKillCouponRequest.getNewSeckillCoupon());
        log.info("添加秒杀优惠券成功");
        return Result.success();
    }

    @PostMapping("/buy")
    public Result<?> buySecKillCoupon(@RequestBody SecKillCouponRequest secKillCouponRequest){
        iSecKillCouponService.buySecKillCoupon(secKillCouponRequest.getSeckillCouponId());
        return Result.success();
    }



}
