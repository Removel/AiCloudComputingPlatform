package com.removel.accp.model.response;

import com.removel.accp.model.entity.ChatMessage;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {

    //回答文本
    private String answer;

    //历史对话列表
    private List<ChatMessage> sessionHistory;
}
