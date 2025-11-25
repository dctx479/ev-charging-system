#!/bin/bash

###############################################################################
# EV Charging System - 监控系统部署脚本
# 用途: 部署 Prometheus, Grafana, Alertmanager 监控栈
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

log_info "=========================================="
log_info "  EV Charging System - 监控系统部署"
log_info "=========================================="

# 1. 检查 Docker 和 Docker Compose
log_info "检查 Docker 环境..."
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

log_info "Docker 版本: $(docker --version)"
log_info "Docker Compose 版本: $(docker-compose --version)"

# 2. 检查配置文件
log_info "检查配置文件..."

required_files=(
    "prometheus/prometheus.yml"
    "prometheus/rules/alerts.yml"
    "alertmanager/alertmanager.yml"
    "grafana/provisioning/datasources/prometheus.yml"
    "grafana/provisioning/dashboards/dashboards.yml"
    "grafana/dashboards/ev-charging-dashboard.json"
    "docker-compose.yml"
)

for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        log_error "配置文件不存在: $file"
        exit 1
    fi
done

log_info "配置文件检查通过"

# 3. 创建环境变量文件
if [ ! -f ".env" ]; then
    log_warn ".env 文件不存在，从 .env.example 复制..."
    cp .env.example .env
    log_info "已创建 .env 文件，请根据实际情况修改配置"
fi

# 4. 停止旧容器（如果存在）
log_info "停止旧容器..."
docker-compose down 2>/dev/null || true

# 5. 拉取最新镜像
log_info "拉取最新镜像..."
docker-compose pull

# 6. 启动监控栈
log_info "启动监控服务..."
docker-compose up -d

# 7. 等待服务启动
log_info "等待服务启动..."
sleep 10

# 8. 验证服务状态
log_info "验证服务状态..."

services=(
    "prometheus:9090:-/healthy"
    "grafana:3000:api/health"
    "alertmanager:9093:-/healthy"
)

all_healthy=true

for service_info in "${services[@]}"; do
    IFS=':' read -r service port path <<< "$service_info"

    log_info "检查 $service..."

    max_retries=30
    retry=0

    while [ $retry -lt $max_retries ]; do
        if curl -sf "http://localhost:$port/$path" > /dev/null 2>&1; then
            log_info "$service 服务正常 ✓"
            break
        fi

        retry=$((retry+1))
        if [ $retry -eq $max_retries ]; then
            log_error "$service 服务启动失败 ✗"
            all_healthy=false
            break
        fi

        sleep 2
    done
done

# 9. 显示服务访问地址
log_info ""
log_info "=========================================="
log_info "  监控系统部署完成"
log_info "=========================================="
log_info ""
log_info "服务访问地址:"
log_info "  Prometheus:    http://localhost:9090"
log_info "  Grafana:       http://localhost:3000 (admin/admin123)"
log_info "  Alertmanager:  http://localhost:9093"
log_info ""
log_info "Exporter 指标地址:"
log_info "  MySQL Exporter:  http://localhost:9104/metrics"
log_info "  Redis Exporter:  http://localhost:9121/metrics"
log_info "  Node Exporter:   http://localhost:9100/metrics"
log_info ""
log_info "后端应用指标:"
log_info "  Spring Boot:     http://localhost:8080/api/actuator/prometheus"
log_info ""

# 10. 检查告警规则
log_info "验证 Prometheus 告警规则..."
if curl -sf "http://localhost:9090/api/v1/rules" | grep -q "alerts.yml"; then
    log_info "告警规则加载成功 ✓"
else
    log_warn "告警规则可能未正确加载，请检查 Prometheus 日志"
fi

# 11. 显示容器状态
log_info ""
log_info "容器状态:"
docker-compose ps

# 12. 提示后续操作
log_info ""
log_info "后续操作:"
log_info "  1. 访问 Grafana (http://localhost:3000) 查看监控 Dashboard"
log_info "  2. 确认后端应用已启动并暴露 /actuator/prometheus 端点"
log_info "  3. 在 Alertmanager 中配置告警通知渠道（邮件/钉钉/企业微信）"
log_info "  4. 根据需要调整 prometheus/rules/alerts.yml 中的告警阈值"
log_info ""

if [ "$all_healthy" = false ]; then
    log_warn "部分服务启动失败，请检查日志:"
    log_warn "  docker-compose logs <service-name>"
    exit 1
fi

log_info "部署成功！ ✓"
