package com.removel.accp.util;

import com.removel.accp.model.entity.ChatMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

//消息转换器
public class MessageConverter {

    //ChatMessage-》AiMessage 单个转换
    public static Message toAiMessage(ChatMessage chatMessage) {
        String role = chatMessage.getRole();
        String content = chatMessage.getContent();

        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> throw new RuntimeException("未知角色：" + role);
        };
    }

    //ChatMessage-》AiMessage 批量转换
    public static List<Message> toAiMessageList(List<ChatMessage> chatMessages) {
        return chatMessages.stream()
                .map(MessageConverter::toAiMessage)
                .collect(Collectors.toList());
    }

    //AiMessage-》ChatMessage 单个转换
    public static ChatMessage toChatMessage(Message message, Integer userId, Long sessionId, Integer contentId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setContent(message.getText());
        chatMessage.setContentId(contentId);
        chatMessage.setRole(message.getMessageType().name().toLowerCase()); // user/assistant/system
        chatMessage.setTimeStamp(LocalDateTime.now());
        return chatMessage;
    }

    //AiMessage-》ChatMessage 批量转换
    public static List<ChatMessage> toChatMessageList(List<Message> messages, Integer userId, Long sessionId) {
        return messages.stream()
                .map(message -> toChatMessage(message, userId, sessionId, messages.indexOf(message) + 1))
                .collect(Collectors.toList());
    }




}
