package com.removel.accp.model.request;

import com.removel.accp.model.entity.SecKillCoupon;
import lombok.Data;

@Data
public class SecKillCouponRequest {
    // 添加秒杀券类信息
    private SecKillCoupon newSeckillCoupon;

    // 购买类信息：券id和用户id
    private Long seckillCouponId;
    private Integer userId;

}
