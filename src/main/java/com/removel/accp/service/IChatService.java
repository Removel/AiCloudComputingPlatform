package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.ChatMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface IChatService extends IService<ChatMessage> {

    List<Message> getSessionHistory(Integer sessionId);

    Message chat(Long sessionId ,String prompt);

    Long generateNewSessionId();
}
