#!/bin/bash

###############################################################################
# EV Charging System - 监控系统停止脚本
# 用途: 停止所有监控服务
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
log_info "  EV Charging System - 停止监控系统"
log_info "=========================================="

# 询问是否删除数据卷
read -p "是否删除数据卷（包括历史指标数据）？[y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_warn "停止服务并删除数据卷..."
    docker-compose down -v
    log_info "服务已停止，数据卷已删除"
else
    log_info "停止服务（保留数据卷）..."
    docker-compose down
    log_info "服务已停止，数据卷已保留"
fi

log_info "监控系统已停止"
