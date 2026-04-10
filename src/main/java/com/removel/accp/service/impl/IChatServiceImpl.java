package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.ChatMapper;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.service.IChatService;
import com.removel.accp.util.MessageConverter;
import com.removel.accp.util.SnowflakeIdGenerator;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.MissingResourceException;

@Service
@Slf4j
public class IChatServiceImpl extends ServiceImpl<ChatMapper, ChatMessage> implements IChatService {

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ChatModel chatModel;
    public IChatServiceImpl(ChatModel chatModel,SnowflakeIdGenerator snowflakeIdGenerator) {
        this.chatModel = chatModel;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<Message> getSessionHistory(Integer sessionId) {
        // TODO:1.验证合法性
        // 1.1.验证session是否存在
        log.info("用户{}查询历史对话，对话id为{}", UserHolder.getUser().getName(),sessionId);
        ChatMessage result = this.getById(sessionId);
        if (result == null) {
            log.error("查询历史对话未查询到相关对话sessionId信息");
            throw new ResourceMissingException("session不存在",404);
        }
        // TODO:2.使用sessionId在sql中查询session历史（redis只存最近的数据）
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("session_id",sessionId)
                .orderByAsc("content_id");
        List<ChatMessage> chatMessageList = this.list(queryWrapper);
        // TODO:3.将结果转换为ai的message对象
        List<Message> resultList = MessageConverter.toAiMessageList(chatMessageList);
        log.info("查询成功");
        // TODO:4.返回结果
        return resultList;
    }

    @Override
    public Long generateNewSessionId() {
        return snowflakeIdGenerator.nextId();
    }

    @Transactional
    @Override
    public Message chat(Long sessionId, String prompt) {
        log.info("用户{}发起一轮对话，对话id为{}", UserHolder.getUser().getName(),sessionId);
        // TODO:1.验证合法性
        // 1.1.验证session是否存在
        ChatMessage result = this.getById(sessionId);
        if (result == null) {
            log.error("发起对话未查询到相关对话sessionId信息");
            throw new ResourceMissingException("session不存在",404);
        }
        // 1.2.验证prompt是否合法（这个先不管）
        // TODO:2.查询余额是否足够(保证基本满足一次最大token使用)

        // TODO:3.从redis的list结构中读取最近的最多10条消息作为memory

        // TODO:4.调用模型接口，获取结果

        // TODO:5.根据结果token数量，通过计算，扣减余额，使用悲观锁

        // TODO:6.保存结果到数据库

        // TODO:7.将最新的内容追加到redis的list结构中

        // TODO:6.返回结果
        return null;
    }
}
