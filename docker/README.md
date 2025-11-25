# Docker 环境配置说明

## 概述

本项目使用 Docker Compose 编排所有服务，包括基础设施服务（MySQL、Redis、RabbitMQ）和应用服务（后端、AI服务、前端）。

## 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                  (ev-network: 172.20.0.0/16)            │
│                                                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │   MySQL    │  │   Redis    │  │  RabbitMQ  │       │
│  │   :3306    │  │   :6379    │  │  :5672     │       │
│  └────────────┘  └────────────┘  │  :15672    │       │
│                                    └────────────┘       │
│         ▲              ▲                ▲               │
│         │              │                │               │
│         └──────────────┼────────────────┘               │
│                        │                                 │
│                   ┌────┴────┐                           │
│                   │ Backend │                           │
│                   │  :8080  │                           │
│                   └────┬────┘                           │
│                        │                                 │
│              ┌─────────┼─────────┐                      │
│              │                   │                       │
│         ┌────▼────┐         ┌───▼────┐                 │
│         │AI Service│         │Frontend│                 │
│         │  :5000  │         │User:5173│                 │
│         └─────────┘         │Admin:5174                 │
│                             └─────────┘                 │
└─────────────────────────────────────────────────────────┘
```

## 服务列表

| 服务名称 | 镜像 | 端口映射 | 用途 |
|---------|------|---------|------|
| mysql | mysql:8.0 | 3306:3306 | 主数据库 |
| redis | redis:7-alpine | 6379:6379 | 缓存服务 |
| rabbitmq | rabbitmq:3.12-management-alpine | 5672:5672, 15672:15672 | 消息队列 |
| backend | 自定义构建 | 8080:8080 | Spring Boot 后端 |
| ai-service | 自定义构建 | 5000:5000 | Python AI 预测服务 |
| frontend-user | 自定义构建 | 5173:5173 | Vue3 用户移动端 |
| frontend-admin | 自定义构建 | 5174:5174 | Vue3 管理后台 |

## 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 初始化配置

1. **复制环境变量文件**:
   ```bash
   cp .env.example .env
   ```

2. **修改 .env 文件**（建议修改密码）:
   ```bash
   # 编辑 .env 文件，设置安全密码
   nano .env
   ```

3. **准备数据库初始化脚本**:
   确保 `docker/mysql/init/01-init.sql` 文件存在

### 启动服务

**启动所有服务**:
```bash
docker-compose up -d
```

**仅启动基础设施服务**:
```bash
docker-compose up -d mysql redis rabbitmq
```

**查看服务状态**:
```bash
docker-compose ps
```

**查看服务日志**:
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f ai-service
```

### 停止服务

**停止所有服务**:
```bash
docker-compose down
```

**停止并删除数据卷**:
```bash
docker-compose down -v
```

### 重新构建服务

**重新构建所有应用服务**:
```bash
docker-compose up -d --build
```

**重新构建特定服务**:
```bash
docker-compose up -d --build backend
docker-compose up -d --build ai-service
```

## 健康检查

所有服务都配置了健康检查机制：

### 自动健康检查

Docker 会自动执行健康检查，可以通过以下命令查看：
```bash
docker-compose ps
```

状态说明：
- `(healthy)` - 服务健康
- `(health: starting)` - 健康检查启动中
- `(unhealthy)` - 服务不健康

### 手动健康检查

运行健康检查脚本：
```bash
bash docker/health-check.sh
```

或者手动检查各服务：

**后端服务**:
```bash
curl http://localhost:8080/api/health
```

**AI 服务**:
```bash
curl http://localhost:5000/health
```

**前端服务**:
```bash
curl http://localhost:5173/
curl http://localhost:5174/
```

**MySQL**:
```bash
docker exec ev-charging-mysql mysqladmin ping -h localhost -u root -p
```

**Redis**:
```bash
docker exec ev-charging-redis redis-cli -a <password> ping
```

**RabbitMQ**:
```bash
# 访问管理界面
open http://localhost:15672/
# 默认账号: admin / admin123
```

## 数据持久化

所有重要数据都使用 Docker 数据卷持久化：

| 数据卷名称 | 用途 |
|-----------|------|
| mysql-data | MySQL 数据库文件 |
| redis-data | Redis 持久化数据 |
| rabbitmq-data | RabbitMQ 队列数据 |
| backend-logs | 后端服务日志 |
| ai-logs | AI 服务日志 |

**查看数据卷**:
```bash
docker volume ls | grep ev-charging
```

**备份数据卷**:
```bash
# 备份 MySQL 数据
docker run --rm -v ev-charging-system_mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data
```

**恢复数据卷**:
```bash
# 恢复 MySQL 数据
docker run --rm -v ev-charging-system_mysql-data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-backup.tar.gz -C /
```

## 网络配置

### 自定义网络

项目使用自定义桥接网络 `ev-network`，子网为 `172.20.0.0/16`。

**查看网络信息**:
```bash
docker network inspect ev-charging-system_ev-network
```

### 服务间通信

服务间可以通过服务名称直接通信：

