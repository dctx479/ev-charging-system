#!/bin/bash

###############################################################################
# EV Charging System - 监控数据备份脚本
# 用途: 备份 Prometheus、Grafana、Alertmanager 数据
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

# 备份目录
BACKUP_DIR="$SCRIPT_DIR/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="$BACKUP_DIR/backup_$TIMESTAMP"

log_info "=========================================="
log_info "  EV Charging System - 监控数据备份"
log_info "=========================================="

# 创建备份目录
mkdir -p "$BACKUP_PATH"

log_info "备份目录: $BACKUP_PATH"

# 1. 备份 Prometheus 数据
log_info "备份 Prometheus 数据..."
if docker volume inspect monitoring_prometheus-data &> /dev/null; then
    docker run --rm \
        -v monitoring_prometheus-data:/source:ro \
        -v "$BACKUP_PATH":/backup \
        alpine \
        tar czf /backup/prometheus-data.tar.gz -C /source .
    log_info "Prometheus 数据备份完成 ✓"
else
    log_warn "Prometheus 数据卷不存在，跳过"
fi

# 2. 备份 Grafana 数据
log_info "备份 Grafana 数据..."
if docker volume inspect monitoring_grafana-data &> /dev/null; then
    docker run --rm \
        -v monitoring_grafana-data:/source:ro \
        -v "$BACKUP_PATH":/backup \
        alpine \
        tar czf /backup/grafana-data.tar.gz -C /source .
    log_info "Grafana 数据备份完成 ✓"
else
    log_warn "Grafana 数据卷不存在，跳过"
fi

# 3. 备份 Alertmanager 数据
log_info "备份 Alertmanager 数据..."
if docker volume inspect monitoring_alertmanager-data &> /dev/null; then
    docker run --rm \
        -v monitoring_alertmanager-data:/source:ro \
        -v "$BACKUP_PATH":/backup \
        alpine \
        tar czf /backup/alertmanager-data.tar.gz -C /source .
    log_info "Alertmanager 数据备份完成 ✓"
else
    log_warn "Alertmanager 数据卷不存在，跳过"
fi

# 4. 备份配置文件
log_info "备份配置文件..."
tar czf "$BACKUP_PATH/configs.tar.gz" \
    prometheus/ \
    alertmanager/ \
    grafana/ \
    docker-compose.yml \
    .env 2>/dev/null || true
log_info "配置文件备份完成 ✓"

# 5. 创建备份清单
cat > "$BACKUP_PATH/backup_info.txt" <<EOF
备份时间: $TIMESTAMP
备份内容:
  - Prometheus 数据
  - Grafana 数据和 Dashboard
  - Alertmanager 数据
  - 配置文件

恢复方法:
  1. 停止监控服务: ./stop.sh
  2. 解压配置: tar xzf configs.tar.gz
  3. 恢复数据卷:
     docker volume create monitoring_prometheus-data
     docker run --rm -v monitoring_prometheus-data:/target -v $(pwd):/backup alpine sh -c "cd /target && tar xzf /backup/prometheus-data.tar.gz"
     (对 grafana-data 和 alertmanager-data 重复相同操作)
  4. 启动监控服务: ./deploy.sh
EOF

# 6. 计算备份大小
BACKUP_SIZE=$(du -sh "$BACKUP_PATH" | cut -f1)
log_info "备份大小: $BACKUP_SIZE"

# 7. 清理旧备份（保留最近 7 天）
log_info "清理超过 7 天的旧备份..."
find "$BACKUP_DIR" -type d -name "backup_*" -mtime +7 -exec rm -rf {} + 2>/dev/null || true

log_info ""
log_info "=========================================="
log_info "  备份完成"
log_info "=========================================="
log_info "备份路径: $BACKUP_PATH"
log_info "备份大小: $BACKUP_SIZE"
log_info ""
