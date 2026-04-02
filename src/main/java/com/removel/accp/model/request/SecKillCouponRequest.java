package com.removel.accp.model.request;

import com.removel.accp.model.entity.SecKillCoupon;
import lombok.Data;

@Data
public class SecKillCouponRequest {
    // 添加秒杀券类信息
    private SecKillCoupon newSeckillCoupon;
}
