package com.removel.accp.util.SeckKillOrderUtil;

// 工具类
// 用于异步处理redis的stream的消息队列当中的订单
// 或许会不同与黑马点评，这里更相信使用ai的内容

import com.removel.accp.model.constant.RedisConstant;
import com.removel.accp.service.ICouponOrderService;
import com.removel.accp.service.ISecKillCouponService;
import com.removel.accp.util.MessageQueueProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SeckillOrderProcessor implements MessageQueueProcessor {

    // 构造器注入初始化
    private final StringRedisTemplate stringRedisTemplate;  //redisTemplate注入
    private final ISecKillCouponService iSecKillCouponService;  //秒杀优惠券服务注入
    private final ICouponOrderService iCouponOrderService;  //优惠券订单服务注入
    @Autowired
    public SeckillOrderProcessor(StringRedisTemplate stringRedisTemplate,
                                 ISecKillCouponService iSecKillCouponService,
                                 ICouponOrderService iCouponOrderService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.iSecKillCouponService = iSecKillCouponService;
        this.iCouponOrderService = iCouponOrderService;
    }

    @Override
    @PostConstruct
    public void init() {
        initConsumerGroup();    //初始化消费者组
        startAsyncProcess();    //异步启动消费者线程
    }

    // 初始化消费者组
    @Override
    public void initConsumerGroup(){
        try{
            // 创建消费者组
            stringRedisTemplate.opsForStream().createGroup(
                    RedisConstant.SECKILL_ORDER_QUEUE_KEY,
                    RedisConstant.CONSUMER_GROUP
            );
        }catch (Exception e){
            log.error("初始化消费者组已存在或者创建失败，原因：{}", e.getMessage());
        }
    }

    // 异步启动消费者线程
    @Override
    public void startAsyncProcess(){
        // 创建一个新的消费者线程，用于异步处理秒杀订单消息
        Thread consumerThread = new Thread(() -> {
            // 持续运行循环，只要系统处于运行状态
            while (RedisConstant.RUNNING){
                try{
                    // TODO:1.将从消息队列当中获取的信息封装成列表用于下一个步骤处理
                    // 从Stream当中获取信息（这里设定为最多读取5条，阻塞时间为2秒）
                    // 从Redis Stream中读取消息
                    // 参数说明：
                    // - Consumer.from: 指定消费者组和消费者名称
                    // - StreamReadOptions: 配置读取选项，每次最多读取5条消息，阻塞2秒等待新消息
                    // - StreamOffset: 从上次消费的位置开始读取
                    List<MapRecord<String,Object,Object>> messages = stringRedisTemplate.opsForStream().read(
                            Consumer.from(RedisConstant.CONSUMER_GROUP,RedisConstant.CONSUMER_NAME),
                            StreamReadOptions.empty().count(5).block(Duration.ofSeconds(2)),
                            StreamOffset.create(RedisConstant.SECKILL_ORDER_QUEUE_KEY, ReadOffset.lastConsumed())
                    );
                    // TODO:2.处理获取到的信息列表
                    // for循环处理上面得到的信息列表messages
                    // 遍历获取到的消息列表，逐条处理
                    for(MapRecord<String,Object,Object> message : messages){
                        try{
                            // TODO:2.1.处理订单消息
                            //处理订单消息
                            // 调用业务方法处理订单消息
                            processOrderMessage(message);
                            // TODO:2.2.确认订单已经处理，将订单信息从队列的pendingList中确认并去除
                            //确认订单已经处理，将订单信息从队列的pendingList中确认并去除
                            // 消息处理成功后，向Redis发送ACK确认
                            // 这会将消息从pending list（待处理列表）中移除，表示消息已被成功处理
                            stringRedisTemplate.opsForStream()
                                    .acknowledge(
                                            RedisConstant.SECKILL_ORDER_QUEUE_KEY,
                                            RedisConstant.CONSUMER_GROUP,
                                            message.getId()
                                    );
                        }catch (Exception e) {
                            // TODO:2.3.处理处理失败的队列消息
                            // 记录处理失败的错误日志
                            log.error("处理订单失败: {}", e.getMessage(), e);
                            // 处理失败的消息，可以记录到失败队列或重试
                            // 调用失败处理方法，将消息转移到失败队列或进行重试处理
                            // TODO:2.4.尝试处理失败的队列消息
                            handleFailedMessage(message, e);
                        }
                    }
                }catch (Exception e ){
                    // TODO:3.捕获读取队列的异常并输出日志
                    // TODO:3.1。捕获消费过程中的异常，记录错误日志
                    log.error("消费消息异常: {}", e.getMessage(), e);
                    // TODO：3.2.发生异常时休眠1秒，避免频繁重试导致系统压力过大
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException ie){
                        // TODO:3.3.如果线程被中断，恢复中断状态并退出循环
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        // TODO:4.启动消费者线程
        // TODO:4.1.设置消费者线程的名称
        consumerThread.setName("seckill-order-consumer");
        // TODO:4.2.设置为守护线程
        consumerThread.setDaemon(true);
        // TODO:4.3.启动消费者线程（注意：此处需要添加start()调用）
        consumerThread.start();
    }

    // 处理订单消息
    @Override
    public void processOrderMessage(MapRecord<String, Object, Object> message){
        // TODO:1.从消息中获取订单信息
        Map<Object, Object> value = message.getValue();
        Long seckillCouponId = Long.valueOf(value.get("seckillCouponId").toString());
        Integer userId = Integer.valueOf(value.get("userId").toString());
        // TODO:2.调用业务方法处理订单
        // TODO:2.1.创建订单并存储到coupon_order表当中，该方法会在一个事务当中更新秒杀优惠券的剩余库存
        log.info("开始处理订单: userId={}, seckillCouponId={}", userId, seckillCouponId);
        iCouponOrderService.createCouponOrder(userId,seckillCouponId,1);

        log.info("订单处理成功");
    }

    // 获取消息的重试次数
    private int getRetryCount(MapRecord<String, Object, Object> message) {
        Object retryObj = message.getValue().get("retryCount");
        if (retryObj != null) {
            try {
                return Integer.parseInt(retryObj.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    // 处理处理失败的队列消息
    // 这里我们使用重试2次+加入死信队列的方法
    public void handleFailedMessage(MapRecord<String, Object, Object> message, Exception e) {
        int retryCount = getRetryCount(message);

        if (retryCount < 2) {
            // 重试：构造新消息（retryCount+1）
            Map<String, String> newMessage = new HashMap<>();
            //newMessage.putAll((Map) message.getValue());
            //这样子似乎会安全一点？
            newMessage.put("seckillCouponId", message.getValue().get("seckillCouponId").toString());
            newMessage.put("userId", message.getValue().get("userId").toString());
            newMessage.put("retryCount", String.valueOf(retryCount + 1));
            newMessage.put("createTime",message.getValue().get("createTime").toString());
            stringRedisTemplate.opsForStream().add(
                    RedisConstant.SECKILL_ORDER_QUEUE_KEY,
                    newMessage
            );
            log.warn("消息重试中... retry={}, originalId={}", retryCount + 1, message.getId());
        } else {
            // 超过重试次数：进入死信队列
            String deadInfo = String.format("originalId=%s, data=%s, error=%s",
                    message.getId(), message.getValue(), e.getMessage());
            stringRedisTemplate.opsForList().leftPush(
                    RedisConstant.SECKILL_ORDER_DEAD_QUEUE_KEY,
                    deadInfo
            );
            log.error("消息彻底失败，已进入死信队列: id={}", message.getId());
        }

        // 无论重试还是死信，原消息都必须 ACK，否则会永远留在 Pending 列表
        stringRedisTemplate.opsForStream().acknowledge(
                RedisConstant.SECKILL_ORDER_QUEUE_KEY,
                RedisConstant.CONSUMER_GROUP,
                message.getId()
        );
    }

    // 关闭线程
    @PreDestroy
    @Override
    public void destroy(){
        RedisConstant.RUNNING = false;
        log.info("秒杀订单消费者线程已停止");
    }
}
