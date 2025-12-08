# EV Charging Station Management System - 项目框架

本文档是项目代码框架的说明文档。完整的项目规划请参阅 [README.md](README.md)

## 已创建的代码框架

本项目包含以下完整的基础代码框架：

### 1. 后端服务 (backend/)

基于 Spring Boot 3.2.0 的后端服务，包含：

**核心功能**：
- 用户认证与授权（JWT）
- 充电站管理（CRUD、搜索、附近查询）
- 充电桩管理（状态监控、故障管理）
- 统一返回格式和异常处理
- Redis 缓存配置
- 跨域配置

**技术栈**：
- Spring Boot 3.2.0 + Spring Data JPA
- MySQL 8.0 + Redis
- JWT 认证
- Lombok + Hutool

**启动方式**：
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

访问：http://localhost:8080/api

### 2. 用户端前端 (frontend-user/)

基于 Vue 3 + Vant 4 的移动端应用，包含：

**核心页面**：
- 登录页面
- 首页（充电站地图列表）
- 充电站详情页
- 充电桩列表页

**核心组件**：
- StationCard（充电站卡片）
- PileCard（充电桩卡片）

**功能特性**：
- Pinia 状态管理
- Axios 请求封装（拦截器、错误处理）
- Vue Router 路由配置
- 高德地图工具类（定位、距离计算）

**启动方式**：
```bash
cd frontend-user
npm install
npm run dev
```

访问：http://localhost:5173

### 3. 管理后台前端 (frontend-admin/)

基于 Vue 3 + Element Plus 的管理后台，包含：

**核心页面**：
- 登录页面
- 数据概览仪表盘
- 充电桩管理
- 故障管理
- 统计分析

**功能特性**：
- Element Plus UI 组件库
- 侧边栏导航布局
- WebSocket 工具类（实时通信）
- Pinia 状态管理

**启动方式**：
```bash
cd frontend-admin
npm install
npm run dev
```

访问：http://localhost:5174

### 4. AI 服务 (ai-service/)

基于 Flask + Scikit-learn 的 AI 预测服务，包含：

**核心功能**：
- 充电时长预测（随机森林回归）
- 充电桩故障预测（随机森林分类）
- Mock 数据生成器
- 模型训练脚本

**API 接口**：
- POST `/api/ai/predict/duration` - 充电时长预测
- POST `/api/ai/predict/fault` - 故障预测

**启动方式**：
```bash
cd ai-service
pip install -r requirements.txt

# 首次运行需要训练模型
python train/train_duration_model.py
python train/train_fault_model.py

# 启动服务
python app.py
```

访问：http://localhost:5000

### 5. 配置文件

**docker-compose.yml**：
- MySQL 8.0 数据库
- Redis 7 缓存
- RabbitMQ 3.12 消息队列

**启动方式**：
```bash
docker-compose up -d
```

**.gitignore**：
- 已配置 Java、Node.js、Python 项目忽略规则
- 排除构建产物、依赖文件、日志等

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- Python 3.8+
- Docker（可选，用于启动 MySQL/Redis）

### 启动顺序

1. 启动基础服务（MySQL、Redis）
```bash
docker-compose up -d
```

2. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

3. 启动 AI 服务
```bash
cd ai-service
pip install -r requirements.txt
python train/train_duration_model.py
python train/train_fault_model.py
python app.py
```

4. 启动用户端前端
```bash
cd frontend-user
npm install
npm run dev
```

5. 启动管理后台前端
```bash
cd frontend-admin
npm install
npm run dev
```

## 目录结构

```
课设/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ev/charging/
│   │   ├── ChargingSystemApplication.java
│   │   ├── controller/        # 控制器（Auth, Station, Pile）
│   │   ├── service/           # 业务逻辑
│   │   ├── repository/        # 数据访问
│   │   ├── entity/            # 实体类（User, Station, Pile）
│   │   ├── dto/               # 数据传输对象
│   │   ├── vo/                # 视图对象
│   │   ├── common/            # 通用类（Result, Exception）
│   │   ├── config/            # 配置类（Redis, Web）
│   │   └── util/              # 工具类（JWT, Distance）
│   └── src/main/resources/
│       ├── application.yml
│       └── application-dev.yml
│
├── frontend-user/             # Vue3 + Vant4 用户端
│   ├── src/
│   │   ├── api/              # API 接口
│   │   ├── components/       # 组件（StationCard, PileCard）
│   │   ├── views/            # 页面（Login, Home, Detail, List）
│   │   ├── router/           # 路由
│   │   ├── store/            # Pinia 状态管理
│   │   ├── utils/            # 工具类（request, map）
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js
│   └── package.json
│
├── frontend-admin/            # Vue3 + Element Plus 管理后台
│   ├── src/
│   │   ├── api/              # API 接口
│   │   ├── layout/           # 主布局
│   │   ├── views/            # 页面
│   │   ├── router/           # 路由
│   │   ├── store/            # Pinia 状态管理
│   │   ├── utils/            # 工具类（request, websocket）
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js
│   └── package.json
│
├── ai-service/                # Flask AI 服务
│   ├── models/               # 训练好的模型文件
│   ├── train/                # 训练脚本
│   │   ├── train_duration_model.py
│   │   └── train_fault_model.py
│   ├── utils/
│   │   └── data_generator.py
│   ├── app.py
│   └── requirements.txt
│
├── docker-compose.yml         # Docker 配置
├── .gitignore                 # Git 忽略规则
├── PROJECT_FRAMEWORK.md       # 本文件
└── README.md                  # 完整项目说明
```

