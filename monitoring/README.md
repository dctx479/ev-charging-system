# EV Charging System - 监控系统文档

## 概述

本监控系统为充电站点管理系统提供完整的可观测性，包括指标监控、可视化Dashboard和告警通知。

### 架构组件

```
┌─────────────────────────────────────────────────────────────────┐
│                         监控系统架构                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │   Spring     │───▶│  Prometheus  │───▶│   Grafana    │    │
│  │   Boot App   │    │              │    │  Dashboards  │    │
│  └──────────────┘    └──────┬───────┘    └──────────────┘    │
│         │                   │                                  │
│         │                   │                                  │
│  ┌──────▼──────┐    ┌──────▼───────┐    ┌──────────────┐    │
│  │  Actuator   │    │ Alertmanager │───▶│   钉钉/邮件  │    │
│  │  /metrics   │    │   告警管理    │    │   告警通知   │    │
│  └─────────────┘    └──────────────┘    └──────────────┘    │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │   MySQL      │    │    Redis     │    │  RabbitMQ    │    │
│  │   Exporter   │    │   Exporter   │    │   Metrics    │    │
│  └──────────────┘    └──────────────┘    └──────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 核心功能

1. **指标采集**
   - Spring Boot Actuator 指标
   - 自定义业务指标（订单、充电桩、用户等）
   - 系统资源指标（CPU、内存、磁盘）
   - 数据库指标（MySQL、Redis）
   - 消息队列指标（RabbitMQ）

2. **可视化监控**
   - 系统概览 Dashboard
   - 业务指标 Dashboard
   - JVM 性能监控
   - 数据库性能监控

3. **智能告警**
   - 多级告警规则（Critical/Warning/Info）
   - 告警分组和抑制
   - 多渠道通知（邮件、钉钉、企业微信）
   - 告警自动恢复通知

## 快速开始

### 前置条件

- Docker 20.10+
- Docker Compose 1.29+
- 充电系统后端已启动（端口 8080）

### 部署步骤

1. **配置环境变量**

```bash
cd monitoring
cp .env.example .env
# 编辑 .env 文件，配置数据库连接等信息
```

2. **启动监控栈**

```bash
chmod +x deploy.sh
./deploy.sh
```

3. **访问监控服务**

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)
- Alertmanager: http://localhost:9093

4. **验证监控数据**

访问 Grafana，查看 "EV Charging System - 业务监控" Dashboard。

## 服务说明

### Prometheus

**端口**: 9090

**功能**: 时序数据库，存储和查询监控指标

**配置文件**:
- `prometheus/prometheus.yml` - 主配置
- `prometheus/rules/alerts.yml` - 告警规则

**数据保留**: 30天（可在 docker-compose.yml 中调整）

**常用操作**:
```bash
# 重载配置（无需重启）
curl -X POST http://localhost:9090/-/reload

# 查看目标状态
curl http://localhost:9090/api/v1/targets

# 查看告警规则
curl http://localhost:9090/api/v1/rules
```

### Grafana

**端口**: 3000

**默认凭证**: admin/admin123

**功能**: 可视化 Dashboard 和图表展示

**预置 Dashboard**:
1. **EV Charging System - 业务监控**
   - QPS 和响应时间
   - 错误率趋势
   - JVM 内存使用
   - 业务指标（订单、充电桩、营收）

**自定义 Dashboard**:
1. 在 Grafana 中创建新 Dashboard
2. 导出为 JSON
3. 保存到 `grafana/dashboards/` 目录
4. 重启 Grafana 生效

### Alertmanager

**端口**: 9093

**功能**: 告警管理和通知分发

**配置文件**: `alertmanager/alertmanager.yml`

**告警路由**:
- Critical 告警 → 邮件 + 钉钉 + 企业微信（30分钟重复）
- Warning 告警 → 邮件 + 钉钉（2小时重复）
- Info 告警 → Webhook（6小时重复）

**配置告警通知**:

1. **邮件通知**

编辑 `alertmanager/alertmanager.yml`:
```yaml
global:
  smtp_from: 'alertmanager@ev-charging.com'
  smtp_smarthost: 'smtp.example.com:587'
  smtp_auth_username: 'alertmanager@ev-charging.com'
  smtp_auth_password: 'your-password'
