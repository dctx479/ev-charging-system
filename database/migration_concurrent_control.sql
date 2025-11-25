-- ============================================
-- 并发控制优化 - 数据库迁移脚本
-- ============================================
-- 用途：为 charge_order 表添加乐观锁版本字段
-- 创建时间：2025-12-14
-- ============================================

USE ev_charging_system;

-- 1. 为 charge_order 表添加 version 字段（乐观锁）
ALTER TABLE charge_order
ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号'
AFTER update_time;

-- 2. 为现有数据初始化 version 字段
UPDATE charge_order SET version = 0 WHERE version IS NULL;

-- 3. 创建索引以优化并发查询
-- 为充电桩ID和订单状态创建组合索引（用于快速检查充电桩是否被占用）
CREATE INDEX idx_pile_status ON charge_order(pile_id, order_status);

-- 为用户ID和订单状态创建组合索引（用于快速检查用户是否有进行中的订单）
CREATE INDEX idx_user_status ON charge_order(user_id, order_status);

-- ============================================
-- 验证SQL
-- ============================================
-- 查看表结构
DESC charge_order;

-- 查看索引
SHOW INDEX FROM charge_order;

-- 测试查询性能
EXPLAIN SELECT * FROM charge_order WHERE pile_id = 1 AND order_status = 0;
EXPLAIN SELECT * FROM charge_order WHERE user_id = 1 AND order_status = 0;
