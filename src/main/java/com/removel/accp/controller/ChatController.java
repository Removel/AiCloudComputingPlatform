package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.request.MyChatRequest;
import com.removel.accp.model.response.MyChatResponse;
import com.removel.accp.service.IChatService;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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


    @PostMapping("/chat")
    public Result<MyChatResponse> chat(@RequestBody MyChatRequest myChatRequest){
        log.info("用户{}发起一次对话", UserHolder.getUser().getName());
        MyChatResponse response = new MyChatResponse();
        Flux<String> answer = iChatService.chat(myChatRequest.getSessionId(), myChatRequest.getPrompt());
        response.setAnswerText(answer);
        log.info("用户{}发起一次对话成功", UserHolder.getUser().getName());
        return Result.success(response);
    }

}


