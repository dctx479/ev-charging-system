# EV Charging Station Management System - Backend

## 项目简介

基于 Spring Boot 3.x 的电动汽车充电站管理系统后端服务。

## 技术栈

- Spring Boot 3.2.0
- Spring Data JPA
- Spring Data Redis
- MySQL 8.0
- Redis
- JWT（JSON Web Token）
- Lombok
- Hutool

## 项目结构

```
backend/
├── src/main/java/com/ev/charging/
│   ├── ChargingSystemApplication.java    # 主启动类
│   ├── controller/                       # 控制器层
│   │   ├── AuthController.java          # 用户认证
│   │   ├── ChargingStationController.java # 充电站
│   │   └── ChargingPileController.java  # 充电桩
│   ├── service/                         # 服务层
│   │   ├── UserService.java
│   │   ├── ChargingStationService.java
│   │   └── ChargingPileService.java
│   ├── repository/                      # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── ChargingStationRepository.java
│   │   └── ChargingPileRepository.java
│   ├── entity/                          # 实体类
│   │   ├── User.java
│   │   ├── ChargingStation.java
│   │   └── ChargingPile.java
│   ├── dto/                             # 数据传输对象
│   ├── vo/                              # 视图对象
│   ├── common/                          # 通用类
│   │   ├── Result.java                  # 统一返回格式
│   │   ├── ResultCode.java              # 状态码枚举
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── config/                          # 配置类
│   │   ├── RedisConfig.java
│   │   └── WebConfig.java
│   └── util/                            # 工具类
│       ├── JwtUtil.java
│       └── DistanceUtil.java
└── src/main/resources/
    ├── application.yml                   # 主配置文件
    └── application-dev.yml               # 开发环境配置
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库配置

创建数据库：
```sql
CREATE DATABASE ev_charging DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ev_charging
    username: root
    password: your_password
```

### 3. 启动 Redis

```bash
redis-server
```

### 4. 编译项目

```bash
mvn clean install
```

### 5. 运行项目

```bash
mvn spring-boot:run
```

或者：
```bash
java -jar target/charging-system-1.0.0.jar
```

访问地址：http://localhost:8080/api

## API 接口

### 认证接口

- POST `/api/auth/login` - 用户登录
- POST `/api/auth/register` - 用户注册
- GET `/api/auth/user/info` - 获取用户信息
- POST `/api/auth/logout` - 退出登录

### 充电站接口

- GET `/api/stations` - 获取所有营业中的充电站
- GET `/api/stations/{id}` - 获取充电站详情
- GET `/api/stations/search?keyword=xxx` - 搜索充电站
- GET `/api/stations/nearby?latitude=xxx&longitude=xxx&radius=5` - 查询附近充电站
- POST `/api/stations` - 创建充电站（管理员）
- PUT `/api/stations/{id}` - 更新充电站（管理员）
- DELETE `/api/stations/{id}` - 删除充电站（管理员）

### 充电桩接口

- GET `/api/piles` - 获取所有充电桩
- GET `/api/piles/{id}` - 获取充电桩详情
- GET `/api/piles/station/{stationId}` - 获取充电站的充电桩列表
- GET `/api/piles/station/{stationId}/available` - 获取可用充电桩
- GET `/api/piles/fault` - 获取故障充电桩（管理员）
- POST `/api/piles` - 创建充电桩（管理员）
- PUT `/api/piles/{id}` - 更新充电桩（管理员）
- PATCH `/api/piles/{id}/status` - 更新充电桩状态（管理员）
- POST `/api/piles/{id}/maintenance` - 记录维护（管理员）
- DELETE `/api/piles/{id}` - 删除充电桩（管理员）

## 统一返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 开发说明

1. 实体类使用 JPA 注解自动建表
2. 密码使用 BCrypt 加密存储
3. 使用 JWT 进行用户认证
4. 全局异常处理确保接口返回一致
5. 支持 CORS 跨域访问

## 注意事项

- 首次启动时 JPA 会自动创建数据表
- 默认使用 `application-dev.yml` 开发环境配置
- JWT Secret Key 在生产环境中应使用更复杂的密钥
- 需要先启动 MySQL 和 Redis 服务

## 后续开发

- [ ] 实现 JWT 拦截器
- [ ] 添加订单管理模块
- [ ] 添加支付模块
- [ ] 实现 WebSocket 实时通信
- [ ] 添加更多单元测试
