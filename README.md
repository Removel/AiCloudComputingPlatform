# ACCP - AI Cloud Computing Platform

ACCP (AI Cloud Computing Platform) 是一个基于Spring Boot和AI技术构建的云计算平台，提供智能对话、服务器租赁、算力管理等功能。

## 项目简介

ACCP是一个集成了大语言模型(LLM)对话功能的云计算平台，用户可以通过平台与AI进行交互，同时管理自己的计算资源。平台支持用户注册登录、算力充值、服务器租赁、优惠券系统等功能，并采用Redis和MySQL进行数据存储，使用Redisson实现分布式锁，保证高并发场景下的数据一致性。

ps：剩下的比较简单的crud操作就懒得写了，基本思路类似于锁、夺标查询、工具类封装等似乎都差不多，这个文档是ai写的有一些问题。

## 技术栈

- **后端框架**: Spring Boot 4.0.3
- **数据库**: MySQL
- **ORM框架**: MyBatis-Plus 3.5.15
- **缓存**: Redis + Redisson 4.3.1
- **AI框架**: Spring AI (Ollama) 1.0.0-M6
- **认证授权**: uuid+redis缓存
- **消息队列**: redis Stream数据类型
- **工具库**: Hutool 5.8.40 等
- **构建工具**: Maven
- **JDK版本**: Java 21

## 项目结构

```
ACCP/
├── src/main/java/com/removel/accp/
│   ├── AccpApplication.java          # 应用程序入口
│   ├── annotation/                   # 自定义注解
│   ├── aspect/                       # 切面处理
│   ├── config/                       # 配置类
│   ├── controller/                   # 控制器层
│   ├── exception/                    # 异常处理
│   ├── filter/                       # 过滤器
│   ├── interceptor/                  # 拦截器
│   ├── mapper/                       # 数据访问层
│   ├── model/                        # 数据模型
│   │   ├── constant/                 # 常量定义
│   │   ├── entity/                   # 实体类
│   │   ├── enums/                    # 枚举类
│   │   ├── request/                  # 请求对象
│   │   └── response/                 # 响应对象
│   ├── service/                      # 服务层
│   │   └── impl/                     # 服务实现
│   └── util/                         # 工具类
├── src/main/resources/
│   ├── application.yml               # 主配置文件
│   ├── application-dev.yml           # 开发环境配置
│   ├── application-prod.yml          # 生产环境配置
│   ├── seckill.lua                   # 秒杀Lua脚本
│   ├── static/                       # 静态资源
│   └── templates/                    # 模板文件
└── CreateDatabase.sql                # 数据库初始化脚本
```

## 核心功能

### 1. 用户管理
- 用户注册与登录
- 用户信息管理
- 角色权限控制(管理员/普通用户)
- 用户状态管理(正常/停用/过期/锁定/注销)

### 2. AI对话服务
- 基于Ollama的LLM对话功能
- 对话历史管理
- 流式响应支持
- Token计数与算力消耗计算

### 3. 算力管理
- 用户算力余额管理
- 算力消耗记录
- 算力充值功能
- 算力使用统计

### 4. 服务器租赁
- 服务器信息管理
- 服务器规格描述
- 算力分配与占用
- 服务器状态监控

### 5. 优惠券系统
- 普通优惠券管理
- 秒杀优惠券系统
- 优惠券订单管理
- Redis+Lua脚本实现高并发秒杀

### 6. 分布式特性
- Redisson分布式锁
- Redis缓存管理
- 消息队列处理
- 异步任务执行

## 数据库设计

### 主要数据表

1. **user**: 用户表
   - 存储用户基本信息、角色、状态和算力余额

2. **server_machine**: 服务器机器表
   - 存储服务器信息、规格、算力分配和状态

3. **server_product**: 服务器租赁商品表
   - 存储服务器租赁套餐信息，支持秒杀场景

