package com.removel.accp.model.response;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

@Data
public class MyChatResponse {

    //回答文本
    private Flux<String> answerText;

    //历史对话列表
    private List<Message> sessionHistory;

    //对话id（当前/新建）
    private Long sessionId;
}
