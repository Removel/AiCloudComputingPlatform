package com.removel.accp.model.request;

import com.removel.accp.model.entity.RegularCoupon;
import lombok.Data;

@Data
public class RegularCouponRequest {

    // 添加平常券类信息
    private RegularCoupon newRegularCoupon;

    // 购买平常券类使用的id
    private Long toBuyRegularCouponId;
}
