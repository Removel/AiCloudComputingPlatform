package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.removel.accp.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//算力秒杀商品实体类
@TableName("compute_power_sec_kill")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServerRentalFlashCoupon {
    /**
     * 服务器租赁商品表，代表一个租赁套餐，秒杀版本
     */
    // 主键ID
    @TableId(value = "id",type = IdType.ASSIGN_ID)
    private Long id;
    // 所属服务器ID（商铺ID）
    @TableField("machine_id")
    private Long machineId;
    // 商品名称（如：基础GPU服务器1天）
    @TableField("product_name")
    private String productName;
    // 租赁时长（单位：小时）
    @TableField("rental_hours")
    private Integer rentalHours;
    // 消耗算力（每小时/整个套餐消耗的算力点数）
    @TableField("consume_compute_power")
    private Integer computePower;
    // 商品价格（单位：分）
    @TableField("price")
    private Integer price;
    // 适用场景描述
    @TableField("scene_desc")
    private String sceneDesc;
    // 商品状态
    @TableField("status")
    private Status status;
    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
    // 更新时间
    @TableField("update_time")
    private LocalDateTime updateTime;
    // 过期时间
    @TableField("expire_time")
    private LocalDateTime expireTime;
    // 库存
    @TableField("inventory")
    private Integer inventory;
}
