package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.exception.AuthException;
import com.removel.accp.exception.BusinessException;
import com.removel.accp.exception.ResourceMissingException;
import com.removel.accp.mapper.ChatMapper;
import com.removel.accp.mapper.UserMapper;
import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.model.constant.RedissonConstant;
import com.removel.accp.model.entity.ChatMessage;
import com.removel.accp.model.entity.Session;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.enums.Status;
import com.removel.accp.service.IChatService;
import com.removel.accp.service.IChatSessionService;
import com.removel.accp.service.IUserService;
import com.removel.accp.util.AccurateTokenCounter;
import com.removel.accp.util.CacheClientUtil.RedisDataUtil;
import com.removel.accp.util.MessageConverter;
import com.removel.accp.util.SnowflakeIdGenerator;
import com.removel.accp.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IChatServiceImpl extends ServiceImpl<ChatMapper, ChatMessage> implements IChatService {


    private final ChatModel chatModel;
    private final IUserService iUserService;
    private final RedisDataUtil redisDataUtil;
    private final IChatSessionService iChatSessionService;
    private final RedissonClient redissonClient;

    public IChatServiceImpl(ChatModel chatModel,
                            IUserService iUserService,
                            RedisDataUtil redisDataUtil,
                            IChatService iChatService,
                            IChatSessionService iChatSessionService,
                            RedissonClient redissonClient) {
        this.chatModel = chatModel;
        this.iUserService = iUserService;
        this.redisDataUtil = redisDataUtil;
        this.iChatSessionService = iChatSessionService;
        this.redissonClient = redissonClient;
    }

    @Override
    public List<Message> getSessionHistory(Integer sessionId) {
        // TODO:1.验证合法性
        // 1.1.验证session是否存在
        log.info("用户{}查询历史对话，对话id为{}", UserHolder.getUser().getName(),sessionId);
        Session result = iChatSessionService.getById(sessionId);
        if (result == null) {
            log.error("查询历史对话未查询到相关对话sessionId信息");
            throw new ResourceMissingException("session不存在",404);
        }
        // TODO:2.使用sessionId在sql中查询session的chat历史（redis只存最近的数据）
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
    public Flux<String> chat(Long sessionId, String prompt) {
        log.info("用户{}发起一轮对话，对话id为{}", UserHolder.getUser().getName(), sessionId);
        // TODO:1.验证合法性
        // 1.1.验证session是否存在
        Session result = iChatSessionService.getById(sessionId);
        if (result == null) {
            log.error("发起对话未查询到相关对话sessionId信息");
            throw new ResourceMissingException("session不存在", 404);
        }
        // 1.2.验证prompt是否合法（这个先不管）

        // 1.3.验证用户状态
        if (UserHolder.getUser().getStatus() != Status.NORMAL) {
            log.error("用户状态异常，无法发起对话");
            throw new ResourceMissingException("用户状态异常，无法发起对话", 403);
        }
        // 1.4.是否匹配用户和会话（修正：不匹配时才抛异常）
        if (!result.getUserId().equals(UserHolder.getUser().getId())) {
            log.error("用户{}和会话{}不匹配", UserHolder.getUser().getName(), sessionId);
            throw new AuthException("用户和会话不匹配", 401);
        }

        // TODO:2.查询余额是否足够(保证基本满足一次最大token使用)
        // 注意这里我们应当从数据库中查库实现，UserHolder当中的user只是为了用于标识身份
        User user = iUserService.getById(UserHolder.getUser().getId());
        if (user.getRemainingComputePower() < 5) {
            log.error("用户余额不足，无法发起对话，余额为：{}", user.getRemainingComputePower());
            throw new ResourceMissingException("用户余额不足，无法发起对话", 402);
        }

        // TODO:3.从redis的list结构中读取最近的最多10条消息作为memory
        List<ChatMessage> chatMessageList = redisDataUtil.queryListWithPassThrough(
                RedisConstant.CHAT_MESSAGE_QUEUE_KEY,
                sessionId,
                ChatMessage.class,
                -10,
                -1,
                this::queryLastChatMessageBySessionId,
                RedisConstant.CHAT_MESSAGE_QUEUE_TTL,
                TimeUnit.MINUTES
        );
        if (chatMessageList.size() < 10) {
            log.warn("对话{}在redis中存储的对话历史不足10条", sessionId);
        }

        // TODO:4.将读取到的内容转化为Message类，并将当前问题prompt加入
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage("你是一个乐于助人的ai助手"));
        history.addAll(MessageConverter.toAiMessageList(chatMessageList));
        UserMessage promptMessage = new UserMessage(prompt);
        history.add(promptMessage);
        Prompt nowPrompt = new Prompt(history);

        // TODO:5.调用模型接口，获取结果（使用分布式锁保证同一会话串行）
        // 5.1 获取分布式锁
        String lockKey = RedissonConstant.LOCK_KEY + sessionId;
        RLock lock = redissonClient.getLock(lockKey);

        // 用于收集完整响应（用于后续保存，不影响流式输出）
        StringBuilder fullResponseBuilder = new StringBuilder();

        // 使用 Flux.usingWhen 管理锁的生命周期
        return Flux.usingWhen(
                // 获取锁资源（阻塞操作转为 Mono）
                Mono.fromCallable(() -> {
                    // 尝试获取锁，最多等待3秒，持有60秒（防止死锁）
                    boolean locked = lock.tryLock(3, 60, TimeUnit.SECONDS);
                    if (!locked) {
                        log.warn("获取锁失败，会话{}正在处理中", sessionId);
                        throw new BusinessException("当前对话正在处理中，请稍后再试", 429);
                    }
                    log.info("成功获取分布式锁，sessionId: {}", sessionId);
                    return lock;
                }),
                // 持有锁时执行业务逻辑（流式对话）
                lockResource -> {
                    return chatModel.stream(nowPrompt)
                            // 立即返回每个chunk给前端，同时收集完整响应
                            .map(response -> {
                                String chunk = response.getResult().getOutput().getText();
                                if (chunk != null) {
                                    fullResponseBuilder.append(chunk);
                                }
                                // TODO:9.返回结果
                                return chunk;
                            })
                            // 流正常结束后执行保存操作
                            .doOnComplete(() -> {
                                String fullAnswer = fullResponseBuilder.toString();
                                // TODO:7.保存结果到数据库
                                // TODO:8.将最新的内容追加到redis的list结构中
                                // 异步保存（不影响响应流）
                                saveToDbAndRedisAsync(sessionId, prompt, fullAnswer);
                            })
                            .doOnError(error -> {
                                log.error("流式对话异常，sessionId: {}", sessionId, error);
                            });
                },
                // 释放锁（无论正常结束还是异常）
                lockResource -> Mono.fromRunnable(() -> {
                    if (lockResource.isHeldByCurrentThread()) {
                        lockResource.unlock();
                        log.info("释放分布式锁，sessionId: {}", sessionId);
                    }
                })
        ).onErrorResume(throwable -> {
            // 统一处理异常，保证返回错误信息给前端
            if (throwable instanceof BusinessException) {
                return Flux.error(throwable);
            }
            log.error("对话未处理异常", throwable);
            return Flux.error(new BusinessException("对话失败，请重试", 500));
        });
    }

    public List<ChatMessage> queryLastChatMessageBySessionId(Long sessionId,Long start,Long end){
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id",sessionId)
                .orderByDesc("content_id")
                .last("limit "+start+","+end);
        return this.list(queryWrapper);
    }

    // 保存
    @Transactional
    public void saveToDbAndRedisAsync(Long sessionId, String prompt, String fullAnswer) {
        int inputTokenCount = AccurateTokenCounter.countTokens(prompt);
        int outputTokenCount = AccurateTokenCounter.countTokens(fullAnswer);
        try {
            // 扣减余额
            deductComputePower(sessionId, inputTokenCount, outputTokenCount);
            // 保存到数据库
            saveMessagesToDatabase(sessionId, prompt, fullAnswer);
            // 追加到Redis
            appendMessagesToRedis(sessionId, prompt, fullAnswer);
        } catch (Exception e) {
            log.error("保存对话失败", e);
            throw new BusinessException("保存对话失败",500);
            // 可以发送到消息队列，后续补偿
            // 这里就懒得用消息队列了，烂了就烂了吧~
        }
    }

    @Transactional
    public void deductComputePower(Long sessionId,int inputTokenCount,int outputTokenCount){
        Integer userId = UserHolder.getUser().getId();

        int sumTokenCount = inputTokenCount+outputTokenCount;

        // 1. 使用QueryWrapper和FOR UPDATE实现悲观锁查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", userId)
                .last("FOR UPDATE");

        // 2. 查询并锁定用户记录
        User user = iUserService.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("用户不存在", 404);
        }

        // 4. 使用LambdaUpdateWrapper更新用户计算能力
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getRemainingComputePower, user.getRemainingComputePower() -sumTokenCount)
                .set(User::getUpdateTime, LocalDateTime.now());
        boolean res = iUserService.update(updateWrapper);
        if (!res) {
            log.error("扣减计算能力失败，用户id:{}", userId);
            throw new BusinessException("扣减计算能力失败", 500);
        }
    }

    @Transactional
    public void saveMessagesToDatabase(Long sessionId, String prompt, String fullAnswer){
        // 创建userMessage
        ChatMessage newUserMessage = new ChatMessage();
        newUserMessage.setSessionId(sessionId);
        newUserMessage.setContent(prompt);
        newUserMessage.setRole("user");
        newUserMessage.setTimeStamp(LocalDateTime.now());
        newUserMessage.setUserId(UserHolder.getUser().getId());
        // 创建systemMessage
        ChatMessage newSystemMessage = new ChatMessage();
        newSystemMessage.setSessionId(sessionId);
        newSystemMessage.setContent(fullAnswer);
        newSystemMessage.setRole("assistant");
        newSystemMessage.setTimeStamp(LocalDateTime.now());
        newSystemMessage.setUserId(UserHolder.getUser().getId());
        // 保存两个message
        List<ChatMessage> messages = Arrays.asList(newUserMessage, newSystemMessage);   //先加入UserMessage再加入systemMessage
        this.saveBatch(messages);
    }

    @Transactional
    public void appendMessagesToRedis(Long sessionId, String prompt, String fullAnswer){
    redisDataUtil.RPushToList(RedisConstant.CHAT_MESSAGE_QUEUE_KEY,sessionId,prompt,RedisConstant.CHAT_MESSAGE_QUEUE_TTL,TimeUnit.HOURS);
    redisDataUtil.RPushToList(RedisConstant.CHAT_MESSAGE_QUEUE_KEY,sessionId,fullAnswer,RedisConstant.CHAT_MESSAGE_QUEUE_TTL,TimeUnit.HOURS);
    }

}
