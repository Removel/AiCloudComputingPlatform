package com.removel.accp.model.response;

import com.removel.accp.model.entity.ChatMessage;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Data
public class ChatResponse {

    //回答文本
    private Message answer;

    //历史对话列表
    private List<Message> sessionHistory;

    //对话id（当前/新建）
    private Long sessionId;
}
