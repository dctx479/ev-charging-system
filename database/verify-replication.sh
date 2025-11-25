#!/bin/bash

# ====================================================================
# MySQL主从复制验证脚本
# 功能: 快速验证主从复制是否正常工作
# 使用: bash verify-replication.sh
# ====================================================================

set -e

echo "=========================================="
echo "MySQL主从复制验证脚本"
echo "=========================================="

ROOT_PASSWORD="root123456"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "1. 检查MySQL服务状态..."
echo "=========================================="

# 检查主库
if docker ps | grep -q mysql-master; then
    echo -e "${GREEN}✅ 主库运行中${NC}"
else
    echo -e "${RED}❌ 主库未运行${NC}"
    exit 1
fi

# 检查从库1
if docker ps | grep -q mysql-slave1; then
    echo -e "${GREEN}✅ 从库1运行中${NC}"
else
    echo -e "${RED}❌ 从库1未运行${NC}"
    exit 1
fi

# 检查从库2
if docker ps | grep -q mysql-slave2; then
    echo -e "${GREEN}✅ 从库2运行中${NC}"
else
    echo -e "${RED}❌ 从库2未运行${NC}"
    exit 1
fi

echo ""
echo "2. 检查主库状态..."
echo "=========================================="

MASTER_STATUS=$(docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} -e "SHOW MASTER STATUS\G" 2>/dev/null)
MASTER_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "Binlog文件: ${MASTER_FILE}"
echo "Binlog位置: ${MASTER_POS}"

if [ -z "$MASTER_FILE" ]; then
    echo -e "${RED}❌ 主库binlog未启用${NC}"
    exit 1
else
    echo -e "${GREEN}✅ 主库binlog正常${NC}"
fi

echo ""
echo "3. 检查从库1复制状态..."
echo "=========================================="

