package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 订单实体类
@TableName("coupon_order")
@Data
public class CouponOrder {
        /**
         * 主键
         */
        @TableId(value = "id", type = IdType.ASSIGN_ID)
        private Long id;

        /**
         * 下单的用户id
         */
        private Integer userId;

        /**
         * 购买的代金券id
         */
        private Long couponId;

        /**
         * 支付方式 1：余额支付；2：支付宝；3：微信
         */
        private Integer payType;

        /**
        * 购买的代金券种类:0-普通，1-秒杀
        */
        private Integer couponType;

        /**
         * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：已退款
         */
        private Integer status;

        /**
         * 下单时间
         */
        private LocalDateTime createTime;

        /**
         * 支付时间
         */
        private LocalDateTime payTime;

        /**
         * 核销时间
         */
        private LocalDateTime useTime;

        /**
         * 退款时间
         */
        private LocalDateTime refundTime;

        /**
         * 更新时间
         */
        private LocalDateTime updateTime;
    }