4. **llm_chat_message**: 对话消息表
   - 存储用户与AI的对话记录

5. **coupon_order**: 优惠券订单表
   - 存储优惠券购买订单信息

6. **regular_coupon**: 普通优惠券表
   - 存储普通优惠券信息

7. **seckill_coupon**: 秒杀优惠券表
   - 存储秒杀优惠券信息

## 快速开始

### 环境要求

- JDK 21
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Ollama服务(用于AI对话功能)

### 安装步骤

1. 克隆项目到本地
```bash
git clone [项目地址]
cd ACCP
```

2. 创建数据库并执行初始化脚本
```bash
mysql -u root -p < CreateDatabase.sql
```

3. 修改配置文件
- 编辑 `src/main/resources/application.yml`
- 配置数据库连接信息
- 配置Redis连接信息
- 配置邮件服务信息
- 配置Ollama服务地址

4. 编译并运行项目
```bash
mvn clean package
java -jar target/accp-0.0.1-SNAPSHOT.jar
```

或使用Maven直接运行
```bash
mvn spring-boot:run
```

5. 访问应用
- 默认端口: 8080
- API地址: http://localhost:8080/api

## API文档

### 用户认证相关
- POST /api/auth/register - 用户注册
- POST /api/auth/login - 用户登录
- POST /api/auth/logout - 用户登出
- POST /api/auth/refresh - 刷新Token

### AI对话相关
- POST /api/llm/chat - 发起AI对话
- GET /api/llm/session/{id} - 获取会话历史

### 用户管理相关
- GET /api/user/info - 获取用户信息
- PUT /api/user/info - 更新用户信息
- GET /api/user/compute-power - 获取用户算力
- POST /api/user/recharge - 算力充值

### 服务器租赁相关
- GET /api/server/list - 获取服务器列表
- GET /api/server/{id} - 获取服务器详情
- POST /api/server/rent - 租赁服务器

### 优惠券相关
- GET /api/coupon/regular/list - 获取普通优惠券列表
- GET /api/coupon/seckill/list - 获取秒杀优惠券列表
- POST /api/coupon/seckill/buy - 购买秒杀优惠券

## 配置说明

### 环境配置

项目支持多环境配置，通过Maven Profile切换：

- 开发环境: `mvn spring-boot:run -Pdev`
- 生产环境: `mvn spring-boot:run -Pprod`

### 主要配置项

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/accp
    username: root
    password: your_password

# Redis配置
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379

# Ollama配置
spring:
  ai:
    ollama:
      base-url: http://your-ollama-server:11434
      chat:
        model: gemma3:4b
        options:
          temperature: 0.7
          max-tokens: 2048
```

## 开发指南

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一异常处理
- 统一返回格式(Result对象)

### 分支管理

- main: 主分支，用于生产环境
- develop: 开发分支
- feature/*: 功能分支
- bugfix/*: 修复分支

### 提交规范

- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链相关


### 传统部署

```bash
# 打包
mvn clean package -Pprod

# 运行
java -jar target/accp-0.0.1-SNAPSHOT.jar
```

## 常见问题

1. **Ollama连接失败**
   - 检查Ollama服务是否正常运行
   - 确认配置文件中的base-url是否正确

2. **Redis连接失败**
   - 检查Redis服务是否启动
   - 确认Redis配置信息是否正确

3. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 确认数据库连接信息是否正确
   - 确认数据库是否已创建

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目。

## 许可证

本项目采用 [MIT 许可证](LICENSE)

## 联系方式

- 项目地址: https://github.com/Removel/AiCloudComputingPlatform.git
- 问题反馈: https://github.com/Removel/AiCloudComputingPlatform/issues
- 邮箱: accp_official@163.com

## 更新日志

### v0.0.1 (2024-04-12)
- 初始版本发布
- 实现用户认证功能
- 实现AI对话功能
- 实现服务器租赁功能
- 实现优惠券系统
