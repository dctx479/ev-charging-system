-- ============================================
-- 积分商城功能数据库迁移脚本
-- 日期: 2025-12-14
-- 说明: 新增 credit_product 和 credit_exchange_record 表
-- ============================================

USE ev_charging_system;

-- ============================================
-- 1. 创建积分商品表
-- ============================================
DROP TABLE IF EXISTS `credit_product`;
CREATE TABLE `credit_product` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
    `image_url` VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
    `category` VARCHAR(20) NOT NULL COMMENT '商品类型: coupon-优惠券, discount-充电折扣, gift-实物礼品',
    `points_required` INT NOT NULL COMMENT '所需积分',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `exchanged_count` INT DEFAULT 0 COMMENT '已兑换数量',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '商品原价（元）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
    `limit_per_user` INT DEFAULT 0 COMMENT '每人限兑数量，0表示不限',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (`category`),
    INDEX idx_status (`status`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商品表';

-- ============================================
-- 2. 创建积分兑换记录表
-- ============================================
DROP TABLE IF EXISTS `credit_exchange_record`;
CREATE TABLE `credit_exchange_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '兑换记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称（冗余存储）',
    `points_used` INT NOT NULL COMMENT '消耗积分',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '兑换数量',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理, 1-已完成, 2-已取消',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '收货地址（实物礼品需要）',
    `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货电话',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_product_id (`product_id`),
    INDEX idx_status (`status`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换记录表';

-- ============================================
-- 3. 插入示例积分商品数据
-- ============================================
INSERT INTO `credit_product` (`name`, `description`, `category`, `points_required`, `stock`, `original_price`, `status`, `limit_per_user`)
VALUES
-- 优惠券类
('10元充电优惠券', '充电时可抵扣10元，有效期30天', 'coupon', 500, 1000, 10.00, 1, 5),
('20元充电优惠券', '充电时可抵扣20元，有效期30天', 'coupon', 900, 500, 20.00, 1, 3),
('50元充电优惠券', '充电时可抵扣50元，有效期30天', 'coupon', 2000, 200, 50.00, 1, 2),

-- 充电折扣类
('7天充电9折卡', '享受7天充电9折优惠', 'discount', 800, 500, 15.00, 1, 2),
('30天充电8折卡', '享受30天充电8折优惠', 'discount', 2500, 200, 50.00, 1, 1),
('VIP季度会员', '享受90天充电7折优惠', 'discount', 6000, 100, 150.00, 1, 1),

-- 实物礼品类
('品牌雨伞', '高品质晴雨伞，便携折叠', 'gift', 1200, 200, 30.00, 1, 2),
('车载充电器', '快充车载充电器，双USB接口', 'gift', 1800, 150, 50.00, 1, 1),
('洗车卡次卡', '5次洗车服务，全市连锁', 'gift', 2500, 100, 80.00, 1, 1),
('车载冰箱', '迷你车载冰箱，保温保鲜', 'gift', 5000, 50, 200.00, 1, 1),
('行车记录仪', '高清夜视行车记录仪', 'gift', 8000, 30, 300.00, 1, 1);

-- ============================================
-- 完成提示
-- ============================================
SELECT '积分商城数据库迁移完成！' AS message;
SELECT COUNT(*) AS product_count FROM credit_product;