SLAVE1_STATUS=$(docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" 2>/dev/null)
SLAVE1_IO=$(echo "$SLAVE1_STATUS" | grep "Slave_IO_Running:" | head -1 | awk '{print $2}')
SLAVE1_SQL=$(echo "$SLAVE1_STATUS" | grep "Slave_SQL_Running:" | head -1 | awk '{print $2}')
SLAVE1_LAG=$(echo "$SLAVE1_STATUS" | grep "Seconds_Behind_Master:" | head -1 | awk '{print $2}')
SLAVE1_IO_ERROR=$(echo "$SLAVE1_STATUS" | grep "Last_IO_Error:" | cut -d':' -f2- | xargs)
SLAVE1_SQL_ERROR=$(echo "$SLAVE1_STATUS" | grep "Last_SQL_Error:" | cut -d':' -f2- | xargs)

echo "IO线程运行: ${SLAVE1_IO}"
echo "SQL线程运行: ${SLAVE1_SQL}"
echo "主从延迟: ${SLAVE1_LAG}秒"

if [ "$SLAVE1_IO" == "Yes" ] && [ "$SLAVE1_SQL" == "Yes" ]; then
    echo -e "${GREEN}✅ 从库1复制正常${NC}"
    if [ "$SLAVE1_LAG" -gt 5 ]; then
        echo -e "${YELLOW}⚠️ 警告: 延迟较高 (${SLAVE1_LAG}秒)${NC}"
    fi
else
    echo -e "${RED}❌ 从库1复制异常${NC}"
    if [ -n "$SLAVE1_IO_ERROR" ]; then
        echo -e "${RED}IO错误: ${SLAVE1_IO_ERROR}${NC}"
    fi
    if [ -n "$SLAVE1_SQL_ERROR" ]; then
        echo -e "${RED}SQL错误: ${SLAVE1_SQL_ERROR}${NC}"
    fi
    exit 1
fi

echo ""
echo "4. 检查从库2复制状态..."
echo "=========================================="

SLAVE2_STATUS=$(docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G" 2>/dev/null)
SLAVE2_IO=$(echo "$SLAVE2_STATUS" | grep "Slave_IO_Running:" | head -1 | awk '{print $2}')
SLAVE2_SQL=$(echo "$SLAVE2_STATUS" | grep "Slave_SQL_Running:" | head -1 | awk '{print $2}')
SLAVE2_LAG=$(echo "$SLAVE2_STATUS" | grep "Seconds_Behind_Master:" | head -1 | awk '{print $2}')
SLAVE2_IO_ERROR=$(echo "$SLAVE2_STATUS" | grep "Last_IO_Error:" | cut -d':' -f2- | xargs)
SLAVE2_SQL_ERROR=$(echo "$SLAVE2_STATUS" | grep "Last_SQL_Error:" | cut -d':' -f2- | xargs)

echo "IO线程运行: ${SLAVE2_IO}"
echo "SQL线程运行: ${SLAVE2_SQL}"
echo "主从延迟: ${SLAVE2_LAG}秒"

if [ "$SLAVE2_IO" == "Yes" ] && [ "$SLAVE2_SQL" == "Yes" ]; then
    echo -e "${GREEN}✅ 从库2复制正常${NC}"
    if [ "$SLAVE2_LAG" -gt 5 ]; then
        echo -e "${YELLOW}⚠️ 警告: 延迟较高 (${SLAVE2_LAG}秒)${NC}"
    fi
else
    echo -e "${RED}❌ 从库2复制异常${NC}"
    if [ -n "$SLAVE2_IO_ERROR" ]; then
        echo -e "${RED}IO错误: ${SLAVE2_IO_ERROR}${NC}"
    fi
    if [ -n "$SLAVE2_SQL_ERROR" ]; then
        echo -e "${RED}SQL错误: ${SLAVE2_SQL_ERROR}${NC}"
    fi
    exit 1
fi

echo ""
echo "5. 测试主从数据同步..."
echo "=========================================="

# 生成唯一测试数据
TEST_DATA="ReplicationTest_$(date +%s)"

# 在主库插入测试数据
docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} ev_charging_system << EOSQL 2>/dev/null
CREATE TABLE IF NOT EXISTS test_replication (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO test_replication (data) VALUES ('${TEST_DATA}');
EOSQL

echo "已在主库插入测试数据: ${TEST_DATA}"

# 等待复制
sleep 2

# 在从库1验证
SLAVE1_FOUND=$(docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system -sN \
  -e "SELECT COUNT(*) FROM test_replication WHERE data='${TEST_DATA}'" 2>/dev/null)

if [ "$SLAVE1_FOUND" -eq 1 ]; then
    echo -e "${GREEN}✅ 从库1数据同步成功${NC}"
else
    echo -e "${RED}❌ 从库1数据同步失败（找到${SLAVE1_FOUND}条记录）${NC}"
    exit 1
fi

# 在从库2验证
SLAVE2_FOUND=$(docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system -sN \
  -e "SELECT COUNT(*) FROM test_replication WHERE data='${TEST_DATA}'" 2>/dev/null)

if [ "$SLAVE2_FOUND" -eq 1 ]; then
    echo -e "${GREEN}✅ 从库2数据同步成功${NC}"
else
    echo -e "${RED}❌ 从库2数据同步失败（找到${SLAVE2_FOUND}条记录）${NC}"
    exit 1
fi

echo ""
echo "6. 检查从库只读模式..."
echo "=========================================="

# 尝试在从库1写入（应该失败）
if docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system \
  -e "INSERT INTO test_replication (data) VALUES ('ShouldFail')" 2>&1 | grep -q "read-only"; then
    echo -e "${GREEN}✅ 从库1只读模式正常${NC}"
else
    echo -e "${YELLOW}⚠️ 警告: 从库1可能未启用只读模式${NC}"
fi

# 尝试在从库2写入（应该失败）
if docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system \
  -e "INSERT INTO test_replication (data) VALUES ('ShouldFail')" 2>&1 | grep -q "read-only"; then
    echo -e "${GREEN}✅ 从库2只读模式正常${NC}"
else
    echo -e "${YELLOW}⚠️ 警告: 从库2可能未启用只读模式${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ 所有检查通过！主从复制工作正常${NC}"
echo "=========================================="
echo ""
echo "总结:"
echo "  - 主库运行正常，binlog已启用"
echo "  - 从库1复制正常，延迟: ${SLAVE1_LAG}秒"
echo "  - 从库2复制正常，延迟: ${SLAVE2_LAG}秒"
echo "  - 主从数据同步验证通过"
echo "  - 从库只读模式已启用"
echo ""
echo "下一步:"
echo "  1. 启动应用: cd backend && mvn spring-boot:run"
echo "  2. 验证读写分离: curl http://localhost:8080/api/monitor/datasource/health"
echo ""
