# 快速启动指南

本指南将帮助您快速搭建和运行 EV 充电站管理系统。

## 📋 目录

1. [环境要求](#环境要求)
2. [数据库与服务配置](#数据库与服务配置)
3. [后端服务启动](#后端服务启动)
4. [前端服务启动](#前端服务启动)
5. [验证系统运行](#验证系统运行)
6. [常见问题](#常见问题)

---

## 环境要求

请确保您的开发环境已安装以下软件：

### 必需软件

| 软件 | 版本要求 | 下载链接 |
|------|---------|---------|
| JDK | 17+ | https://www.oracle.com/java/technologies/downloads/ |
| Maven | 3.8+ | https://maven.apache.org/download.cgi |
| Node.js | 18+ | https://nodejs.org/ |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/mysql/ |
| Redis | 7.0+ | https://redis.io/download/ |

### 可选软件

- **Docker Desktop**: 用于快速启动 MySQL 和 Redis（推荐）
  - 下载地址: https://www.docker.com/products/docker-desktop/

---

## 数据库与服务配置

### 方案一：使用 Docker（推荐）

#### 1. 安装 Docker Desktop

下载并安装 Docker Desktop，启动后确保 Docker 服务正常运行。

#### 2. 启动数据库服务

在项目根目录执行：

```bash
cd D:\软工设计code\ev-charging-system
docker-compose up -d
```

#### 3. 验证服务状态

```bash
docker-compose ps
```

应该看到以下容器正在运行：
- `ev-charging-mysql` (MySQL 8.0, 端口 3306)
- `ev-charging-redis` (Redis 7, 端口 6379)
- `ev-charging-rabbitmq` (RabbitMQ, 端口 5672, 15672)

#### 4. 初始化数据库

首次启动需要创建数据库表和测试数据：

```bash
# 方式1: 使用 MySQL 客户端
mysql -h localhost -P 3306 -u root -proot123456

# 进入MySQL后执行
CREATE DATABASE IF NOT EXISTS ev_charging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_charging;

# 导入初始化脚本（需要创建 init.sql 文件）
SOURCE ./database/init.sql;
```

**注意**: 由于项目使用 JPA 自动建表（`ddl-auto: update`），首次启动后端服务时会自动创建表结构。

---

### 方案二：本地安装（不使用 Docker）

#### 1. 安装 MySQL 8.0

1. 下载并安装 MySQL 8.0
2. 设置 root 密码为 `root123456`（或修改 `backend/src/main/resources/application.yml` 中的配置）
3. 启动 MySQL 服务

**Windows 启动命令**:
```bash
net start MySQL80
```

**验证 MySQL 运行**:
```bash
mysql -u root -proot123456 -e "SHOW DATABASES;"
```

#### 2. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS ev_charging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. 安装 Redis

**Windows**:
- 下载 Redis for Windows: https://github.com/microsoftarchive/redis/releases
- 解压后运行 `redis-server.exe`

**验证 Redis 运行**:
```bash
redis-cli ping
# 应返回 PONG
```

---

## 后端服务启动

### 1. 进入后端目录

```bash
cd D:\软工设计code\ev-charging-system\backend
```

### 2. 检查配置文件

确认 `src/main/resources/application.yml` 中的数据库配置正确：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ev_charging?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123456  # 确保与您的MySQL密码一致

  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 安装依赖并启动

```bash
# 下载依赖（首次运行）
mvn clean install

# 启动后端服务
mvn spring-boot:run
```

### 4. 验证后端启动成功

看到以下日志表示启动成功：

```
Started ChargingSystemApplication in X.XXX seconds
```

访问测试接口：
```bash
curl http://localhost:8080/api/stations
```

---

## 前端服务启动

### 1. 配置高德地图 API Key（重要）

#### 申请高德地图 Key

1. 访问 https://console.amap.com/
2. 注册/登录开发者账号
3. 进入「应用管理」→「我的应用」
4. 创建新应用，添加 **Web端（JSAPI）** Key
5. 复制生成的 Key

#### 更新配置

编辑 `frontend-user/index.html`，替换 `your-amap-key-here`:

```html
<script src="https://webapi.amap.com/maps?v=2.0&key=YOUR_ACTUAL_KEY&plugin=AMap.Geolocation"></script>
```

同时更新 `frontend-user/src/utils/map.js`:

```javascript
export const AMAP_KEY = 'YOUR_ACTUAL_KEY'
```

### 2. 安装依赖

```bash
cd D:\软工设计code\ev-charging-system\frontend-user
npm install
```

如果遇到网络问题，可以使用淘宝镜像：

```bash
npm install --registry=https://registry.npmmirror.com
```

### 3. 启动开发服务器

```bash
npm run dev
```

### 4. 访问应用

浏览器访问: http://localhost:5173

---

## 验证系统运行

### 1. 测试后端API

使用浏览器或 Postman 测试以下接口：

```bash
# 获取充电站列表
GET http://localhost:8080/api/stations

# 注册新用户
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "nickname": "测试用户"
}

# 用户登录
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

### 2. 测试前端功能

1. 打开 http://localhost:5173
2. 点击「注册」创建测试账号
3. 登录后查看充电站列表
4. 点击定位按钮（允许浏览器获取位置权限）
5. 查看附近充电站

### 3. 插入测试数据（可选）

如果数据库为空，可以手动插入测试数据：

```sql
USE ev_charging;

-- 插入测试充电站
INSERT INTO charging_stations (name, address, longitude, latitude, phone, business_hours, total_piles, available_piles, status, rating, review_count, create_time, update_time)
VALUES
('北京朝阳充电站', '北京市朝阳区朝阳北路101号', 116.4833, 39.9219, '010-12345678', '00:00-24:00', 20, 15, 'ACTIVE', 4.5, 128, NOW(), NOW()),
('上海浦东充电站', '上海市浦东新区世纪大道1号', 121.5054, 31.2454, '021-87654321', '00:00-24:00', 15, 10, 'ACTIVE', 4.3, 96, NOW(), NOW()),
('深圳南山充电站', '广东省深圳市南山区科技园', 113.9419, 22.5346, '0755-88888888', '00:00-24:00', 25, 20, 'ACTIVE', 4.7, 215, NOW(), NOW());
```

---

## 常见问题

### Q1: 后端启动报错 "Could not connect to MySQL"

**原因**: MySQL 服务未启动或连接配置错误

**解决方案**:

1. 检查 MySQL 是否运行:
   ```bash
   # Windows
   net start MySQL80

   # 或使用 Docker
   docker-compose ps
   ```

2. 验证密码是否正确:
   ```bash
   mysql -u root -proot123456
   ```

3. 检查 `application.yml` 中的配置是否与实际一致

---

### Q2: 前端启动后地图无法加载

**原因**: 高德地图 API Key 未配置或无效

**解决方案**:

1. 确认已在 `index.html` 和 `map.js` 中配置了正确的 Key
2. 检查浏览器控制台错误信息
3. 访问高德地图控制台确认 Key 是否有效
4. 确保 Key 类型为「Web端（JSAPI）」

---

### Q3: Redis 连接失败

**原因**: Redis 服务未启动

**解决方案**:

```bash
# 使用 Docker
docker-compose start redis

# 或手动启动 Redis
redis-server

# 验证 Redis 运行
redis-cli ping
```

---

### Q4: Maven 下载依赖缓慢

**解决方案**: 配置阿里云镜像

编辑 `~/.m2/settings.xml`:

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

### Q5: npm install 报错

**解决方案**:

```bash
# 清除缓存
npm cache clean --force

# 使用淘宝镜像
npm install --registry=https://registry.npmmirror.com

# 或全局设置淘宝镜像
npm config set registry https://registry.npmmirror.com
```

---

### Q6: 端口被占用

**检查端口占用**:

```bash
# Windows 查看端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :5173

# 结束占用进程
taskkill /PID <进程ID> /F
```

---

## 📚 下一步

系统启动成功后，您可以：

1. 阅读 [README.md](./README.md) 了解完整功能
2. 查看 [PROJECT_FRAMEWORK.md](./PROJECT_FRAMEWORK.md) 了解项目结构
3. 根据 [第一部分_基础架构与用户端(1).md](./第一部分_基础架构与用户端(1).md) 继续开发

---

## 🆘 获取帮助

如果遇到其他问题：

1. 检查系统日志和控制台输出
2. 查看各子项目的 README.md
3. 提交 Issue 到项目仓库

---

**最后更新**: 2025-12-01
**版本**: 1.0.0
