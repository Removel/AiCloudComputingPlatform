package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.model.request.ChatRequest;
import com.removel.accp.model.response.ChatResponse;
import com.removel.accp.service.IChatService;
import com.removel.accp.util.SnowflakeIdGenerator;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
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
    ChatController(IChatService iChatService) {
        this.iChatService = iChatService;
    }

    @GetMapping("/session/{sessionId}")
    public Result<ChatResponse> getSessionHistory(@PathVariable Integer sessionId){
        log.info("用户{}获取对话{}全部内容", UserHolder.getUser().getName(),sessionId);
        List<Message> sessionHistory = iChatService.getSessionHistory(sessionId);
        ChatResponse response = new ChatResponse();
        response.setSessionHistory(sessionHistory);
        return Result.success(response);
    }

    @PostMapping("/session")
    public Result<ChatResponse> addSession(){
        log.info("用户{}创建对话",UserHolder.getUser().getName());
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setSessionId(iChatService.generateNewSessionId());
        log.info("用户{}创建对话成功，对话ID为{}",UserHolder.getUser().getName(),chatResponse.getSessionId());
        return Result.success(chatResponse);
    }

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest chatRequest){
        log.info("用户{}发起一次对话", UserHolder.getUser().getName());
        ChatResponse response = new ChatResponse();
        Message answer = iChatService.chat(chatRequest.getSessionId(),chatRequest.getPrompt());
        response.setAnswer(answer);
        log.info("用户{}发起一次对话成功", UserHolder.getUser().getName());
        return Result.success(response);
    }


}


