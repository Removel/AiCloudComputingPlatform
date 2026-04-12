package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.response.MyChatResponse;
import com.removel.accp.service.IChatService;
import com.removel.accp.service.IChatSessionService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
@Slf4j
public class ChatSessionController {

    private final IChatService iChatService;
    private final IChatSessionService iChatSessionService;
    @Autowired
    public ChatSessionController(IChatService iChatService,
                                 IChatSessionService iChatSessionService){
        this.iChatService = iChatService;
        this.iChatSessionService = iChatSessionService;
    }

    @GetMapping("/{sessionId}")
    public Result<MyChatResponse> getSessionHistory(@PathVariable Integer sessionId){
        log.info("用户{}获取对话{}全部内容", UserHolder.getUser().getName(),sessionId);
        List<Message> sessionHistory = iChatService.getSessionHistory(sessionId);
        MyChatResponse response = new MyChatResponse();
        response.setSessionHistory(sessionHistory);
        return Result.success(response);
    }

    @PostMapping
    public Result<MyChatResponse> addSession(){
        log.info("用户{}创建对话",UserHolder.getUser().getName());
        MyChatResponse myChatResponse = new MyChatResponse();
        myChatResponse.setSessionId(iChatSessionService.generateNewSessionId());
        log.info("用户{}创建对话成功，对话ID为{}",UserHolder.getUser().getName(), myChatResponse.getSessionId());
        return Result.success(myChatResponse);
    }

}
