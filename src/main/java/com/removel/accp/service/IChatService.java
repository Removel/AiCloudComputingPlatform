package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.ChatMessage;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface IChatService extends IService<ChatMessage> {
    List<Message> getSessionHistory(Integer sessionId);

    Flux<String> chat(Long sessionId , String prompt);

}