```

2. **钉钉机器人**

在 `.env` 中设置 `DINGTALK_TOKEN`，或直接在 `alertmanager.yml` 中配置:
```yaml
webhook_configs:
  - url: 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN'
```

3. **企业微信机器人**

在 `.env` 中设置 `WECHAT_WEBHOOK_KEY`，或直接在 `alertmanager.yml` 中配置:
```yaml
webhook_configs:
  - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
```

### Exporters

#### MySQL Exporter

**端口**: 9104

**采集指标**:
- 连接数、QPS
- 慢查询统计
- InnoDB 指标
- 主从复制状态

**配置**: 在 `.env` 中设置 `MYSQL_EXPORTER_DSN`

#### Redis Exporter

**端口**: 9121

**采集指标**:
- 内存使用
- 命中率
- 连接数
- 持久化状态

**配置**: 在 `.env` 中设置 `REDIS_ADDR` 和 `REDIS_PASSWORD`

#### Node Exporter

**端口**: 9100

**采集指标**:
- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络流量

## 自定义业务指标

### 已实现的指标

在 `backend/src/main/java/com/ev/charging/metrics/BusinessMetrics.java` 中定义：

#### 订单指标
- `orders_created_total` - 订单创建总数
- `orders_completed_total` - 订单完成总数
- `orders_failed_total` - 订单失败总数
- `orders_cancelled_total` - 订单取消总数
- `orders_duration_seconds` - 订单处理时长

#### 充电桩指标
- `piles_online` - 在线充电桩数量
- `piles_charging` - 正在充电的充电桩数量
- `piles_idle` - 空闲充电桩数量
- `piles_fault` - 故障充电桩数量
- `piles_utilization_rate` - 充电桩利用率

#### 用户指标
- `users_active_total` - 活跃用户数
- `users_new_total` - 新注册用户数

#### 业务指标
- `business_revenue` - 实时营收（元）
- `business_energy_total` - 总充电量（kWh）
- `business_carbon_credits_total` - 碳积分发放总数

#### 队列指标
- `queue_length` - 队列等待人数
- `queue_wait_time_seconds` - 队列等待时长

#### 支付指标
- `payment_success_total` - 支付成功数
- `payment_failed_total` - 支付失败数

### 使用示例

在业务代码中注入 `BusinessMetrics` 并记录指标：

```java
@Service
public class OrderService {

    @Autowired
    private BusinessMetrics businessMetrics;

    public void createOrder(CreateOrderDTO dto) {
        // 业务逻辑
        Order order = new Order();
        // ...

        // 记录指标
        businessMetrics.recordOrderCreated();
    }

    public void completeOrder(Long orderId) {
        long startTime = System.currentTimeMillis();

        // 业务逻辑
        Order order = orderRepository.findById(orderId).orElseThrow();
        // ...

        // 记录指标
        businessMetrics.recordOrderCompleted();
        businessMetrics.recordOrderDuration(System.currentTimeMillis() - startTime);
        businessMetrics.addRevenue(order.getTotalAmount()); // 分
        businessMetrics.recordEnergy(order.getChargeAmount()); // kWh
    }
}
```

### 添加新指标

1. **在 `BusinessMetrics.java` 中定义指标**

```java
private final Counter myNewMetricCounter;

public BusinessMetrics(MeterRegistry meterRegistry) {
    // ...
    this.myNewMetricCounter = Counter.builder("my_new_metric")
        .description("我的新指标")
        .tag("type", "custom")
        .register(meterRegistry);
}

