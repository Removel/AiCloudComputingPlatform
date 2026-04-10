package com.removel.accp.model.request;

import lombok.Data;

@Data
public class ChatRequest {

    private Long sessionId;  // 会话ID，用来管理上下文
    private String prompt;     // 用户本次提问内容

}
