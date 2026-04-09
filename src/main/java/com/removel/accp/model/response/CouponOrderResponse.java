package com.removel.accp.model.response;

import com.removel.accp.model.entity.CouponOrder;
import lombok.Data;

import java.util.List;

@Data
public class CouponOrderResponse {
    //用于返回查询优惠券订单列表结果
    private List<CouponOrder> couponOrderList;
}
