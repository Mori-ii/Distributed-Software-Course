# 生活服务-秒杀商品系统

分布式软件工程课程项目

## 项目简介

本项目是一个基于 Spring Boot 的分布式生活服务秒杀商品系统，实现了用户注册登录、商品浏览、秒杀抢购、订单管理、笔记互动等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.4
- **ORM**: MyBatis-Plus 3.5.5
- **缓存**: Redis + Redisson 3.13.6
- **消息队列**: Kafka
- **数据库**: MySQL 8.0
- **前端**: Vue.js 2 + Element UI + Axios
- **反向代理**: Nginx (负载均衡 + 动静分离)

## 项目结构

```
Distributed-Software-Course/
├── docs/                          # 课程作业文档
│   ├── 系统设计文档.md
│   ├── 第一讲作业-项目搭建与基础开发.md
│   ├── 第二讲作业-容器与缓存.md
│   ├── 第三讲作业-读写分离.md
│   ├── 第四讲作业-消息队列.md
│   └── 第五讲作业-事务与一致性.md
├── frontend/
│   └── seckill/                   # 前端静态资源
│       ├── *.html                 # 页面
│       ├── css/                   # 样式
│       ├── js/                    # 脚本
│       └── imgs/                  # 图片
├── src/
│   ├── main/
│   │   ├── java/com/seckill/      # Java源码
│   │   │   ├── config/            # 配置类
│   │   │   ├── controller/        # 控制器
│   │   │   ├── service/           # 服务层
│   │   │   ├── mapper/            # 数据访问层
│   │   │   ├── entity/            # 实体类
│   │   │   ├── dto/               # 数据传输对象
│   │   │   └── utils/             # 工具类
│   │   └── resources/
│   │       ├── application.yaml   # 应用配置
│   │       └── db/seckill.sql     # 数据库脚本
│   └── test/                      # 测试类
├── docker-compose.yml             # Docker部署配置
├── Dockerfile                     # Docker镜像构建
├── nginx.conf                     # Nginx负载均衡配置
├── frontend.conf                  # Nginx动静分离配置
└── pom.xml                        # Maven配置
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.2+
- Kafka

### 本地启动

1. 初始化数据库:
```bash
mysql -u root -p < src/main/resources/db/seckill.sql
```

2. 启动 Redis:
```bash
redis-server
```

3. 启动应用:
```bash
mvn spring-boot:run
```

### Docker 部署

```bash
docker-compose up -d
```

## 功能模块

- **用户模块**: 注册、登录、签到、个人信息管理
- **商品模块**: 商品浏览、详情查看、店铺分类
- **秒杀模块**: 秒杀抢购、优惠券、库存管理
- **笔记模块**: 笔记发布、评论、点赞
- **社交模块**: 用户关注

## API 接口

详见 [系统设计文档](docs/系统设计文档.md)

## 课程文档

| 讲次 | 文档 | 主要内容 |
|------|------|---------|
| 第一讲 | [项目搭建与基础开发](docs/第一讲作业-项目搭建与基础开发.md) | Spring Boot项目搭建、数据库设计、用户登录、拦截器 |
| 第二讲 | [容器与缓存](docs/第二讲作业-容器与缓存.md) | Docker容器化、Nginx负载均衡、动静分离、Redis缓存 |
| 第三讲 | [读写分离](docs/第三讲作业-读写分离.md) | MySQL主从复制、ShardingSphere读写分离 |
| 第四讲 | [消息队列](docs/第四讲作业-消息队列.md) | Kafka异步下单、雪花算法、幂等性、分库分表 |
| 第五讲 | [事务与一致性](docs/第五讲作业-事务与一致性.md) | Redis库存预扣减、消息一致性、分布式锁、TCC事务 |
