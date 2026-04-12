package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.Session;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface IChatSessionService extends IService<Session> {
    Long generateNewSessionId();

}
