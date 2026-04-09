package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.SecKillCoupon;
import com.removel.accp.util.SeckKillOrderUtil.SeckillOrderMessage;

public interface ISecKillCouponService extends IService<SecKillCoupon> {
    void addSecKillCoupon(SecKillCoupon secKillCoupon);

    void buySecKillCoupon(Long seckillCouponId);

    void addToMessageQueue(SeckillOrderMessage seckillOrderMessage);

    boolean updateSecKillCouponSurplusInventory(Long seckillCouponId,int amount);
}
