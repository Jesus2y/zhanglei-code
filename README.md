# 跨境物流成本计算器

## 项目简介
一个基于 Spring Boot 3.2 + 微信小程序的跨境物流成本计算系统，支持空运/海运费用计算、会员订阅、限流机制等功能。

## 技术栈

### 后端
- **Spring Boot 3.2.0** - Web框架
- **Java 17** - 编程语言
- **MySQL 8.0** - 关系型数据库
- **Spring Data JPA** - ORM框架
- **Lombok** - 代码简化工具

### 前端
- **微信小程序** - 原生小程序开发

## 主要功能

1. **物流费用计算**
   - 支持美国、德国、日本三个国家
   - 支持空运和海运两种方式
   - 自动计算体积重量和计费重量
   - 包含燃油附加费、清关费、仓储费等明细

2. **用户系统**
   - 微信一键登录
   - 会员订阅（9.9元/月）
   - 普通用户每日免费3次计算
   - 会员不限次数

3. **历史记录**
   - 自动保存计算历史
   - 分页查询历史记录
   - 支持查看历史详情

4. **限流机制**
   - 基于用户的每日限流
   - 防止滥用和恶意请求

## 项目结构

```
demo/
├── logistics-calculator-backend/      # 后端项目
│   ├── src/main/java/com/logistics/calculator/
│   │   ├── controller/                # 控制器层
│   │   ├── service/                   # 服务层
│   │   ├── repository/                # 数据访问层
│   │   ├── model/                     # 实体类
│   │   └── exception/                 # 异常处理
│   ├── src/main/resources/
│   │   └── application.yml            # 配置文件
│   └── pom.xml                        # Maven配置
└── logistics-calculator-miniprogram/  # 小程序前端
    ├── pages/                         # 页面目录
    ├── app.js                         # 小程序入口
    └── app.json                       # 小程序配置
```

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+
- 微信开发者工具

### 后端启动

1. **创建数据库**
```bash
mysql -u root -p
# 执行 init_database.sql 脚本创建数据库和表
```

2. **修改配置**
编辑 `application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logistics_calculator
    username: your_username
    password: your_password
```

3. **启动应用**
```bash
cd logistics-calculator-backend
mvn spring-boot:run
```

### 前端启动

1. 打开微信开发者工具
2. 导入 `logistics-calculator-miniprogram` 目录
3. 修改 `app.js` 中的 `apiBaseUrl` 为后端地址
4. 编译运行

## 数据库设计

### 核心表
- **users** - 用户信息表
- **calculation_history** - 计算历史记录表
- **payment_records** - 支付记录表

详细建表脚本请参考 `init_database.sql`

## API 接口

### 用户相关
- `POST /api/v1/user/login` - 用户登录
- `GET /api/v1/user/profile` - 获取用户信息
- `GET /api/v1/user/history` - 获取计算历史

### 计算相关
- `POST /api/v1/calculate` - 执行物流计算
- `GET /api/v1/rules/{country}` - 获取计费规则

### 支付相关
- `POST /api/v1/payment/create` - 创建订单
- `POST /api/v1/payment/callback` - 支付回调

## 注意事项

1. **Redis已移除**：项目已简化架构，移除了Redis依赖，限流功能完全基于MySQL实现
2. **数据库自动建表**：开发环境下 `ddl-auto: update` 会自动创建表结构
3. **生产环境建议**：将 `ddl-auto` 改为 `validate`，手动管理数据库变更

## 许可证
MIT License
