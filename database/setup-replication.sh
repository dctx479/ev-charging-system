#!/bin/bash

# ====================================================================
# MySQL主从复制一键配置脚本
# 功能: 自动配置1主2从的MySQL主从复制
# 使用: bash setup-replication.sh
# ====================================================================

set -e

echo "=========================================="
echo "MySQL主从复制配置脚本"
echo "=========================================="

# 配置变量
MASTER_HOST="mysql-master"
SLAVE1_HOST="mysql-slave1"
SLAVE2_HOST="mysql-slave2"
REPL_USER="repl"
REPL_PASSWORD="repl_password_2026"
ROOT_PASSWORD="root123456"

echo ""
echo "步骤1: 等待MySQL服务启动..."
echo "=========================================="

# 等待主库启动
until docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} -e "SELECT 1" > /dev/null 2>&1; do
    echo "等待主库启动..."
    sleep 2
done
echo "✅ 主库已启动"

# 等待从库1启动
until docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} -e "SELECT 1" > /dev/null 2>&1; do
    echo "等待从库1启动..."
    sleep 2
done
echo "✅ 从库1已启动"

# 等待从库2启动
until docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} -e "SELECT 1" > /dev/null 2>&1; do
    echo "等待从库2启动..."
    sleep 2
done
echo "✅ 从库2已启动"

echo ""
echo "步骤2: 在主库创建复制用户..."
echo "=========================================="

docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} << EOSQL
CREATE USER IF NOT EXISTS '${REPL_USER}'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
GRANT REPLICATION SLAVE ON *.* TO '${REPL_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL

echo "✅ 复制用户创建成功"

echo ""
echo "步骤3: 获取主库binlog位置..."
echo "=========================================="

# 获取主库状态
MASTER_STATUS=$(docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} -e "SHOW MASTER STATUS\G")
MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "主库binlog文件: ${MASTER_LOG_FILE}"
echo "主库binlog位置: ${MASTER_LOG_POS}"

if [ -z "$MASTER_LOG_FILE" ] || [ -z "$MASTER_LOG_POS" ]; then
    echo "❌ 无法获取主库binlog位置，请检查主库配置"
    exit 1
fi

echo ""
echo "步骤4: 配置从库1..."
echo "=========================================="

docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} << EOSQL
STOP SLAVE;
CHANGE MASTER TO
  MASTER_HOST='${MASTER_HOST}',
  MASTER_PORT=3306,
  MASTER_USER='${REPL_USER}',
  MASTER_PASSWORD='${REPL_PASSWORD}',
  MASTER_LOG_FILE='${MASTER_LOG_FILE}',
  MASTER_LOG_POS=${MASTER_LOG_POS};
START SLAVE;
EOSQL

echo "✅ 从库1配置完成"

echo ""
echo "步骤5: 配置从库2..."
echo "=========================================="

docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} << EOSQL
STOP SLAVE;
CHANGE MASTER TO
  MASTER_HOST='${MASTER_HOST}',
  MASTER_PORT=3306,
  MASTER_USER='${REPL_USER}',
  MASTER_PASSWORD='${REPL_PASSWORD}',
  MASTER_LOG_FILE='${MASTER_LOG_FILE}',
  MASTER_LOG_POS=${MASTER_LOG_POS};
START SLAVE;
EOSQL

echo "✅ 从库2配置完成"

echo ""
echo "步骤6: 验证主从复制状态..."
echo "=========================================="

# 验证从库1
echo "从库1状态:"
SLAVE1_STATUS=$(docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G")
SLAVE1_IO=$(echo "$SLAVE1_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SLAVE1_SQL=$(echo "$SLAVE1_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')
SLAVE1_LAG=$(echo "$SLAVE1_STATUS" | grep "Seconds_Behind_Master:" | awk '{print $2}')

echo "  IO线程: $SLAVE1_IO"
echo "  SQL线程: $SLAVE1_SQL"
echo "  延迟: ${SLAVE1_LAG}秒"

if [ "$SLAVE1_IO" == "Yes" ] && [ "$SLAVE1_SQL" == "Yes" ]; then
    echo "  ✅ 从库1复制正常"
else
    echo "  ❌ 从库1复制异常"
    exit 1
fi

# 验证从库2
echo ""
echo "从库2状态:"
SLAVE2_STATUS=$(docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} -e "SHOW SLAVE STATUS\G")
SLAVE2_IO=$(echo "$SLAVE2_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SLAVE2_SQL=$(echo "$SLAVE2_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')
SLAVE2_LAG=$(echo "$SLAVE2_STATUS" | grep "Seconds_Behind_Master:" | awk '{print $2}')

echo "  IO线程: $SLAVE2_IO"
echo "  SQL线程: $SLAVE2_SQL"
echo "  延迟: ${SLAVE2_LAG}秒"

if [ "$SLAVE2_IO" == "Yes" ] && [ "$SLAVE2_SQL" == "Yes" ]; then
    echo "  ✅ 从库2复制正常"
else
    echo "  ❌ 从库2复制异常"
    exit 1
fi

echo ""
echo "步骤7: 测试主从同步..."
echo "=========================================="

# 在主库插入测试数据
docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} ev_charging_system << EOSQL
CREATE TABLE IF NOT EXISTS test_replication (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO test_replication (data) VALUES ('Replication Test at $(date)');
EOSQL

# 等待复制
sleep 2

# 在从库1验证
SLAVE1_COUNT=$(docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system -sN -e "SELECT COUNT(*) FROM test_replication")
echo "从库1测试数据条数: ${SLAVE1_COUNT}"

# 在从库2验证
SLAVE2_COUNT=$(docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} ev_charging_system -sN -e "SELECT COUNT(*) FROM test_replication")
echo "从库2测试数据条数: ${SLAVE2_COUNT}"

if [ "$SLAVE1_COUNT" -gt 0 ] && [ "$SLAVE2_COUNT" -gt 0 ]; then
    echo "✅ 主从同步测试成功"
else
    echo "❌ 主从同步测试失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ MySQL主从复制配置完成！"
echo "=========================================="
echo ""
echo "快速命令:"
echo "  查看主库状态: docker exec mysql-master mysql -uroot -p${ROOT_PASSWORD} -e 'SHOW MASTER STATUS'"
echo "  查看从库1状态: docker exec mysql-slave1 mysql -uroot -p${ROOT_PASSWORD} -e 'SHOW SLAVE STATUS\G'"
echo "  查看从库2状态: docker exec mysql-slave2 mysql -uroot -p${ROOT_PASSWORD} -e 'SHOW SLAVE STATUS\G'"
echo ""
