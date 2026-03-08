# 分布式秒杀商城系统

> 分布式软件工程课程实验项目 —— 基于 Spring Boot 的高并发限时抢购系统

---

## 一、项目概述

本项目是一个面向高并发场景的限时抢购（秒杀）商城系统，旨在通过实际开发实践，深入理解分布式系统中的**库存一致性**、**防重复下单**、**缓存预热**等核心问题。系统采用 Redis 缓存 + 数据库乐观锁的双层防护策略，确保在高并发请求下库存数据的准确性和订单的唯一性。

---

## 二、系统架构设计

### 2.1 系统架构草图

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端（浏览器）                           │
│                   login.html  /  index.html                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP RESTful API
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Nginx / 网关层（可扩展）                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
  ┌────────────────┐ ┌──────────────┐ ┌──────────────────┐
  │  用户服务       │ │  商品服务     │ │  订单/库存服务     │
  │ AccountCtrl    │ │ ProductCtrl  │ │ FlashSaleCtrl    │
  │ AccountService │ │ ProductSvc   │ │ FlashSaleService │
  └───────┬────────┘ └──────┬───────┘ └────────┬─────────┘
          │                 │                   │
          ▼                 ▼                   ▼
  ┌────────────────┐ ┌──────────────┐ ┌──────────────────┐
  │  MemberMapper  │ │ ProductMapper│ │ FlashItemMapper  │
  │                │ │ SellerMapper │ │ FlashGuardMapper │
  │                │ │              │ │ TradeOrderMapper │
  └───────┬────────┘ └──────┬───────┘ └────────┬─────────┘
          │                 │                   │
          └─────────────────┼───────────────────┘
                            ▼
              ┌──────────────────────────┐
              │        MySQL 数据库       │
              │     flash_sale_db        │
              └──────────────────────────┘
                            │
              ┌──────────────────────────┐
              │      Redis 缓存          │
              │  库存预热 / 防重复购买     │
              └──────────────────────────┘
```

### 2.2 服务拆分说明

本系统按业务领域划分为以下四个服务模块：

| 服务模块 | 负责内容 | 核心类 |
|---------|---------|--------|
| **用户服务** | 用户注册、登录认证、密码加密 | `AccountController` → `AccountService` → `MemberMapper` |
| **商品服务** | 商品信息管理、秒杀商品列表查询 | `ProductController` → `ProductService` → `ProductMapper` / `SellerMapper` |
| **订单服务** | 订单创建、订单状态管理、历史订单查询 | `FlashSaleController` → `FlashSaleService` → `TradeOrderMapper` |
| **库存服务** | Redis 库存预热、缓存预扣减、数据库乐观锁扣减、防重复购买 | `InventoryWarmUpTask` + `FlashSaleService` → `FlashItemMapper` / `FlashGuardMapper` |

> 当前版本为单体架构，所有服务模块部署在同一个 Spring Boot 应用中。后续可通过 Spring Cloud 拆分为独立微服务。

---

## 三、各服务 API 接口定义（RESTful）

### 3.1 用户服务

| 方法 | 路径 | 说明 | 请求参数 | 返回值 |
|------|------|------|---------|--------|
| POST | `/v1/account/sign-in` | 用户登录 | `account`（用户ID或昵称）, `password` | 用户信息（脱敏） |
| POST | `/v1/account/sign-up` | 用户注册 | `alias`（昵称）, `password`, `confirmPassword` | 新用户信息 |

**登录逻辑**：支持数字ID或昵称两种方式登录，密码使用 MD5 + 随机盐值加密验证。

**注册逻辑**：
- 昵称不能为纯数字（避免与ID冲突）
- 密码最少6位
- 系统自动分配随机数字ID（范围 1000 ~ 9999999）

### 3.2 商品服务

| 方法 | 路径 | 说明 | 请求参数 | 返回值 |
|------|------|------|---------|--------|
| GET | `/v1/product/flash/active` | 获取当前生效的秒杀商品列表 | 无 | `List<FlashProductVO>` |

**返回字段**：商品ID、商品名称、图片URL、原价、秒杀价、剩余库存、秒杀开始/结束时间

### 3.3 订单/库存服务

| 方法 | 路径 | 说明 | 请求参数 | 返回值 |
|------|------|------|---------|--------|
| POST | `/v1/flash/snap` | 执行秒杀抢购 | `memberId`, `flashId` | 订单信息 |
| GET | `/v1/flash/my-orders` | 查询用户历史订单 | `memberId` | `List<TradeOrder>` |

**秒杀核心流程**（三重防护机制）：
1. **Redis Set 去重**：检查用户是否已抢购过该商品（`fs:purchased:{flashId}` 集合）
2. **Redis 原子预扣**：对库存 key（`fs:inv:{flashId}`）执行 `decrement`，若结果 < 0 则回滚
3. **数据库乐观锁**：通过 `version` 字段实现 CAS 扣减（`remaining > 0 AND version = ?`）
4. **唯一索引兜底**：`t_flash_guard` 表的 `(member_id, product_id)` 联合唯一索引防止极端情况下的重复订单

---

## 四、数据库 ER 图

### 4.1 ER 关系图

```
┌──────────────┐       ┌──────────────────┐       ┌────────────────┐
│   t_seller   │       │    t_product     │       │  t_flash_item  │
│──────────────│       │──────────────────│       │────────────────│
│ id (PK)      │◄──┐   │ id (PK)          │◄──┐   │ id (PK)        │
│ shop_name    │   └───│ seller_id (FK)   │   └───│ product_id(FK) │
│ status       │       │ product_name     │       │ flash_price    │
└──────────────┘       │ image_url        │       │ remaining      │
                       │ price            │       │ begin_at       │
                       │ stock            │       │ finish_at      │
                       └──────────────────┘       │ version        │
                                                  └────────────────┘
                              │
                              │ product_id
                              ▼
