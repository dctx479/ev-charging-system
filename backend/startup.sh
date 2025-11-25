#!/bin/bash

# EV充电站点管理系统 - 后端服务启动脚本
# 版本: 1.0.0
# 用途: 一键启动后端服务及所有依赖服务

set -e

# 配置
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
LOG_FILE="$BACKEND_DIR/startup.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

# 清空日志
> "$LOG_FILE"

# 标题
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  EV充电站点管理系统 - 后端服务启动脚本                        ║"
echo "║  版本: 1.0.0                                                   ║"
echo "║  启动时间: $(date '+%Y-%m-%d %H:%M:%S')                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 检查环境
log_info "正在检查环境..."

# 检查Java
if ! command -v java &> /dev/null; then
    log_error "Java未安装"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | grep "version" | head -1)
log_success "Java已安装: $JAVA_VERSION"

# 检查Maven Wrapper
if [ ! -f "$BACKEND_DIR/mvnw" ]; then
    log_error "Maven Wrapper不存在"
    exit 1
fi
log_success "Maven Wrapper已就绪"

# 检查MySQL
log_info "正在检查MySQL..."
if ! command -v mysql &> /dev/null; then
    log_warn "MySQL命令行工具未安装，跳过连接测试"
else
    if mysql -h localhost -u root -p"${DB_PASSWORD:-root123456}" -e "SELECT 1" > /dev/null 2>&1; then
        log_success "MySQL连接成功"
    else
        log_error "MySQL连接失败，请检查MySQL是否启动"
        exit 1
    fi
fi

# 检查Redis
log_info "正在检查Redis..."
if ! command -v redis-cli &> /dev/null; then
    log_warn "Redis命令行工具未安装，跳过连接测试"
else
    if redis-cli ping > /dev/null 2>&1; then
        log_success "Redis连接成功"
    else
        log_error "Redis连接失败，请检查Redis是否启动"
        exit 1
    fi
fi

# 编译
log_info "正在编译后端项目..."
cd "$BACKEND_DIR"

if ./mvnw clean install -DskipTests -q; then
    log_success "编译成功"
else
    log_error "编译失败"
    exit 1
fi

# 启动应用
log_info "正在启动Spring Boot应用..."
log_info "应用地址: http://localhost:8080"
log_info "Swagger文档: http://localhost:8080/api/swagger-ui.html"
log_info ""
log_info "按 Ctrl+C 停止应用"
log_info ""

# 设置环境变量（如果需要）
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-ev_charging_system}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="${DB_PASSWORD:-root123456}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export JWT_SECRET="${JWT_SECRET:-your-secret-key-min-32-chars-long}"
export SERVER_PORT="${SERVER_PORT:-8080}"

# 启动应用
./mvnw spring-boot:run -q

log_success "应用启动完成"
