package com.removel.accp.util.SeckKillOrderUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 秒杀订单消息实体类
// 不会暴露到前端，用于传递消息队列的信息
public class SeckillOrderMessage {
    // 秒杀券id
    private Long seckillCouponId;
    // 用户id
    private Integer userId;
    // 创建时间
    private LocalDateTime createTime;
    // 重试次数
    private Integer retryCount;
}
