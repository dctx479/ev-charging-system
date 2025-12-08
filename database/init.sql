-- ====================================
-- EV Charging Station Management System
-- 数据库初始化脚本
-- ====================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ev_charging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ev_charging;

-- ====================================
-- 表结构由 JPA 自动创建 (ddl-auto: update)
-- 本脚本主要用于插入测试数据
-- ====================================

-- 清空现有测试数据（可选，谨慎使用）
-- TRUNCATE TABLE charging_piles;
-- TRUNCATE TABLE charging_stations;
-- TRUNCATE TABLE users;

-- ====================================
-- 1. 插入测试用户
-- ====================================

-- 密码都是 123456，已使用 BCrypt 加密
-- BCrypt 哈希值: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO users (username, password, nickname, phone, email, user_type, balance, status, create_time, update_time)
VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800138000', 'admin@evcharging.com', 'ADMIN', 10000.00, 'ACTIVE', NOW(), NOW()),
('user001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800138001', 'zhangsan@example.com', 'USER', 500.00, 'ACTIVE', NOW(), NOW()),
('user002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800138002', 'lisi@example.com', 'USER', 300.00, 'ACTIVE', NOW(), NOW()),
('user003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '13800138003', 'wangwu@example.com', 'USER', 200.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ====================================
-- 2. 插入测试充电站
-- ====================================

INSERT INTO charging_stations (name, address, longitude, latitude, phone, business_hours, total_piles, available_piles, status, image_url, description, rating, review_count, create_time, update_time)
VALUES
-- 北京地区
('北京朝阳大悦城充电站', '北京市朝阳区朝阳北路101号大悦城停车场', 116.4833, 39.9219, '010-12345001', '00:00-24:00', 20, 15, 'ACTIVE', NULL, '位于朝阳大悦城地下停车场B2层，配备快充桩20个，周边有商场、餐饮等设施。', 4.5, 128, NOW(), NOW()),

('北京国贸充电站', '北京市朝阳区建国门外大街1号国贸中心', 116.4588, 39.9097, '010-12345002', '06:00-23:00', 15, 10, 'ACTIVE', NULL, '国贸商圈核心位置，提供快充服务，停车费5元/小时。', 4.3, 96, NOW(), NOW()),

('北京西单充电站', '北京市西城区西单北大街131号', 116.3772, 39.9134, '010-12345003', '00:00-24:00', 12, 8, 'ACTIVE', NULL, '西单商圈，地理位置优越，配备超充桩。', 4.6, 84, NOW(), NOW()),

-- 上海地区
('上海浦东国际机场充电站', '上海市浦东新区机场大道900号', 121.8054, 31.1434, '021-87654001', '00:00-24:00', 30, 25, 'ACTIVE', NULL, '机场P4停车场，提供快充和超充服务，适合候机充电。', 4.7, 215, NOW(), NOW()),

('上海陆家嘴充电站', '上海市浦东新区世纪大道1号', 121.5054, 31.2454, '021-87654002', '00:00-24:00', 18, 12, 'ACTIVE', NULL, '陆家嘴金融中心，智能充电桩，支持预约。', 4.4, 156, NOW(), NOW()),

('上海南京路充电站', '上海市黄浦区南京东路520号', 121.4872, 31.2389, '021-87654003', '07:00-22:00', 10, 6, 'ACTIVE', NULL, '南京路步行街附近，便于购物充电。', 4.2, 78, NOW(), NOW()),

-- 深圳地区
('深圳南山科技园充电站', '广东省深圳市南山区科技园南路', 113.9419, 22.5346, '0755-88888001', '00:00-24:00', 25, 20, 'ACTIVE', NULL, '科技园区核心位置，提供快充服务，适合上班族。', 4.8, 342, NOW(), NOW()),

('深圳宝安机场充电站', '广东省深圳市宝安区领航四路', 113.8205, 22.6393, '0755-88888002', '00:00-24:00', 28, 22, 'ACTIVE', NULL, '机场停车场，24小时快充，出行首选。', 4.6, 267, NOW(), NOW()),

('深圳福田中心区充电站', '广东省深圳市福田区深南大道', 114.0579, 22.5455, '0755-88888003', '00:00-24:00', 16, 11, 'ACTIVE', NULL, '福田CBD核心商圈，购物办公充电两不误。', 4.5, 198, NOW(), NOW()),

-- 广州地区
('广州天河城充电站', '广东省广州市天河区天河路208号', 113.3308, 23.1367, '020-66666001', '08:00-23:00', 14, 9, 'ACTIVE', NULL, '天河商圈，地下停车场B3层。', 4.3, 112, NOW(), NOW()),

-- 杭州地区
('杭州西湖文化广场充电站', '浙江省杭州市下城区中山北路口', 120.1634, 30.2875, '0571-77777001', '00:00-24:00', 12, 8, 'ACTIVE', NULL, '西湖景区附近，旅游充电好选择。', 4.4, 89, NOW(), NOW()),

-- 成都地区
('成都春熙路充电站', '四川省成都市锦江区春熙路', 104.0813, 30.6610, '028-99999001', '07:00-23:00', 15, 10, 'ACTIVE', NULL, '春熙路商圈核心位置。', 4.5, 134, NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ====================================
-- 3. 插入测试充电桩
-- ====================================

-- 为每个充电站创建充电桩
-- 北京朝阳大悦城充电站 (ID假设为1)
INSERT INTO charging_piles (station_id, pile_code, pile_name, pile_type, rated_power, price, status, qr_code, connector_type, create_time, update_time)
SELECT
    cs.id,
    CONCAT('BJ-DYCY-', LPAD(n, 3, '0')),
    CONCAT(n, '号快充桩'),
    CASE WHEN n <= 15 THEN 'FAST' WHEN n <= 18 THEN 'SUPER' ELSE 'SLOW' END,
    CASE WHEN n <= 15 THEN 120.0 WHEN n <= 18 THEN 180.0 ELSE 7.0 END,
    CASE WHEN n <= 15 THEN 1.5 WHEN n <= 18 THEN 1.8 ELSE 0.6 END,
    CASE WHEN n <= 15 THEN 'AVAILABLE' WHEN n <= 17 THEN 'CHARGING' ELSE 'AVAILABLE' END,
    CONCAT('https://qr.example.com/', MD5(CONCAT('BJ-DYCY-', n))),
    'GB/T',
    NOW(),
    NOW()
FROM charging_stations cs
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
) nums
WHERE cs.name = '北京朝阳大悦城充电站'
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 上海浦东国际机场充电站
INSERT INTO charging_piles (station_id, pile_code, pile_name, pile_type, rated_power, price, status, qr_code, connector_type, create_time, update_time)
SELECT
    cs.id,
    CONCAT('SH-PDAIRPORT-', LPAD(n, 3, '0')),
    CONCAT(n, '号充电桩'),
    CASE WHEN n <= 20 THEN 'SUPER' ELSE 'FAST' END,
    CASE WHEN n <= 20 THEN 180.0 ELSE 120.0 END,
    CASE WHEN n <= 20 THEN 1.8 ELSE 1.5 END,
    CASE WHEN n <= 25 THEN 'AVAILABLE' ELSE 'CHARGING' END,
    CONCAT('https://qr.example.com/', MD5(CONCAT('SH-PDAIRPORT-', n))),
    'GB/T',
    NOW(),
    NOW()
FROM charging_stations cs
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
) nums
WHERE cs.name = '上海浦东国际机场充电站'
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 深圳南山科技园充电站
INSERT INTO charging_piles (station_id, pile_code, pile_name, pile_type, rated_power, price, status, qr_code, connector_type, create_time, update_time)
SELECT
    cs.id,
    CONCAT('SZ-NSKY-', LPAD(n, 3, '0')),
    CONCAT(n, '号充电桩'),
    CASE WHEN n <= 18 THEN 'FAST' ELSE 'SUPER' END,
    CASE WHEN n <= 18 THEN 120.0 ELSE 180.0 END,
    CASE WHEN n <= 18 THEN 1.5 ELSE 1.8 END,
    CASE WHEN n <= 20 THEN 'AVAILABLE' ELSE 'CHARGING' END,
    CONCAT('https://qr.example.com/', MD5(CONCAT('SZ-NSKY-', n))),
    'GB/T',
    NOW(),
    NOW()
