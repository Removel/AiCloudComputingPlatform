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

// 服务器租赁商品实体类
@TableName("server_machine")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerMachine {
    /**
     * 服务器商铺表，代表一台物理服务器
     */
    // 主键ID
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    // 服务器名称/标识
    @TableField("server_name")
    private String serverName;
    @TableField("specification")
    // 服务器规格描述（如：NVIDIA A100 80GB），使用json格式记录
    private String specification;
    // 总算力额度（单位：算力点）
    @TableField("total_compute_power")
    private Integer totalComputePower;
    // 已占用算力额度
    @TableField("occupied_compute_power")
    private Integer occupiedComputePower;
    // 服务器状态状态
    @TableField("status")
    private Status status;
    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
    // 更新时间
    @TableField("update_time")
    private LocalDateTime updateTime;


}