## 已实现功能

### 后端 API

1. **认证接口**
   - POST `/api/auth/login` - 用户登录
   - POST `/api/auth/register` - 用户注册
   - GET `/api/auth/user/info` - 获取用户信息

2. **充电站接口**
   - GET `/api/stations` - 获取充电站列表
   - GET `/api/stations/{id}` - 获取充电站详情
   - GET `/api/stations/search` - 搜索充电站
   - GET `/api/stations/nearby` - 查询附近充电站
   - POST/PUT/DELETE - 管理充电站（管理员）

3. **充电桩接口**
   - GET `/api/piles/station/{stationId}` - 获取充电站的充电桩
   - GET `/api/piles/station/{stationId}/available` - 获取可用充电桩
   - GET `/api/piles/fault` - 获取故障充电桩
   - POST/PUT/DELETE - 管理充电桩（管理员）

### 前端页面

**用户端**：
- 登录页面（表单验证、JWT 存储）
- 首页（充电站列表、下拉刷新、定位功能）
- 充电站详情（信息展示、评分、距离）
- 充电桩列表（全部/可用筛选）

**管理后台**：
- 登录页面
- 数据概览仪表盘（统计卡片）
- 侧边栏导航布局
- 充电桩管理、故障管理、统计分析页面占位

### AI 功能

- 充电时长预测（基于电池容量、电量、功率、温度）
- 故障预测（基于使用次数、电量、温度、维护间隔）
- 完整的模型训练脚本和数据生成器

## 待实现功能

根据 [README.md](README.md) 中的完整规划，以下功能需要继续开发：

### 第三部分：订单系统 + 创新服务
- 充电订单流程
- 智能排队系统
- 周边服务推荐
- 碳积分体系
- 支付集成

### 第四部分：能源管理 + 社交功能
- V2G 双向充电
- 光储充一体化
- 充电搭子社交
- 代充服务

### 其他功能
- WebSocket 实时数据推送
- 订单管理
- 用户个人中心
- 评价系统
- 更多数据可视化图表

## 数据库说明

数据库设计文档和建表脚本请参考：
- `database/database_design.sql` - 18张表的完整SQL脚本
- `database/ER图设计说明.md` - 数据库关系说明

本框架已创建的实体类对应的核心表：
- `users` - 用户表
- `charging_stations` - 充电站表
- `charging_piles` - 充电桩表

## 注意事项

1. **首次运行**：
   - 确保 MySQL 和 Redis 已启动
   - 后端会自动创建数据表（JPA ddl-auto: update）
   - AI 服务首次运行需要先训练模型

2. **端口占用**：
   - 8080：后端服务
   - 5173：用户端前端
   - 5174：管理后台前端
   - 5000：AI 服务
   - 3306：MySQL
   - 6379：Redis

3. **环境变量配置**：
   - 在项目根目录创建 `.env` 文件（已在 .gitignore 中）
   - 配置示例：
     ```env
     DB_PASSWORD=your-database-password
     REDIS_PASSWORD=your-redis-password
     JWT_SECRET=your-jwt-secret-key
     ```
   - MySQL 密码：从环境变量 `${DB_PASSWORD}` 读取
   - JWT Secret：在 application.yml 中配置
   - 测试账号：admin / 请自行设置

4. **开发建议**：
   - 使用 Postman 测试 API
   - 前端使用 Chrome DevTools 移动设备模拟器
   - 查看各子项目的 README.md 了解详细信息
   - **切勿将密码硬编码到代码或配置文件中**

## 技术支持

各模块的详细文档：
- [后端 README](backend/README.md)
- [用户端前端 README](frontend-user/README.md)
- [管理后台前端 README](frontend-admin/README.md)
- [AI 服务 README](ai-service/README.md)

完整项目规划：
- [项目总览 README](README.md)

---

**框架创建时间**：2025-11-25
**框架状态**：基础代码框架已完成，可在此基础上继续开发
