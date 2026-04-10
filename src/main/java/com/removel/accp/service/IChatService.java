package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.ChatMessage;

import java.util.List;

public interface IChatService extends IService<ChatMessage> {

    List<ChatMessage> getSessionHistory(Integer sessionId);

    String chat(String model,Integer sessionId ,String prompt);

}
