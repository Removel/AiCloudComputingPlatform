
# ACCP 核心技术点问答

> 本文档深入剖析 AiCloudComputingPlatform（ACCP）项目中的核心技术实现，以问题-回答形式呈现。

---

## 目录

- [一、项目架构与整体设计](#一项目架构与整体设计)
- [二、AI 对话与流式输出](#二ai-对话与流式输出)
- [三、认证与鉴权体系](#三认证与鉴权体系)
- [四、缓存策略与一致性](#四缓存策略与一致性)
- [五、秒杀系统设计](#五秒杀系统设计)
- [六、并发控制与分布式锁](#六并发控制与分布式锁)
- [七、AOP 日志与全局异常处理](#七aop-日志与全局异常处理)
- [八、用户权限与敏感字段保护](#八用户权限与敏感字段保护)
- [九、数据库设计与会话管理](#九数据库设计与会话管理)

---

## 一、项目架构与整体设计

### Q1: ACCP 项目的整体架构是什么？

**A:** ACCP（AI Cloud Computing Platform）是一个基于 **Spring Boot 4.0.3 + Java 21** 构建的 AI 云计算平台后端服务。项目采用经典的三层架构：

- **Controller 层**：负责 HTTP 请求路由与参数接收（ChatController、UserController、SecKillCouponController 等）
- **Service 层**：核心业务逻辑实现（IChatServiceImpl、IUserServiceImpl、ISecKillCouponImpl 等）
- **Mapper 层**：基于 MyBatis-Plus 的数据持久化访问

技术栈包括：Spring AI + Ollama（大模型集成）、Redis + Redisson（缓存与分布式锁）、MySQL（持久化）、Spring AOP（切面日志）、JWT（令牌工具）、Hutool（工具库）。

### Q2: 项目为什么选择 Spring AI 而不是手搓 HTTP 客户端调用大模型？

**A:** 从 ChatController 中的注释 `// 算了我们使用spring_ai吧，不要手搓了` 可以看出，项目最初考虑过手搓 HTTP 调用，但最终选择 Spring AI 的原因包括：

1. **标准化抽象**：Spring AI 提供了统一的 `ChatModel` 接口，屏蔽了不同模型厂商的 API 差异
2. **流式支持**：内置 `chatModel.stream()` 方法，原生支持 Reactor 的 `Flux<String>` 流式输出
3. **消息体系**：提供 `SystemMessage`、`UserMessage`、`AssistantMessage` 等标准消息类型，方便构建对话上下文
4. **Prompt 管理**：`Prompt` 类封装了消息列表，简化了多轮对话的上下文组装

---

## 二、AI 对话与流式输出

### Q3: 项目如何实现 AI 对话的流式输出？

**A:** 在 `IChatServiceImpl.chat()` 方法中，采用了 **Reactor 响应式流** 实现流式输出：

```java
return chatModel.stream(nowPrompt)
    .map(response -> {
        String chunk = response.getResult().getOutput().getText();
        if (chunk != null) {
            fullResponseBuilder.append(chunk);  // 收集完整响应
        }
        return chunk;  // 逐块返回给前端
    })
    .doOnComplete(() -> {
        String fullAnswer = fullResponseBuilder.toString();
        saveToDbAndRedisAsync(sessionId, prompt, fullAnswer);  // 流结束后异步保存
    });
```

核心思路：
1. `chatModel.stream()` 返回 `Flux<ChatResponse>`，每个 chunk 包含一小段文本
2. 使用 `map` 操作逐块提取文本并立即返回给前端，同时用 `StringBuilder` 收集完整响应
3. `doOnComplete` 在流正常结束后触发异步保存操作（写入数据库 + 追加 Redis）
4. 整个过程用 `Flux.usingWhen` 管理分布式锁的生命周期

### Q4: 流式对话中如何保证同一会话的串行处理？

**A:** 使用 **Redisson 分布式锁 + Flux.usingWhen** 实现响应式流中的锁管理：

```java
return Flux.usingWhen(
    // 1. 获取锁：尝试获取，最多等待3秒，持有60秒（防死锁）
    Mono.fromCallable(() -> {
        boolean locked = lock.tryLock(3, 60, TimeUnit.SECONDS);
        if (!locked) throw new BusinessException("当前对话正在处理中，请稍后再试", 429);
        return lock;
    }),
    // 2. 持有锁时执行流式对话
    lockResource -> chatModel.stream(nowPrompt)...,
    // 3. 释放锁（无论正常结束还是异常）
    lockResource -> Mono.fromRunnable(() -> {
        if (lockResource.isHeldByCurrentThread()) lockResource.unlock();
    })
);
```

**关键设计**：
- 锁粒度为 `sessionId`，不同会话可并行，同一会话串行
- `tryLock(3, 60, SECONDS)`：等待3秒获取锁，持有60秒自动释放防死锁
- `isHeldByCurrentThread()` 判断避免释放他人锁
- `Flux.usingWhen` 确保锁在流结束（无论成功或异常）后一定释放

### Q5: AI 对话的上下文记忆如何实现？

**A:** 采用 **Redis List + MySQL 双层存储** 的混合记忆策略：

1. **短期记忆（Redis）**：从 Redis List 中读取最近 10 条消息作为上下文
   ```java
   List<ChatMessage> chatMessageList = redisDataUtil.queryListWithPassThrough(
       RedisConstant.CHAT_MESSAGE_QUEUE_KEY, sessionId, ChatMessage.class,
       -10, -1,  // 取最后10条
       this::queryLastChatMessageBySessionId, ...);
   ```

2. **长期记忆（MySQL）**：完整的对话历史存储在数据库的 `chat_message` 表中

3. **上下文组装**：SystemMessage + Redis 中的历史消息 + 当前用户提问
   ```java
   history.add(new SystemMessage("你是一个乐于助人的ai助手"));
   history.addAll(MessageConverter.toAiMessageList(chatMessageList));
   history.add(new UserMessage(prompt));
   ```

4. **写入策略**：对话完成后，用户提问和AI回答同时追加到 Redis List 和 MySQL

### Q6: Token 计费是如何实现的？

**A:** 在 `saveToDbAndRedisAsync()` 方法中，对话完成后进行 Token 计费：

1. **Token 计数**：使用 `AccurateTokenCounter.countTokens()` 分别计算输入和输出的 Token 数
2. **余额扣减**：使用 **悲观锁（SELECT ... FOR UPDATE）** 锁定用户行，保证并发安全
   ```java
   QueryWrapper<User> queryWrapper = new QueryWrapper<>();
   queryWrapper.eq("id", userId).last("FOR UPDATE");
   User user = iUserService.getOne(queryWrapper);
   // 计算并扣减
   updateWrapper.set(User::getRemainingComputePower, user.getRemainingComputePower() - sumTokenCount);
   ```
3. **事务保障**：扣减余额、保存消息、追加 Redis 三步在同一个 `@Transactional` 方法中

---

## 三、认证与鉴权体系

### Q7: 项目的认证流程是怎样的？

**A:** 采用 **Filter + Interceptor 双层认证架构**：

**第一层：TokenFilter（Servlet Filter）**
- 拦截所有 `/*` 请求
- 对 `/login` 和 `/register` 路径直接放行
- 检查请求头 `Authorization` 是否为空，为空则抛出异常
- 仅做最基本的 Token 存在性校验

**第二层：LoginInterceptor（Spring MVC Interceptor）**
- 从 Redis 中根据 Token 查询用户信息（`LOGIN_CODE_KEY + token`）
- 校验用户状态是否为 `NORMAL`
- 将用户信息存入 `UserHolder`（ThreadLocal），供后续业务使用
- 在 `afterCompletion` 中清除 ThreadLocal，防止内存泄漏

**第三层：RefreshTokenInterceptor**
- 在请求处理完成后刷新 Token 的 Redis 过期时间
- 实现用户活跃期间 Token 不过期的效果

### Q8: 为什么需要 TokenFilter 和 LoginInterceptor 两层校验？

**A:** 这是由于 **Filter 和 Interceptor 的作用域不同**：

- **TokenFilter** 属于 Servlet 规范，在请求进入 Spring MVC 之前执行，适合做粗粒度的过滤（Token 是否存在）
- **LoginInterceptor** 属于 Spring MVC，可以访问 Spring 容器，能从 Redis 查询用户信息并注入到上下文
- **RefreshTokenInterceptor** 在 LoginInterceptor 之后执行（order=2），确保用户已认证后才刷新 Token

这种分层设计实现了 **关注点分离**：Filter 负责请求过滤，Interceptor 负责身份验证和 Token 续期。

### Q9: UserHolder 是如何实现线程安全的用户信息传递的？

**A:** `UserHolder` 基于 **ThreadLocal** 实现：

```java
public class UserHolder {
    private static final ThreadLocal<User> userHolder = new ThreadLocal<User>();
    public static User getUser() { return userHolder.get(); }
    public static void setUser(User user) { userHolder.set(user); }
    public static void removeUser() { userHolder.remove(); }
}
```

- **set**：在 LoginInterceptor 的 `preHandle` 中设置当前线程的用户信息
- **get**：在 Service 层任意位置获取当前登录用户
- **remove**：在 LoginInterceptor 的 `afterCompletion` 中清除，防止线程池复用导致的内存泄漏

**注意事项**：在异步场景下（如 `@Async`），ThreadLocal 无法传递到子线程，需要手动传递用户信息。

### Q10: Token 的存储和刷新机制是什么？

**A:** Token 采用 **Redis 存储 + 物理过期 + 自动续期** 机制：

1. **登录时**：生成 UUID 作为 Token，用户信息（脱敏后）存入 Redis，设置物理过期时间
   ```java
   String token = UUID.randomUUID().toString();
   redisDataUtil.setWithPhysicalExpire(LOGIN_CODE_KEY + token, user, LOGIN_CODE_TTL, TimeUnit.DAYS);
   ```

2. **每次请求时**：RefreshTokenInterceptor 重新设置 Redis 的 Key-Value 和过期时间，实现活跃用户永不过期
   ```java
   stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + token, userJson, LOGIN_CODE_TTL, TimeUnit.DAYS);
   ```

3. **登出时**：直接从 Redis 删除对应的 Key

---

## 四、缓存策略与一致性

### Q11: 项目中使用了哪些缓存策略来解决常见问题？

**A:** `RedisDataUtil` 封装了三种核心缓存策略：

| 策略 | 方法 | 解决的问题 | 实现方式 |
|------|------|-----------|---------|
| 缓存穿透 | `queryWithPassThrough` | 查询不存在的数据 | 缓存空值 + 短TTL |
| 缓存击穿（逻辑过期） | `queryWithLogicalExpire` | 热点Key过期瞬间大量请求 | 逻辑过期 + 互斥锁重建 + 异步线程池 |
| 缓存击穿（互斥锁） | `queryWithMutex` | 热点Key过期瞬间大量请求 | Redis SETNX 互斥锁 + 递归重试 |

### Q12: 物理过期和逻辑过期有什么区别？各自适用什么场景？

**A:**

**物理过期（setWithPhysicalExpire）**：
- 直接使用 Redis 的 TTL 机制，到期后 Redis 自动删除 Key
- 优点：实现简单，Redis 自动清理
- 缺点：Key 过期瞬间会有缓存击穿风险
- 适用场景：普通数据，如用户登录信息、服务器信息缓存

**逻辑过期（setWithLogicalExpire）**：
- 不设置 Redis TTL，在 Value 中嵌入过期时间字段
- 读取时检查逻辑过期时间，过期则异步重建缓存
- 优点：永远不会有缓存击穿问题，用户始终能读到数据（可能是旧数据）
- 缺点：有短暂的数据不一致窗口；如果不主动删除，数据永不过期
- 适用场景：热点数据，允许短暂不一致，如秒杀商品信息

### Q13: 缓存与数据库的一致性如何保证？

**A:** 项目采用 **Cache-Aside（旁路缓存）+ 先更新数据库再更新缓存** 的策略：

以 `IServerMachineServiceImpl` 为例：
```java
// 1. 先写入数据库
int result = serverMachineMapper.updateById(exist);
// 2. 再更新缓存
redisDataUtil.setWithPhysicalExpire(CACHE_MACHINE_KEY + id, exist, CACHE_MACHINE_TTL, TimeUnit.MINUTES);
```

**额外保障**：
- 查询时使用 `queryWithPassThrough`，缓存未命中时查库并回填
- 更新/删除操作时主动更新缓存，而非等待缓存自然过期
- 关键查询使用悲观锁（`selectByIdForUpdate`）保证数据库层面的一致性

---

## 五、秒杀系统设计

### Q14: 秒杀系统的整体架构是怎样的？

**A:** 采用 **Lua 脚本原子校验 + Redis Stream 消息队列 + 异步下单** 的三段式架构：

```
用户请求 → Lua脚本(库存校验+一人一单) → Redis Stream消息队列 → 消费者异步创建订单
```

1. **前置校验（Lua 脚本）**：在 Redis 中原子性地检查库存和一人一单
2. **消息入队**：校验通过后，将订单消息放入 Redis Stream
3. **异步消费**：消费者从 Stream 读取消息，执行数据库操作（扣减库存、创建订单）

### Q15: 为什么使用 Lua 脚本而不是 Java 代码来做秒杀校验？

**A:** Lua 脚本在 Redis 中是 **原子执行** 的，不会被其他命令打断。如果用 Java 代码：

```java
// 非原子操作，存在并发问题
String stock = redis.get("stock:" + couponId);  // 步骤1：查库存
if (Integer.parseInt(stock) <= 0) return;        // 步骤2：判断
redis.decr("stock:" + couponId);                 // 步骤3：扣库存
```

在步骤1和步骤3之间，其他线程可能已经修改了库存，导致超卖。Lua 脚本将检查和扣减合并为一个原子操作，彻底避免并发问题。

### Q16: 秒杀中如何实现一人一单？

**A:** 在 Lua 脚本中，使用 Redis Set 记录已购买用户：

- Key 设计：`seckill:order:{couponId}` → 存储已购买该优惠券的用户 ID 集合
- Lua 脚本逻辑：
  1. 检查 `seckill:stock:{couponId}` 库存是否 > 0
  2. 检查 `seckill:order:{couponId}` 中是否已包含当前用户 ID
  3. 两项检查都通过 → 扣减库存 + 添加用户 ID 到集合
  4. 任一检查失败 → 返回对应错误码（1=库存不足，2=重复下单）

### Q17: 秒杀库存扣减如何防止超卖？

**A:** 采用 **双重保障**：

1. **Redis 层（Lua 脚本）**：原子性检查并扣减 Redis 中的库存计数器
2. **数据库层（乐观锁）**：
   ```java
   LambdaUpdateWrapper<SecKillCoupon> wrapper = new LambdaUpdateWrapper<>();
   wrapper.eq(SecKillCoupon::getId, seckillCouponId)
       .gt(SecKillCoupon::getSurplusInventory, 0)  // 乐观锁条件
       .setSql("surplus_inventory = surplus_inventory - " + amount);
   ```
   `gt(SurplusInventory, 0)` 确保库存大于0时才扣减，利用数据库的行锁保证最终一致性。

### Q18: 为什么使用 Redis Stream 而不是普通的消息队列？

**A:** Redis Stream 相比 List/Queue 的优势：

1. **消费者组**：支持多消费者组，实现发布/订阅模式
2. **消息确认（ACK）**：消费者处理完消息后确认，未确认的消息可被重新消费
3. **消息持久化**：消息持久化在 Redis 中，消费者宕机后不丢失
4. **消息回溯**：可以按时间或ID回溯历史消息
5. **轻量级**：无需额外部署 RabbitMQ/Kafka，Redis 即可满足需求

项目中的使用：
```java
stringRedisTemplate.opsForStream().add(
    RedisConstant.SECKILL_ORDER_QUEUE_KEY, message);
```

---

## 六、并发控制与分布式锁

### Q19: 项目中使用了哪些锁机制？各自适用于什么场景？

**A:**

| 锁类型 | 实现方式 | 适用场景 | 示例 |
|--------|---------|---------|------|
| Redisson 分布式锁 | `RLock lock = redissonClient.getLock(key)` | 跨 JVM 的并发控制 | AI 对话同一会话串行 |
| Redis SETNX 锁 | `setIfAbsent(key, "1", 10, SECONDS)` | 缓存重建互斥 | 缓存击穿时的互斥锁 |
| 数据库悲观锁 | `SELECT ... FOR UPDATE` | 余额扣减等强一致性场景 | 用户计算力扣减 |
| 数据库乐观锁 | `gt(SurplusInventory, 0)` | 秒杀库存扣减 | 秒杀券库存扣减 |

### Q20: 为什么 AI 对话用 Redisson 分布式锁而余额扣减用数据库悲观锁？

**A:**

- **AI 对话**：涉及流式响应（长时间持有锁），且需要跨多个服务实例协调，Redisson 分布式锁支持看门狗自动续期，适合长耗时操作
- **余额扣减**：操作在数据库事务内完成，`SELECT ... FOR UPDATE` 直接锁住数据行，避免引入额外的锁组件，同时保证事务内的数据一致性

选择原则：
- 需要跨服务/长耗时 → Redisson 分布式锁
- 仅在单事务内/短操作 → 数据库悲观锁
- 高并发读/低并发写 → 数据库乐观锁

---

## 七、AOP 日志与全局异常处理

### Q21: @LogOperation 注解 + LogAspect 是如何实现方法级日志的？

**A:** 使用 **Spring AOP 的 @Around 环绕通知**：

1. 自定义注解 `@LogOperation` 标记需要记录日志的方法
2. `LogAspect` 通过 `@Around("@annotation(...)")` 拦截所有标注方法
3. 记录：目标类名、方法名、参数、执行耗时、返回结果

```java
@Around("@annotation(com.removel.accp.annotation.LogOperation)")
public Object LogAround(ProceedingJoinPoint pjp) throws Throwable {
    Long startTime = System.currentTimeMillis();
    Object result = pjp.proceed();
    Long endTime = System.currentTimeMillis();
    log.info("该请求使用的时间为：{}ms", endTime - startTime);
    return result;
}
```

**优点**：对业务代码零侵入，通过注解灵活控制日志粒度。

### Q22: 全局异常处理器如何统一错误响应？

**A:** 使用 `@RestControllerAdvice` + `@ExceptionHandler` 实现统一异常处理：

| 异常类型 | HTTP 状态码含义 | 触发场景 |
|---------|---------------|---------|
| `AuthException` | 401/403 | 认证失败、权限不足 |
| `ParamValidationException` | 400 | 参数校验失败 |
| `BusinessException` | 400/500 | 业务逻辑异常 |
| `ResourceMissingException` | 404 | 资源不存在 |
| `Exception` | 500 | 兜底：未知系统异常 |

所有异常统一返回 `Result<T>` 格式，前端只需处理一种响应结构。

---

## 八、用户权限与敏感字段保护

### Q23: 用户更新信息时如何保护敏感字段？

**A:** 在 `IUserServiceImpl.updateUser()` 中实现了 **细粒度的权限控制**：

1. **修改他人**：必须是状态正常的管理员（`isAdminNormal`）
2. **修改自己**：
   - 普通用户：禁止修改 `role`、`status`、`createTime`、`remainingComputePower`，尝试修改则强制恢复原值
   - 状态异常的管理员：同样禁止修改敏感字段
   - 状态正常的管理员：可以修改自己的所有字段

```java
// 非管理员尝试修改敏感字段 → 强制恢复原值
if (!isAdmin) {
    user.setRole(currentUser.getRole());
    user.setStatus(currentUser.getStatus());
    user.setCreateTime(currentUser.getCreateTime());
    user.setRemainingComputePower(currentUser.getRemainingComputePower());
}
```

### Q24: 用户删除为什么是逻辑删除而非物理删除？

**A:** 将用户状态改为 `CANCELLED` 而非从数据库删除：

```java
updateWrapper.eq(User::getId, id).set(User::getStatus, Status.CANCELLED);
```

原因：
1. **数据完整性**：用户关联的对话记录、订单等需要保留
2. **审计追溯**：可追溯历史操作记录
3. **恢复能力**：误删可通过修改状态恢复
4. **合规要求**：数据保留期限内的法律要求

---

## 九、数据库设计与会话管理

### Q25: 数据库的表结构设计是怎样的？

**A:** 从 `CreateDatabase.sql` 和 Mapper 推断，核心表包括：

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `user` | 用户信息 | id, username, email, password, role, status, remaining_compute_power |
| `session` | 对话会话 | id, user_id, title, status |
| `chat_message` | 对话消息 | content_id, session_id, user_id, role(user/assistant), content, timestamp |
| `server_machine` | 服务器设备 | id, server_name, ip, spec, total_compute_power, occupied_compute_power, status |
| `sec_kill_coupon` | 秒杀优惠券 | id, product_name, surplus_inventory, server_machine_id |
| `coupon_order` | 优惠券订单 | id, user_id, coupon_id, status |
| `regular_coupon` | 常规优惠券 | id, product_name, total_inventory, server_machine_id |

### Q26: 会话（Session）与消息（ChatMessage）的关系是什么？

**A:** 一对多关系：

- 一个 `Session` 属于一个用户，包含多条 `ChatMessage`
- `ChatMessage` 通过 `session_id` 关联到 `Session`
- `ChatMessage.role` 区分 `user`（用户提问）和 `assistant`（AI回答）
- `ChatMessage.content_id` 作为排序字段，保证消息的时间顺序

对话流程：
1. 用户创建 Session → 获取 sessionId
2. 每次对话：用户提问（role=user）+ AI回答（role=assistant）成对保存
3. 查询历史：按 `content_id` 升序排列，还原完整对话

### Q27: 雪花算法 ID 生成器的作用是什么？

**A:** `SnowflakeIdGenerator` 用于生成分布式环境下的全局唯一 ID：

- 趋势递增：有利于 MySQL B+ 树索引的插入性能
- 无需依赖数据库自增：适合分库分表场景
- 时间有序：ID 本身包含时间信息，可按 ID 排序得到时间顺序

---

## 总结

ACCP 项目在技术选型上体现了以下核心设计思想：

1. **响应式编程**：AI 对话使用 Reactor Flux 实现流式输出，提升用户体验
2. **分层防护**：Filter → Interceptor → Service 层层校验，保证安全性
3. **缓存三板斧**：穿透（空值缓存）、击穿（互斥锁/逻辑过期）、一致性（Cache-Aside）
4. **秒杀三段式**：Lua 原子校验 → 消息队列 → 异步下单，削峰填谷
5. **锁的精细化选择**：分布式锁/悲观锁/乐观锁按场景选用
6. **零侵入增强**：AOP 注解实现日志，全局异常处理器统一响应