- 后端连接 MySQL: `mysql:3306`
- 后端连接 Redis: `redis:6379`
- 后端连接 RabbitMQ: `rabbitmq:5672`
- 后端调用 AI 服务: `http://ai-service:5000`

## 资源限制

### 资源配额

| 服务 | CPU限制 | 内存限制 | CPU预留 | 内存预留 |
|------|---------|---------|---------|---------|
| MySQL | 2核 | 1GB | 0.5核 | 512MB |
| Redis | 1核 | 512MB | 0.25核 | 256MB |
| RabbitMQ | 1核 | 512MB | 0.25核 | 256MB |
| Backend | 2核 | 1.5GB | 0.5核 | 768MB |
| AI Service | 1核 | 1GB | 0.25核 | 512MB |
| Frontend | 1核 | 512MB | 0.25核 | 256MB |

**总计**:
- CPU: 最多 8核，预留 2核
- 内存: 最多 4.5GB，预留 2.5GB

### 调整资源限制

编辑 `docker-compose.yml` 文件中的 `deploy.resources` 部分：

```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

## 日志管理

### 日志配置

所有服务都配置了日志轮转：
- 单个日志文件最大: 10MB (基础设施) / 50MB (应用)
- 保留日志文件数: 3个 (基础设施) / 5个 (应用)

### 查看日志

**实时查看日志**:
```bash
docker-compose logs -f <service-name>
```

**查看最近日志**:
```bash
docker-compose logs --tail=100 <service-name>
```

**导出日志**:
```bash
docker-compose logs <service-name> > logs.txt
```

## 故障排查

### 常见问题

#### 1. 端口被占用

**错误信息**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

**解决方案**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :3306  # Windows
lsof -i :3306                 # Linux/Mac

# 修改 .env 文件中的端口配置
MYSQL_PORT=3307
```

#### 2. 容器启动失败

**查看容器日志**:
```bash
docker logs <container-name>
```

**重启容器**:
```bash
docker-compose restart <service-name>
```

#### 3. 数据库连接失败

**检查 MySQL 是否启动**:
```bash
docker-compose ps mysql
```

**检查数据库初始化**:
```bash
docker exec -it ev-charging-mysql mysql -u root -p -e "SHOW DATABASES;"
```

#### 4. 内存不足

**清理未使用的镜像和容器**:
```bash
docker system prune -a
```

**查看资源使用情况**:
```bash
docker stats
```

### 完全重置

如果遇到无法解决的问题，可以完全重置：

```bash
# 1. 停止所有服务
docker-compose down

# 2. 删除所有数据卷
docker-compose down -v

# 3. 删除构建的镜像
docker rmi $(docker images 'ev-charging-system*' -q)

# 4. 重新启动
docker-compose up -d --build
```

## 安全建议

### 生产环境部署

1. **修改默认密码**:
   - 在 `.env` 文件中设置强密码
   - 使用 `openssl rand -base64 32` 生成随机密码

2. **限制网络访问**:
   - 不要将数据库端口暴露到公网
   - 使用防火墙规则限制访问

3. **启用 TLS/SSL**:
   - 为 MySQL 启用 SSL
   - 为 Redis 启用 TLS
   - 前端使用 HTTPS

4. **定期备份**:
   - 备份 MySQL 数据
   - 备份配置文件
   - 备份应用日志

5. **监控和告警**:
   - 使用 Prometheus + Grafana 监控
   - 配置日志聚合（ELK/Loki）
   - 设置告警规则

## 性能优化

### MySQL 优化

1. **配置优化**（在 `docker/mysql/my.cnf`）:
   ```ini
   [mysqld]
   max_connections = 500
   innodb_buffer_pool_size = 512M
   query_cache_size = 64M
   ```

2. **索引优化**:
   - 检查慢查询日志
   - 为常用字段创建索引

### Redis 优化

1. **内存策略**:
   - 已配置 `maxmemory 256mb`
   - 已配置 `maxmemory-policy allkeys-lru`

2. **持久化**:
   - 已启用 AOF 持久化

### 应用服务优化

1. **后端 JVM 参数** (已配置):
   ```
   -Xms512m -Xmx1024m
   -XX:+UseG1GC
   -XX:MaxGCPauseMillis=200
   ```

2. **前端生产构建**:
   ```bash
   # 修改 Dockerfile 使用生产构建
   RUN npm run build
   CMD ["npm", "run", "preview"]
   ```

## 更新日志

### 2025-12-15

- ✅ 优化 .env 配置文件，添加缺失的环境变量
- ✅ 修复后端健康检查路径（`/health` -> `/api/health`）
- ✅ 为 AI 服务添加 wget 依赖
- ✅ 移除 docker-compose.yml 中过时的 version 字段
- ✅ 创建后端健康检查控制器
- ✅ 优化所有 Dockerfile 的健康检查配置
- ✅ 所有服务成功启动并运行正常

## 相关资源

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [项目主文档](../README.md)
- [数据库设计](../database/ER图设计说明.md)

## 联系支持

如有问题，请参考：
1. 项目 README.md
2. 项目 CLAUDE.md
3. 各部分的 CLAUDE.md 文档
