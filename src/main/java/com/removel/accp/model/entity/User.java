package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 用户实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
public class User {
    /**
     * 用户表，代表一个用户
     */
    //主键id，自增
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;
    //用户名
    @TableField("name")
    private String name;
    //密码
    @TableField("password")
    private String password;
    //邮箱
    @TableField("email")
    private String email;
    //角色
    @TableField("role")
    private UserRole role;
    //剩余算力
    @TableField("remaining_compute_power")
    private Integer remainingComputePower;
    //创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
    //更新时间
    @TableField("update_time")
    private LocalDateTime updateTime;
    //状态
    @TableField("status")
    private Status status;
}
