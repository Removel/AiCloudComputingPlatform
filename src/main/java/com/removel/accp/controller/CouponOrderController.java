package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.entity.CouponOrder;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.response.CouponOrderResponse;
import com.removel.accp.service.ICouponOrderService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupon_order")
@Slf4j
public class CouponOrderController {

    private final ICouponOrderService iCouponOrderService;
    @Autowired
    public CouponOrderController(ICouponOrderService iCouponOrderService) {
        this.iCouponOrderService = iCouponOrderService;
    }

    @GetMapping("/list")
    public Result<CouponOrderResponse> list(){
        User user = UserHolder.getUser();
        log.info("用户：{}（id为：{}）查询其订单列表", user.getName(), user.getId());
        List<CouponOrder> result = iCouponOrderService.list(user.getId());
        CouponOrderResponse couponOrderResponse = new CouponOrderResponse();
        couponOrderResponse.setCouponOrderList(result);
        log.info("用户：{}（id为：{}）查询其订单列表成功，订单列表为：{}", user.getName(), user.getId(), couponOrderResponse);
        return Result.success(couponOrderResponse);
    }
}
