package com.removel.accp.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("llm_chat_message")
public class ChatMessage {

    // 标记这个信息归属的用户
    @TableField("user_id")
    private Integer userId;

    // 标记这个信息归属的对话id
    @TableField("session_id")
    private Long sessionId;

    // 这个信息的内容
    @TableField("content")
    private String content;

    // 这个信息在这轮对话中的序号
    @TableField("content_id")
    @TableId(type = IdType.AUTO)
    private Integer contentId;

    // 这个信息在这轮对话中的角色:user/system/assistant
    @TableField("role")
    private String role;

    // 时间戳
    @TableField("timestamp")
    private LocalDateTime timeStamp;

}