public void recordMyNewMetric() {
    myNewMetricCounter.increment();
}
```

2. **在业务代码中记录指标**

```java
businessMetrics.recordMyNewMetric();
```

3. **在 Prometheus 中查询**

访问 http://localhost:9090，输入查询：
```promql
my_new_metric_total
```

4. **添加到 Grafana Dashboard**

在 Dashboard 中添加新 Panel，选择 Prometheus 数据源，输入查询表达式。

## 告警规则

### 告警级别

- **Critical**: 严重影响系统可用性，需要立即处理
- **Warning**: 潜在问题，需要关注和计划处理
- **Info**: 信息性告警，用于审计和趋势分析

### 主要告警规则

#### 应用服务告警

1. **ApplicationDown** (Critical)
   - 应用服务宕机超过 1 分钟
   - 立即通知所有渠道

2. **HighErrorRate** (Warning)
   - 5xx 错误率超过 5%，持续 5 分钟
   - 通知开发团队

3. **HighResponseTime** (Warning)
   - P95 响应时间超过 1 秒，持续 5 分钟
   - 通知开发团队

4. **HighJVMMemoryUsage** (Warning)
   - JVM 堆内存使用率超过 85%，持续 5 分钟
   - 通知运维团队

#### 数据库告警

1. **HighMySQLConnections** (Warning)
   - MySQL 连接数超过 100，持续 5 分钟

2. **MySQLReplicationLag** (Critical)
   - MySQL 主从延迟超过 10 秒，持续 2 分钟

3. **LowRedisCacheHitRate** (Warning)
   - Redis 缓存命中率低于 80%，持续 10 分钟

#### 业务告警

1. **HighOrderFailureRate** (Warning)
   - 订单失败率超过 10%，持续 5 分钟

2. **HighPileFaultRate** (Warning)
   - 充电桩故障率超过 10%，持续 10 分钟

3. **HighPaymentFailureRate** (Warning)
   - 支付失败率超过 5%，持续 5 分钟

### 自定义告警规则

编辑 `prometheus/rules/alerts.yml`，添加新规则：

```yaml
groups:
  - name: my-custom-alerts
    interval: 30s
    rules:
      - alert: MyCustomAlert
        expr: my_metric > threshold
        for: 5m
        labels:
          severity: warning
          category: business
        annotations:
          summary: "自定义告警"
          description: "指标 {{ $labels.metric }} 的值为 {{ $value }}"
```

重载 Prometheus 配置：
```bash
curl -X POST http://localhost:9090/-/reload
```

## 运维操作

### 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f prometheus
docker-compose logs -f grafana
docker-compose logs -f alertmanager
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart prometheus
```

### 备份数据

```bash
chmod +x backup.sh
./backup.sh
```

备份内容包括：
- Prometheus 时序数据
- Grafana Dashboard 配置
- Alertmanager 告警状态
- 所有配置文件

备份保存在 `backups/backup_YYYYMMDD_HHMMSS/` 目录。

### 恢复数据

```bash
# 1. 停止服务
./stop.sh

# 2. 解压配置文件
cd backups/backup_YYYYMMDD_HHMMSS/
tar xzf configs.tar.gz -C ../../

# 3. 恢复数据卷
docker volume create monitoring_prometheus-data
docker run --rm \
  -v monitoring_prometheus-data:/target \
  -v $(pwd):/backup \
  alpine sh -c "cd /target && tar xzf /backup/prometheus-data.tar.gz"

# 重复上述步骤恢复 grafana-data 和 alertmanager-data

# 4. 启动服务
cd ../../
./deploy.sh
```

### 停止服务

```bash
chmod +x stop.sh
./stop.sh
```

可选择是否删除数据卷。

### 升级服务

```bash
# 1. 备份当前数据
./backup.sh

# 2. 拉取最新镜像
docker-compose pull

# 3. 重新部署
./deploy.sh
```

## 性能优化

### Prometheus 性能调优

1. **调整采集间隔**

在 `prometheus.yml` 中:
```yaml
global:
  scrape_interval: 15s  # 默认 15 秒，可根据需要调整
```

2. **调整数据保留时间**

