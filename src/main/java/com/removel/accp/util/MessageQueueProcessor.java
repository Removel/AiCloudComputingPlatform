package com.removel.accp.util;

import org.springframework.data.redis.connection.stream.MapRecord;

public interface MessageQueueProcessor {

    //初始化
    void init();
    //消费者组初始化
    void initConsumerGroup();
    //启动异步处理
    void startAsyncProcess();
    //处理订单消息
    void processOrderMessage(MapRecord<String, Object, Object> message);
    //处理失败消息
    void handleFailedMessage(MapRecord<String, Object, Object> message, Exception e);
    //关闭线程
    void destroy();
}
