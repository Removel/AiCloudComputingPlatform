package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.CouponOrder;

import java.util.List;

public interface ICouponOrderService extends IService<CouponOrder> {
    //暴露给用户的查询方法
    List<CouponOrder> list(int userId);

    //内部调用的创建方法
    void createCouponOrder(Integer userId,Long couponId,int type);

}
