package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("session")
public class Session {
    @TableField("session_id")
    private Long sessionId;
    @TableField("user_id")
    private Integer userId;
}
