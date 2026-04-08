# 生活服务-秒杀商品系统

分布式软件工程课程项目

## 项目简介

本项目是一个基于 Spring Boot 的分布式生活服务秒杀商品系统，实现了用户注册登录、商品浏览、订单管理、秒杀等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.4
- **ORM**: MyBatis-Plus 3.5.5
- **缓存**: Redis + Redisson 3.13.6
- **消息队列**: Kafka
- **数据库**: MySQL 8.0

## 项目结构

```
Distributed-Software-Course/
├── docs/
│   └── 系统设计文档.md           # 系统设计文档
├── src/
│   ├── main/
│   │   ├── java/com/seckill/   # Java源码
│   │   └── resources/          # 配置文件
│   └── test/                   # 测试类
├── docker-compose.yml          # Docker部署配置
├── Dockerfile                  # Docker镜像构建
├── nginx.conf                  # Nginx配置
└── pom.xml                     # Maven配置
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.2+

### 启动步骤

1. 初始化数据库:
```bash
mysql -u root -p < src/main/resources/db/seckill.sql
```

2. 启动应用:
```bash
mvn spring-boot:run
```

## 功能模块

- **用户服务**: 注册、登录、签到
- **商品服务**: 商品浏览、详情查看
- **订单服务**: 订单创建、查询
- **库存服务**: 库存管理、秒杀

## API 接口

详见 [系统设计文档](docs/系统设计文档.md)