FROM charging_stations cs
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
) nums
WHERE cs.name = '深圳南山科技园充电站'
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ====================================
-- 4. 验证数据插入
-- ====================================

-- 查看插入的数据
SELECT '=== 用户数据 ===' AS '';
SELECT id, username, nickname, phone, user_type, balance, status FROM users;

SELECT '=== 充电站数据 ===' AS '';
SELECT id, name, address, total_piles, available_piles, rating, review_count FROM charging_stations;

SELECT '=== 充电桩统计 ===' AS '';
SELECT
    cs.name AS station_name,
    COUNT(cp.id) AS total_piles,
    SUM(CASE WHEN cp.status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_piles,
    SUM(CASE WHEN cp.status = 'CHARGING' THEN 1 ELSE 0 END) AS charging_piles
FROM charging_stations cs
LEFT JOIN charging_piles cp ON cs.id = cp.station_id
GROUP BY cs.id, cs.name;

-- ====================================
-- 完成
-- ====================================
SELECT '数据库初始化完成！' AS message;
SELECT CONCAT('总用户数: ', COUNT(*)) AS info FROM users;
SELECT CONCAT('总充电站数: ', COUNT(*)) AS info FROM charging_stations;
SELECT CONCAT('总充电桩数: ', COUNT(*)) AS info FROM charging_piles;
