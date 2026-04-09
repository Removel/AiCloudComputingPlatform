package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

//算力秒杀商品实体类
@Getter
@Setter
@TableName("server_rental_sec_kill_coupon")
@NoArgsConstructor
@AllArgsConstructor
public class SecKillCoupon extends RegularCoupon {
    /**
     * 服务器租赁商品表，代表一个租赁套餐，秒杀版本
     */
    // 秒杀总库存
    @TableField("inventory")
    private Integer inventory;

    // 剩余库存
    @TableField("surplus_inventory")
    private Integer surplusInventory;

    // 秒杀开始时间
    @TableField("start_time")
    private LocalDateTime startTime;

    // 秒杀结束时间
    @TableField("end_time")
    private LocalDateTime endTime;

    // 券过期时间（用户抢到后多久失效）
    @TableField("expire_time")
    private LocalDateTime expireTime;
}
