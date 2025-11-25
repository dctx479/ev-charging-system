# EV Charging Station Management System

新能源汽车充电站点管理系统

## 📋 项目简介

这是一个完整的全栈项目，实现了新能源汽车充电站点的智能管理系统，包含用户端、运营商管理后台和 AI 智能预测服务。

## 🏗️ 项目结构

```
ev-charging-system/
├── backend/                    # Spring Boot 3.x 后端服务
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── frontend-user/              # Vue3 + Vant4 用户端（移动端）
│   ├── src/
│   ├── package.json
│   └── README.md
│
├── frontend-admin/             # Vue3 + Element Plus 管理后台
│   ├── src/
│   ├── package.json
│   └── README.md
│
├── ai-service/                 # Flask AI 预测服务
│   ├── app.py
│   ├── requirements.txt
│   └── README.md
│
├── docker-compose.yml          # Docker Compose 配置
├── .gitignore
├── PROJECT_FRAMEWORK.md        # 框架说明文档
└── README.md                   # 本文件
```

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Node.js**: 18+
- **Python**: 3.9+
- **Maven**: 3.8+
- **MySQL**: 8.0+
- **Redis**: 7.0+
- **RabbitMQ**: 3.x

### 1. 启动基础服务

使用 Docker Compose 一键启动 MySQL、Redis、RabbitMQ：

```bash
docker-compose up -d
```

验证服务状态：
```bash
docker-compose ps
```

### 2. 初始化数据库

```bash
# 连接到 MySQL
mysql -u root -p -h localhost -P 3306

# 导入数据库脚本（在父目录中）
source ../database/database_design.sql
```

### 3. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端 API 地址：http://localhost:8080

测试接口：
```bash
curl http://localhost:8080/api/health
```

### 4. 启动 AI 服务

```bash
cd ai-service

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 训练模型
python train/train_duration_model.py
python train/train_fault_model.py

# 启动 Flask 服务
python app.py
```

AI 服务地址：http://localhost:5000

测试接口：
```bash
curl -X POST http://localhost:5000/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{"battery_capacity":75,"current_soc":20,"target_soc":80,"charge_power":120}'
```

### 5. 启动用户端前端

```bash
cd frontend-user
npm install
npm run dev
```

用户端地址：http://localhost:5173

### 6. 启动管理后台

```bash
cd frontend-admin
npm install
npm run dev
```

管理后台地址：http://localhost:5174

## 📊 技术架构

### 后端技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA
- **缓存**: Redis 7.0
- **消息队列**: RabbitMQ 3.x
- **认证**: JWT
- **工具**: Lombok, MapStruct

### 前端技术栈

**用户端（移动端）**:
- Vue 3
- Vite 5
- Vant 4
- Pinia
- Vue Router
- Axios
- 高德地图 API

**管理后台（PC端）**:
- Vue 3
- Vite 5
- Element Plus
- ECharts
- Pinia
- Vue Router
- Axios

### AI 技术栈

- **框架**: Flask 3.0
- **机器学习**: Scikit-learn
- **数据处理**: Pandas, NumPy
- **模型存储**: Joblib

## 🔌 API 文档

### 后端 API

**基础路径**: `http://localhost:8080/api`

#### 认证相关
- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /auth/userinfo` - 获取用户信息

#### 充电站相关
- `GET /stations` - 获取充电站列表
- `GET /stations/{id}` - 获取充电站详情
- `GET /stations/nearby` - 查询附近充电站

#### 充电桩相关
- `GET /piles` - 获取充电桩列表
- `GET /piles/{id}` - 获取充电桩详情
- `PUT /piles/{id}/status` - 更新充电桩状态

### AI API

**基础路径**: `http://localhost:5000/api/ai`

#### 预测接口
- `POST /predict/duration` - 预测充电时长
- `POST /predict/fault` - 预测故障概率

详细 API 文档请参考各模块的 README.md 文件。

## 🎯 核心功能

### 已实现功能

✅ 用户认证与授权（JWT）
✅ 充电站查询与筛选
✅ 附近充电站查询（基于地理位置）
✅ 充电桩状态管理
✅ AI 充电时长预测
✅ AI 故障预测
✅ 数据可视化（管理后台）
✅ 实时状态监控

### 待实现功能（根据原项目规划）

📋 充电订单管理
📋 智能排队系统
📋 碳积分体系
📋 周边服务推荐
📋 V2G 双向充电
📋 光储充一体化
📋 充电搭子社交
📋 代充服务
📋 WebSocket 实时推送
📋 支付集成

## 🧪 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend-user
npm run test

cd frontend-admin
npm run test
```

### AI 模型评估

```bash
cd ai-service
python train/train_duration_model.py  # 查看模型评估指标
```

## 📦 构建部署

### 后端打包

```bash
cd backend
mvn clean package
java -jar target/charging-system-1.0.0.jar
```

### 前端构建

```bash
cd frontend-user
npm run build
# 构建产物在 dist/ 目录

cd frontend-admin
npm run build
# 构建产物在 dist/ 目录
```

### Docker 部署

```bash
# 构建镜像
docker build -t ev-charging-backend ./backend
docker build -t ev-charging-user ./frontend-user
docker build -t ev-charging-admin ./frontend-admin
docker build -t ev-charging-ai ./ai-service

# 启动所有服务
docker-compose up -d
```

## 🔧 开发指南

### 代码规范

- **Java**: 遵循阿里巴巴 Java 开发规范
- **JavaScript**: 使用 ESLint + Prettier
- **Python**: 遵循 PEP 8 规范
- **Git**: 使用语义化提交信息

### 提交信息格式

```
feat: 添加新功能
fix: 修复 bug
docs: 更新文档
style: 代码格式调整
refactor: 重构代码
test: 添加测试
chore: 构建工具或依赖更新
```

### 分支管理

- `main` - 主分支（受保护）
- `develop` - 开发分支
- `feature/*` - 功能分支
- `bugfix/*` - 修复分支

## 📖 相关文档

- [项目框架说明](./PROJECT_FRAMEWORK.md)
- [后端开发指南](./backend/README.md)
- [用户端开发指南](./frontend-user/README.md)
- [管理后台开发指南](./frontend-admin/README.md)
- [AI 服务开发指南](./ai-service/README.md)
- [数据库设计文档](../database/ER图设计说明.md)
- [课设总体规划](../README.md)

## 🐛 问题排查

### 常见问题

**问题 1: 后端启动失败 - 数据库连接错误**
```
解决方案:
1. 确认 MySQL 服务已启动
2. 检查 application.yml 中的数据库配置
3. 确认数据库 ev_charging_system 已创建
```

**问题 2: 前端无法连接后端 API**
```
解决方案:
1. 确认后端服务已启动（http://localhost:8080）
2. 检查浏览器控制台 Network 标签
3. 确认 vite.config.js 中的代理配置正确
```

**问题 3: AI 服务模型未找到**
```
解决方案:
1. 先运行训练脚本生成模型文件
2. 确认 models/ 目录下有 .pkl 文件
3. 检查 app.py 中的模型路径
```

**问题 4: Redis 连接失败**
```
解决方案:
1. 确认 Redis 服务已启动（docker-compose ps）
2. 测试连接：redis-cli ping
3. 检查端口 6379 是否被占用
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目为课程设计项目，仅用于学习和教学目的。

## 👥 团队

本项目由课程设计小组成员共同完成。

## 📮 联系方式

如有问题，请通过以下方式联系：

- 提交 Issue
- 发送邮件至项目维护者

---

**最后更新**: 2025-11-25

**项目版本**: 1.0.0-SNAPSHOT
