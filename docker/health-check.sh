#!/bin/bash
# Docker 服务健康检查脚本
# EV Charging System - Service Health Check

echo "============================================"
echo "  EV Charging System - 服务健康检查"
echo "============================================"
echo ""

# 检查函数
check_service() {
    local service_name=$1
    local url=$2
    local expected_code=${3:-200}

    echo -n "检查 $service_name ... "
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    if [ "$http_code" = "$expected_code" ]; then
        echo "✓ 正常 (HTTP $http_code)"
        return 0
    else
        echo "✗ 异常 (HTTP $http_code, 期望 $expected_code)"
        return 1
    fi
}

# 计数器
success_count=0
total_count=0

# 检查后端服务
total_count=$((total_count + 1))
if check_service "后端服务" "http://localhost:8080/api/health"; then
    success_count=$((success_count + 1))
fi

# 检查 AI 服务
total_count=$((total_count + 1))
if check_service "AI 预测服务" "http://localhost:5000/health"; then
    success_count=$((success_count + 1))
fi

# 检查用户前端
total_count=$((total_count + 1))
if check_service "用户移动端" "http://localhost:5173/"; then
    success_count=$((success_count + 1))
fi

# 检查管理后台
total_count=$((total_count + 1))
if check_service "管理后台" "http://localhost:5174/"; then
    success_count=$((success_count + 1))
fi

# 检查 RabbitMQ 管理界面
total_count=$((total_count + 1))
if check_service "RabbitMQ 管理界面" "http://localhost:15672/"; then
    success_count=$((success_count + 1))
fi

echo ""
echo "============================================"
echo "  数据库服务检查"
echo "============================================"
echo ""

# 检查 MySQL
total_count=$((total_count + 1))
echo -n "检查 MySQL ... "
if docker exec ev-charging-mysql mysqladmin ping -h localhost -u root -proot123456 > /dev/null 2>&1; then
    echo "✓ 正常"
    success_count=$((success_count + 1))
else
    echo "✗ 异常"
fi

# 检查 Redis
total_count=$((total_count + 1))
echo -n "检查 Redis ... "
redis_response=$(docker exec ev-charging-redis redis-cli -a redis123456 ping 2>/dev/null)
if [ "$redis_response" = "PONG" ]; then
    echo "✓ 正常"
    success_count=$((success_count + 1))
else
    echo "✗ 异常"
fi

echo ""
echo "============================================"
echo "  容器状态"
echo "============================================"
echo ""
docker-compose ps --format table

echo ""
echo "============================================"
echo "  检查结果汇总"
echo "============================================"
echo ""
echo "成功: $success_count / $total_count"

if [ $success_count -eq $total_count ]; then
    echo "状态: ✓ 所有服务正常运行"
    exit 0
else
    echo "状态: ✗ 部分服务异常"
    exit 1
fi
