package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.mapper.ChatMapper;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.service.IChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class IChatServiceImpl extends ServiceImpl<ChatMapper, ChatMessage> implements IChatService {

    String OLLAMA_URL = "http://100.70.8.11/11434/api/chat";

    @Override
    public List<ChatMessage> getSessionHistory(Integer sessionId) {
        // TODO:1.验证合法性
        // 1.1.验证session是否存在

        // TODO:2.使用sessionId查询session历史

        // TODO:3.返回结果
        return List.of();
    }

    @Transactional
    @Override
    public String chat(String model, Integer sessionId, String prompt) {
        // TODO:1.验证合法性
        // 1.1.验证model是否存在
        // 1.2.验证session是否存在
        // 1.3.验证prompt是否合法（这个先不管）

        // TODO:2.查询余额是否足够(保证基本满足一次最大token使用)

        // TODO:3.调用模型接口，获取结果

        // TODO:4.根据结果token数量，通过计算，扣减余额，使用悲观锁

        // TODO:5.保存结果到数据库

        // TODO:6.返回结果
        return "";
    }
}
