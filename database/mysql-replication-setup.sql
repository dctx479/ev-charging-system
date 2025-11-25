-- ====================================================================
-- MySQL主从复制配置SQL脚本
-- 功能: 配置1主2从的MySQL主从复制架构
-- 执行顺序: 
--   1. 在主库执行 MASTER 部分
--   2. 在从库1执行 SLAVE1 部分
--   3. 在从库2执行 SLAVE2 部分
-- ====================================================================

-- ====================================================================
-- MASTER 配置（在主库执行）
-- ====================================================================

-- 1. 创建复制用户
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl_password_2026';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- 2. 创建只读用户（供应用连接从库使用）
CREATE USER IF NOT EXISTS 'readonly'@'%' IDENTIFIED BY 'readonly_password_2026';
GRANT SELECT ON ev_charging_system.* TO 'readonly'@'%';
FLUSH PRIVILEGES;

-- 3. 锁定主库，准备备份（可选，生产环境慎用）
-- FLUSH TABLES WITH READ LOCK;

-- 4. 查看主库状态，记录File和Position
SHOW MASTER STATUS;
-- 记录输出：
-- +------------------+----------+--------------+------------------+
-- | File             | Position | Binlog_Do_DB | Binlog_Ignore_DB |
-- +------------------+----------+--------------+------------------+
-- | mysql-bin.000001 |      154 |              |                  |
-- +------------------+----------+--------------+------------------+

-- 5. 导出数据（在shell执行）
-- mysqldump -uroot -p --all-databases --master-data=2 > master_backup.sql

-- 6. 解锁主库
-- UNLOCK TABLES;

-- ====================================================================
-- SLAVE1 配置（在从库1执行）
-- ====================================================================

-- 1. 导入主库数据（在shell执行）
-- mysql -uroot -p < master_backup.sql

-- 2. 配置主从复制
-- 注意：替换MASTER_LOG_FILE和MASTER_LOG_POS为主库实际值
CHANGE MASTER TO
  MASTER_HOST='mysql-master',           -- 主库主机（Docker环境使用容器名）
  MASTER_PORT=3306,                     -- 主库端口
  MASTER_USER='repl',                   -- 复制用户
  MASTER_PASSWORD='repl_password_2026', -- 复制密码
  MASTER_LOG_FILE='mysql-bin.000001',   -- 主库binlog文件
  MASTER_LOG_POS=154;                   -- 主库binlog位置

-- 3. 启动主从复制
START SLAVE;

-- 4. 查看从库状态
SHOW SLAVE STATUS\G
-- 重点关注：
-- Slave_IO_Running: Yes  (IO线程是否运行)
-- Slave_SQL_Running: Yes (SQL线程是否运行)
-- Seconds_Behind_Master: 0 (主从延迟秒数)
-- Last_IO_Error: (IO错误信息，应为空)
-- Last_SQL_Error: (SQL错误信息，应为空)

-- 5. 设置只读模式（防止误写入从库）
SET GLOBAL read_only = ON;
SET GLOBAL super_read_only = ON;

-- ====================================================================
-- SLAVE2 配置（在从库2执行）
-- ====================================================================

-- 与SLAVE1配置相同，重复上述步骤

-- ====================================================================
-- 验证脚本（在任意从库执行）
-- ====================================================================

-- 1. 检查复制状态
SHOW SLAVE STATUS\G

-- 2. 检查主从延迟
SELECT 
    CASE 
        WHEN Slave_IO_Running = 'Yes' AND Slave_SQL_Running = 'Yes' 
        THEN '正常'
        ELSE '异常'
    END AS 复制状态,
    Seconds_Behind_Master AS 延迟秒数,
    Last_IO_Error AS IO错误,
    Last_SQL_Error AS SQL错误
FROM (SELECT 1) AS dummy;

-- 3. 在主库插入测试数据
-- INSERT INTO test_replication (data) VALUES ('test1');

-- 4. 在从库查询，验证数据已同步
-- SELECT * FROM test_replication ORDER BY id DESC LIMIT 1;

-- ====================================================================
-- 故障处理脚本
-- ====================================================================

-- 1. 停止主从复制
STOP SLAVE;

-- 2. 重置主从复制
RESET SLAVE ALL;

-- 3. 跳过一个错误事务（慎用）
-- SET GLOBAL sql_slave_skip_counter = 1;
-- START SLAVE;

-- 4. 查看复制错误详情
-- SHOW SLAVE STATUS\G

-- ====================================================================
-- 主从切换脚本（从库提升为主库）
-- ====================================================================

-- 在被提升的从库执行：

-- 1. 停止主从复制
STOP SLAVE;

-- 2. 重置从库状态
RESET SLAVE ALL;

-- 3. 关闭只读模式
SET GLOBAL read_only = OFF;
SET GLOBAL super_read_only = OFF;

-- 4. 创建复制用户（如果未创建）
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl_password_2026';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- 5. 查看新主库状态
SHOW MASTER STATUS;

-- 在其他从库执行：

-- 1. 停止复制
STOP SLAVE;

-- 2. 指向新主库
CHANGE MASTER TO
  MASTER_HOST='new-master-host',
  MASTER_PORT=3306,
  MASTER_USER='repl',
  MASTER_PASSWORD='repl_password_2026',
  MASTER_LOG_FILE='mysql-bin.000001',  -- 新主库的binlog文件
  MASTER_LOG_POS=154;                  -- 新主库的binlog位置

-- 3. 启动复制
START SLAVE;

-- 4. 验证状态
SHOW SLAVE STATUS\G

-- ====================================================================
-- 性能优化配置
-- ====================================================================

-- 从库：启用并行复制（MySQL 5.7+）
-- SET GLOBAL slave_parallel_type = 'LOGICAL_CLOCK';
-- SET GLOBAL slave_parallel_workers = 4;

-- 从库：跳过冗余的binlog事件（提升性能）
-- SET GLOBAL slave_skip_errors = 1062,1032;  -- 慎用：跳过重复键和找不到行的错误

-- 主库：优化binlog刷盘策略（平衡性能和安全）
-- SET GLOBAL sync_binlog = 100;  -- 每100次事务刷盘一次
-- SET GLOBAL innodb_flush_log_at_trx_commit = 2;  -- 每秒刷盘一次

-- ====================================================================
-- 监控查询
-- ====================================================================

-- 查询当前binlog位置
SHOW MASTER STATUS;

-- 查询从库延迟
SELECT 
    TIMESTAMPDIFF(SECOND, 
        STR_TO_DATE(SUBSTRING_INDEX(SUBSTRING_INDEX(Master_Log_File, '.', -1), '-', -1), '%Y%m%d%H%i%s'),
        NOW()
    ) AS estimated_lag_seconds
FROM information_schema.SLAVE_STATUS;

-- 查询从库连接数
SHOW PROCESSLIST;

-- 查询复制过滤规则
SHOW SLAVE HOSTS;