┌──────────────┐       ┌──────────────────┐       ┌────────────────┐
│   t_member   │       │  t_trade_order   │       │ t_flash_guard  │
│──────────────│       │──────────────────│       │────────────────│
│ id (PK)      │◄──┐   │ id (PK)          │◄──┐   │ id (PK)        │
│ alias        │   ├───│ member_id (FK)   │   └───│ order_id (FK)  │
│ pwd          │   │   │ seller_id (FK)   │   ┌───│ member_id (FK) │
│ salt         │   │   │ product_id (FK)  │   │   │ product_id(FK) │
│ joined_at    │   │   │ product_name     │   │   └────────────────┘
└──────────────┘   │   │ amount           │   │   UNIQUE(member_id,
                   │   │ status           │   │          product_id)
                   │   │ created_at       │   │
                   │   └──────────────────┘   │
                   └──────────────────────────┘
```

### 4.2 各表说明

#### t_member（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK | 用户ID，系统随机分配 |
| alias | VARCHAR(50) | 用户昵称，不可为纯数字 |
| pwd | VARCHAR(64) | MD5(salt + 明文密码) |
| salt | VARCHAR(16) | 随机盐值，6位十六进制 |
| joined_at | DATETIME | 注册时间，默认当前时间 |

#### t_seller（商家表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 商家ID |
| shop_name | VARCHAR(100) | 店铺名称 |
| status | TINYINT | 状态：0-停用, 1-启用 |

#### t_product（商品表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 商品ID |
| seller_id | BIGINT, FK → t_seller.id | 所属商家 |
| product_name | VARCHAR(200) | 商品名称 |
| image_url | VARCHAR(500) | 商品图片地址 |
| price | DECIMAL(10,2) | 原始零售价 |
| stock | INT | 普通库存数量 |

#### t_flash_item（秒杀配置表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 秒杀活动ID |
| product_id | BIGINT, FK → t_product.id | 关联商品 |
| flash_price | DECIMAL(10,2) | 秒杀特价 |
| remaining | INT | 秒杀剩余库存 |
| begin_at | DATETIME | 秒杀开始时间 |
| finish_at | DATETIME | 秒杀结束时间 |
| version | INT | 乐观锁版本号，每次扣减+1 |

> **设计要点**：秒杀库存与普通库存分离（冷热数据分离），秒杀扣减不影响商品主表。

#### t_trade_order（订单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 订单ID |
| member_id | BIGINT, FK → t_member.id | 下单用户 |
| seller_id | BIGINT, FK → t_seller.id | 对应商家 |
| product_id | BIGINT, FK → t_product.id | 对应商品 |
| product_name | VARCHAR(200) | 商品名称快照 |
| amount | DECIMAL(10,2) | 实际支付金额 |
| status | TINYINT | 0-待付款, 1-已付款, 2-已发货, 3-已退款 |
| created_at | DATETIME | 下单时间 |

> **设计要点**：`product_name` 为快照字段，记录下单时刻的商品名称，避免后续商品改名影响历史订单展示。

#### t_flash_guard（秒杀防重表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 记录ID |
| member_id | BIGINT, FK → t_member.id | 购买用户 |
| order_id | BIGINT, FK → t_trade_order.id | 关联订单 |
| product_id | BIGINT, FK → t_product.id | 购买商品 |

> **设计要点**：`(member_id, product_id)` 建立联合唯一索引，从数据库层面彻底杜绝同一用户对同一商品的重复抢购。

---

## 五、技术栈选型说明

### 5.1 编程语言与框架

| 组件 | 选型 | 版本 | 选型理由 |
|------|------|------|---------|
| 编程语言 | Java | 17 | LTS 版本，支持密封类、模式匹配等新特性 |
| Web 框架 | Spring Boot | 3.2.6 | 主流微服务框架，开箱即用，生态完善 |
| ORM 框架 | MyBatis | 3.0.3 | 轻量灵活，注解式 SQL 便于维护，适合复杂查询 |
| 工具库 | Lombok | — | 减少样板代码（getter/setter/构造器等） |

### 5.2 中间件

| 组件 | 选型 | 用途说明 |
|------|------|---------|
| 关系型数据库 | MySQL 8.x | 存储用户、商品、订单等核心业务数据 |
| 缓存中间件 | Redis 7.x | 秒杀库存预热、原子预扣减、防重复购买集合 |

### 5.3 构建与部署

| 组件 | 选型 | 说明 |
|------|------|------|
| 构建工具 | Maven | 依赖管理和项目构建 |
| JDK 版本 | 17+ | 需要 Java 17 或更高版本 |

### 5.4 关键设计模式说明

- **Redis 预扣减 + 数据库乐观锁**：双层防护保证高并发下库存不超卖
- **MD5 + 随机盐值**：密码安全存储，每个用户独立盐值
- **冷热数据分离**：秒杀库存独立于商品主库存，互不影响
- **联合唯一索引**：数据库层面兜底防止重复购买
- **构造器注入**：所有 Service 采用 `@RequiredArgsConstructor` 构造器注入，符合 Spring 最佳实践
- **ApplicationRunner 预热**：应用启动时自动将秒杀库存加载到 Redis

---

## 六、项目结构

```
src/
├── main/
│   ├── java/cn/edu/distcourse/flashsale/
│   │   ├── FlashSaleApplication.java        # 主启动类
│   │   ├── common/
│   │   │   └── ApiResponse.java             # 统一响应封装
│   │   ├── bootstrap/
│   │   │   └── InventoryWarmUpTask.java     # 库存预热任务
│   │   ├── controller/
│   │   │   ├── AccountController.java       # 用户服务接口
│   │   │   ├── ProductController.java       # 商品服务接口
│   │   │   └── FlashSaleController.java     # 秒杀服务接口
│   │   ├── service/
│   │   │   ├── AccountService.java          # 用户业务逻辑
│   │   │   ├── ProductService.java          # 商品业务逻辑
│   │   │   └── FlashSaleService.java        # 秒杀核心逻辑
│   │   ├── dao/
│   │   │   ├── MemberMapper.java            # 用户数据访问
│   │   │   ├── SellerMapper.java            # 商家数据访问
│   │   │   ├── ProductMapper.java           # 商品数据访问
│   │   │   ├── FlashItemMapper.java         # 秒杀配置数据访问
│   │   │   ├── FlashGuardMapper.java        # 防重购数据访问
│   │   │   └── TradeOrderMapper.java        # 订单数据访问
│   │   ├── model/
│   │   │   ├── Member.java                  # 用户实体
│   │   │   ├── Seller.java                  # 商家实体
│   │   │   ├── Product.java                 # 商品实体
│   │   │   ├── FlashItem.java               # 秒杀配置实体
│   │   │   ├── FlashGuard.java              # 防重购实体
│   │   │   └── TradeOrder.java              # 订单实体
│   │   └── vo/
│   │       └── FlashProductVO.java          # 秒杀商品视图对象
│   └── resources/
│       ├── application.yml                  # 应用配置
│       └── static/
│           ├── login.html                   # 登录/注册页面
│           └── index.html                   # 秒杀商城主页
└── test/
    └── java/cn/edu/distcourse/flashsale/
        └── DaoLayerTest.java                # 数据访问层测试
```

---

## 七、快速启动

### 环境要求
- JDK 17+
- MySQL 8.x
- Redis 7.x
- Maven 3.8+

### 启动步骤

1. 创建数据库 `flash_sale_db` 并初始化表结构
2. 修改 `application.yml` 中的数据库连接信息
3. 启动 Redis 服务
4. 运行主类 `FlashSaleApplication`
5. 访问 `http://localhost:9090/login.html`
