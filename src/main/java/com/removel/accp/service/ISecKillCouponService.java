package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.SecKillCoupon;

public interface ISecKillCouponService extends IService<SecKillCoupon> {
    void addSecKillCoupon(SecKillCoupon secKillCoupon);
}
