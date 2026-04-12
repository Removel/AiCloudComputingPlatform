package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.SessionMapper;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.model.entity.Session;
import com.removel.accp.service.IChatService;
import com.removel.accp.service.IChatSessionService;
import com.removel.accp.util.MessageConverter;
import com.removel.accp.util.SnowflakeIdGenerator;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class IChatSessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements IChatSessionService {

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final IChatService iChatService;
    @Autowired
    public IChatSessionServiceImpl(SnowflakeIdGenerator snowflakeIdGenerator,IChatService iChatService) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.iChatService = iChatService;
    }
    @Override
    public Long generateNewSessionId() {
        Session newSession = new Session();
        newSession.setUserId(UserHolder.getUser().getId());
        newSession.setSessionId(snowflakeIdGenerator.nextId());
        save(newSession);
        return newSession.getSessionId();
    }
}
