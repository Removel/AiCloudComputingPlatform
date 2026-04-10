package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.model.request.ChatRequest;
import com.removel.accp.model.response.ChatResponse;
import com.removel.accp.service.IChatService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 算了我们使用spring_ai吧，不要手搓了

@RestController
@RequestMapping("/api/llm")
@Slf4j
public class ChatController {

    // 构造器注入
    private final IChatService iChatService;
    @Autowired
    ChatController(IChatService iChatService){
        this.iChatService = iChatService;
    }

    @GetMapping("/{sessionId}")
    public Result<ChatResponse> getSessionHistory(@PathVariable Integer sessionId){
        log.info("用户{}获取对话{}全部内容", UserHolder.getUser().getName(),sessionId);
        List<ChatMessage> sessionHistory = iChatService.getSessionHistory(sessionId);
        ChatResponse response = new ChatResponse();
        response.setSessionHistory(sessionHistory);
        return Result.success(response);
    }

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest chatRequest){
        log.info("用户{}发起对话", UserHolder.getUser().getName());
        ChatResponse response = new ChatResponse();
        String answer = iChatService.chat(chatRequest.getModel(),chatRequest.getSessionId(),chatRequest.getPrompt());
        response.setAnswer(answer);
        return Result.success(response);
    }


}