在 `docker-compose.yml` 中:
```yaml
command:
  - '--storage.tsdb.retention.time=30d'  # 保留 30 天
  - '--storage.tsdb.retention.size=10GB' # 或限制大小
```

3. **使用 Recording Rules**

对于复杂查询，可以预计算并存储结果。

### Grafana 性能优化

1. **限制时间范围**

避免查询过长时间范围的数据。

2. **使用变量**

在 Dashboard 中使用变量过滤数据。

3. **优化查询**

使用合适的聚合函数和时间窗口。

## 故障排查

### Prometheus 无法采集指标

1. **检查目标状态**

访问 http://localhost:9090/targets，查看目标状态。

2. **检查网络连接**

```bash
docker exec -it ev-charging-prometheus sh
wget -O- http://host.docker.internal:8080/api/actuator/prometheus
```

3. **检查后端配置**

确保后端已配置 Actuator 并暴露 `/actuator/prometheus` 端点。

### Grafana Dashboard 无数据

1. **检查数据源**

在 Grafana 中进入 Configuration → Data Sources，测试 Prometheus 连接。

2. **检查查询**

在 Dashboard Panel 编辑页面，查看查询是否返回数据。

3. **检查时间范围**

确保 Dashboard 时间范围内有数据。

### 告警未触发

1. **检查告警规则**

访问 http://localhost:9090/alerts，查看告警规则状态。

2. **检查 Alertmanager 连接**

在 Prometheus 中进入 Status → Runtime & Build Information，查看 Alertmanager 状态。

3. **检查告警路由**

访问 http://localhost:9093，查看告警路由配置。

### 钉钉/企业微信通知失败

1. **检查 Webhook URL**

在 `alertmanager.yml` 中确认 URL 正确。

2. **测试 Webhook**

```bash
curl -X POST 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"msgtype":"text","text":{"content":"测试消息"}}'
```

3. **检查 Alertmanager 日志**

```bash
docker-compose logs alertmanager
```

## 最佳实践

### 告警设计

1. **避免告警风暴**
   - 使用告警分组（group_by）
   - 配置告警抑制（inhibit_rules）
   - 设置合理的重复间隔

2. **告警分级**
   - Critical: 需要立即处理的紧急问题
   - Warning: 需要关注但非紧急的问题
   - Info: 信息性通知

3. **告警可操作性**
   - 每个告警都应有明确的处理步骤
   - 在 annotations 中提供 runbook_url

### Dashboard 设计

1. **分层展示**
   - 顶层：系统整体健康状态
   - 中层：各子系统关键指标
   - 底层：详细技术指标

2. **使用合适的图表类型**
   - 趋势：时间序列图（Time Series）
   - 当前状态：仪表盘（Gauge）、数字（Stat）
   - 分布：热力图（Heatmap）

3. **设置合理的阈值**
   - 使用颜色标识状态（绿/黄/红）
   - 基于历史数据设置阈值

### 指标采集

1. **遵循命名规范**
   - 使用小写和下划线：`orders_created_total`
   - 包含单位：`_bytes`, `_seconds`, `_total`
   - 使用标签区分维度

2. **控制指标数量**
   - 避免高基数标签（如用户 ID）
   - 使用聚合降低维度

3. **合理采集频率**
   - 业务指标：10-15 秒
   - 系统指标：30-60 秒

## 安全建议

1. **修改默认密码**
   - Grafana 默认密码：admin/admin123
   - 生产环境务必修改

2. **配置访问控制**
   - 限制 Prometheus、Grafana、Alertmanager 访问 IP
   - 使用 HTTPS（通过反向代理）

3. **保护敏感信息**
   - 不要在配置文件中明文存储密码
   - 使用环境变量或密钥管理系统

4. **定期备份**
   - 每天自动备份监控数据
   - 测试备份恢复流程

## 参考资源

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Spring Boot Actuator 指标](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Micrometer Prometheus Registry](https://micrometer.io/docs/registry/prometheus)

## 联系与支持

如有问题，请联系运维团队或提交 Issue。
