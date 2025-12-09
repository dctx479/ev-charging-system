-- ============================================
-- 新能源汽车充电站点管理系统
-- 数据库初始化脚本（完整版）
-- 版本: 1.0
-- 日期: 2025-01-25
-- ============================================

-- ============================================
-- 1. 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS ev_charging_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_charging_system;

-- ============================================
-- 2. 核心业务表结构（DDL）
-- ============================================

-- 2.1 认证用户表（用于系统登录认证）
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `user_type` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户类型：USER-普通用户，ADMIN-管理员',
    `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额',
    `carbon_credits` INT DEFAULT 0 COMMENT '碳积分',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常，DISABLED-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (`username`),
    INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证用户表';

-- 2.1.1 业务用户表（用于业务数据关联）
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号（加密存储）',
    `car_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
    `car_model` VARCHAR(100) DEFAULT NULL COMMENT '车型',
    `battery_capacity` DECIMAL(5,2) DEFAULT NULL COMMENT '电池容量(kWh)',
    `carbon_credits` INT DEFAULT 0 COMMENT '碳积分',
    `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (`phone`),
    INDEX idx_car_plate (`car_plate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务用户表';

-- 2.2 运营商表
DROP TABLE IF EXISTS `operator`;
CREATE TABLE `operator` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '运营商ID',
    `name` VARCHAR(100) NOT NULL COMMENT '运营商名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '运营商编码',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营商表';

-- 2.3 充电站点表
DROP TABLE IF EXISTS `charging_station`;
CREATE TABLE `charging_station` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '站点ID',
    `operator_id` BIGINT NOT NULL COMMENT '运营商ID',
    `name` VARCHAR(100) NOT NULL COMMENT '站点名称',
    `province` VARCHAR(50) NOT NULL COMMENT '省份',
    `city` VARCHAR(50) NOT NULL COMMENT '城市',
    `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
    `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `longitude` DECIMAL(10,7) NOT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) NOT NULL COMMENT '纬度',
    `total_piles` INT DEFAULT 0 COMMENT '总充电桩数',
    `available_piles` INT DEFAULT 0 COMMENT '空闲充电桩数',
    `parking_fee` DECIMAL(5,2) DEFAULT 0.00 COMMENT '停车费(元/小时)',
    `business_hours` VARCHAR(50) DEFAULT '00:00-24:00' COMMENT '营业时间',
    `has_solar` TINYINT DEFAULT 0 COMMENT '是否有光伏：1是 0否',
    `has_storage` TINYINT DEFAULT 0 COMMENT '是否有储能：1是 0否',
    `solar_capacity` DECIMAL(8,2) DEFAULT 0.00 COMMENT '光伏容量(kW)',
    `storage_capacity` DECIMAL(8,2) DEFAULT 0.00 COMMENT '储能容量(kWh)',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1营业中 2维护中 0关闭',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`operator_id`) REFERENCES `operator`(`id`),
    INDEX idx_location (`longitude`, `latitude`),
    INDEX idx_city (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电站点表';

-- 2.4 充电桩表
DROP TABLE IF EXISTS `charging_pile`;
CREATE TABLE `charging_pile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `pile_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '充电桩编号',
    `pile_name` VARCHAR(100) DEFAULT NULL COMMENT '充电桩名称',
    `pile_type` TINYINT NOT NULL COMMENT '充电类型：1快充 2慢充 3超充',
    `power` DECIMAL(6,2) NOT NULL COMMENT '充电功率(kW)',
    `voltage` INT DEFAULT NULL COMMENT '额定电压(V)',
    `current` DECIMAL(6,2) DEFAULT NULL COMMENT '额定电流(A)',
    `connector_type` VARCHAR(50) DEFAULT NULL COMMENT '接口类型：国标/欧标/特斯拉等',
    `price_peak` DECIMAL(5,2) DEFAULT NULL COMMENT '峰时电价(元/度)',
    `price_flat` DECIMAL(5,2) DEFAULT NULL COMMENT '平时电价(元/度)',
    `price_valley` DECIMAL(5,2) DEFAULT NULL COMMENT '谷时电价(元/度)',
    `service_fee` DECIMAL(5,2) DEFAULT 0.00 COMMENT '服务费(元/度)',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1空闲 2充电中 3预约中 4故障 5离线',
    `qr_code` VARCHAR(255) DEFAULT NULL COMMENT '二维码URL',
    `total_charge_count` INT DEFAULT 0 COMMENT '累计充电次数',
    `total_charge_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计充电电量(kWh)',
    `health_score` TINYINT DEFAULT 100 COMMENT '健康度评分(0-100)',
    `last_maintenance_time` DATETIME DEFAULT NULL COMMENT '上次维护时间',
    `support_v2g` TINYINT DEFAULT 0 COMMENT '是否支持V2G：1是 0否',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_pile_no (`pile_no`),
    INDEX idx_station_status (`station_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电桩表';

-- 2.5 充电订单表
DROP TABLE IF EXISTS `charge_order`;
CREATE TABLE `charge_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pile_id` BIGINT NOT NULL COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始充电时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束充电时间',
    `predicted_duration` INT DEFAULT NULL COMMENT '预测充电时长(分钟)',
    `actual_duration` INT DEFAULT NULL COMMENT '实际充电时长(分钟)',
    `start_soc` TINYINT DEFAULT NULL COMMENT '起始电量百分比',
    `end_soc` TINYINT DEFAULT NULL COMMENT '结束电量百分比',
    `charge_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '充电电量(kWh)',
    `electricity_fee` DECIMAL(8,2) DEFAULT 0.00 COMMENT '电费(元)',
    `service_fee` DECIMAL(8,2) DEFAULT 0.00 COMMENT '服务费(元)',
    `parking_fee` DECIMAL(8,2) DEFAULT 0.00 COMMENT '停车费(元)',
    `total_fee` DECIMAL(8,2) DEFAULT 0.00 COMMENT '总费用(元)',
    `carbon_reduction` DECIMAL(6,2) DEFAULT 0.00 COMMENT '碳减排量(kg)',
    `carbon_credit_earned` INT DEFAULT 0 COMMENT '获得碳积分',
    `order_status` TINYINT DEFAULT 1 COMMENT '订单状态：1待支付 2充电中 3已完成 4已取消 5异常',
    `payment_status` TINYINT DEFAULT 0 COMMENT '支付状态：0未支付 1已支付 2退款中 3已退款',
    `is_v2g` TINYINT DEFAULT 0 COMMENT '是否V2G放电：1是 0否',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`pile_id`) REFERENCES `charging_pile`(`id`),
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_order_no (`order_no`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电订单表';

-- 2.6 支付记录表
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `payment_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '支付流水号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `payment_method` TINYINT NOT NULL COMMENT '支付方式：1微信 2支付宝 3余额',
    `payment_amount` DECIMAL(8,2) NOT NULL COMMENT '支付金额',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
    `payment_status` TINYINT DEFAULT 0 COMMENT '支付状态：0待支付 1已支付 2支付失败 3已退款',
    `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `refund_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '退款金额',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`order_id`) REFERENCES `charge_order`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX idx_payment_no (`payment_no`),
    INDEX idx_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ============================================
-- 3. 创新功能表结构
-- ============================================

-- 3.1 智能排队表
DROP TABLE IF EXISTS `queue_record`;
CREATE TABLE `queue_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排队ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `pile_type` TINYINT NOT NULL COMMENT '充电类型：1快充 2慢充',
    `queue_no` VARCHAR(20) NOT NULL COMMENT '排队号',
    `queue_position` INT NOT NULL COMMENT '当前排队位置',
    `estimated_wait_time` INT DEFAULT NULL COMMENT '预计等待时间(分钟)',
    `queue_status` TINYINT DEFAULT 1 COMMENT '状态：1排队中 2已分配 3已取消 4超时',
    `notify_sent` TINYINT DEFAULT 0 COMMENT '是否已发送通知：1是 0否',
    `assigned_pile_id` BIGINT DEFAULT NULL COMMENT '分配的充电桩ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入队列时间',
    `assigned_time` DATETIME DEFAULT NULL COMMENT '分配时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_station_status (`station_id`, `queue_status`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能排队表';

-- 3.2 碳积分记录表
DROP TABLE IF EXISTS `carbon_credit_record`;
CREATE TABLE `carbon_credit_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `credit_type` TINYINT NOT NULL COMMENT '积分类型：1充电获得 2签到 3兑换消耗 4活动奖励',
    `credit_change` INT NOT NULL COMMENT '积分变动（正为获得，负为消耗）',
    `balance_after` INT NOT NULL COMMENT '变动后余额',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳积分记录表';

-- 3.3 周边服务推荐表
DROP TABLE IF EXISTS `nearby_service`;
CREATE TABLE `nearby_service` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '服务ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `service_name` VARCHAR(100) NOT NULL COMMENT '服务名称',
    `service_type` TINYINT NOT NULL COMMENT '服务类型：1餐饮 2娱乐 3购物 4休闲',
    `distance` INT DEFAULT NULL COMMENT '距离(米)',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
    `avg_consume_time` INT DEFAULT NULL COMMENT '平均消费时长(分钟)',
    `recommended_wait_time_min` INT DEFAULT NULL COMMENT '推荐等待时间最小值(分钟)',
    `recommended_wait_time_max` INT DEFAULT NULL COMMENT '推荐等待时间最大值(分钟)',
    `coupon_info` VARCHAR(255) DEFAULT NULL COMMENT '优惠券信息',
    `rating` DECIMAL(2,1) DEFAULT NULL COMMENT '评分(0-5)',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0下架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_station_id (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周边服务推荐表';

-- 3.4 V2G充放电记录表
DROP TABLE IF EXISTS `v2g_record`;
CREATE TABLE `v2g_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pile_id` BIGINT NOT NULL COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `record_type` TINYINT NOT NULL COMMENT '类型：1充电 2放电',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `energy_amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '电量(kWh)',
    `electricity_price` DECIMAL(5,2) DEFAULT NULL COMMENT '实时电价(元/度)',
    `amount` DECIMAL(8,2) DEFAULT 0.00 COMMENT '金额(元)',
    `battery_health_before` TINYINT DEFAULT NULL COMMENT '操作前电池健康度',
    `battery_health_after` TINYINT DEFAULT NULL COMMENT '操作后电池健康度',
    `profit` DECIMAL(8,2) DEFAULT 0.00 COMMENT '收益(元)，仅放电时有效',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`pile_id`) REFERENCES `charging_pile`(`id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_record_type (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V2G充放电记录表';

-- 3.5 能源数据表（光储充）
DROP TABLE IF EXISTS `energy_data`;
CREATE TABLE `energy_data` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `record_time` DATETIME NOT NULL COMMENT '记录时间',
    `solar_generation` DECIMAL(8,2) DEFAULT 0.00 COMMENT '光伏发电量(kWh)',
    `grid_purchase` DECIMAL(8,2) DEFAULT 0.00 COMMENT '电网购电量(kWh)',
    `battery_charge` DECIMAL(8,2) DEFAULT 0.00 COMMENT '储能充电量(kWh)',
    `battery_discharge` DECIMAL(8,2) DEFAULT 0.00 COMMENT '储能放电量(kWh)',
    `battery_soc` TINYINT DEFAULT NULL COMMENT '储能电池电量百分比',
    `total_consumption` DECIMAL(8,2) DEFAULT 0.00 COMMENT '总消耗电量(kWh)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_station_time (`station_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能源数据表';

-- 3.6 代充师傅表
DROP TABLE IF EXISTS `valet_charger`;
CREATE TABLE `valet_charger` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '师傅ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号（加密）',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `service_area` VARCHAR(100) DEFAULT NULL COMMENT '服务区域',
    `driving_license` VARCHAR(255) DEFAULT NULL COMMENT '驾驶证照片',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '服务车辆车牌',
    `total_orders` INT DEFAULT 0 COMMENT '总订单数',
    `completed_orders` INT DEFAULT 0 COMMENT '完成订单数',
    `rating` DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1在线 2忙碌 3离线',
    `is_certified` TINYINT DEFAULT 0 COMMENT '是否认证：1是 0否',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (`phone`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代充师傅表';

-- 3.7 代充订单表
DROP TABLE IF EXISTS `valet_charge_order`;
CREATE TABLE `valet_charge_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `charger_id` BIGINT DEFAULT NULL COMMENT '师傅ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `pile_id` BIGINT DEFAULT NULL COMMENT '充电桩ID',
    `car_plate` VARCHAR(20) NOT NULL COMMENT '车牌号',
    `parking_location` VARCHAR(255) NOT NULL COMMENT '停车位置',
    `target_soc` TINYINT DEFAULT NULL COMMENT '目标电量',
    `pickup_time` DATETIME DEFAULT NULL COMMENT '取车时间',
    `service_fee` DECIMAL(8,2) DEFAULT 0.00 COMMENT '代充服务费',
    `order_status` TINYINT DEFAULT 1 COMMENT '订单状态：1待接单 2已接单 3充电中 4已完成 5已取消',
    `assign_time` DATETIME DEFAULT NULL COMMENT '接单时间',
    `start_charge_time` DATETIME DEFAULT NULL COMMENT '开始充电时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`charger_id`) REFERENCES `valet_charger`(`id`),
    INDEX idx_order_no (`order_no`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_charger_id (`charger_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代充订单表';

-- 3.8 充电搭子聊天表
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
    `user_avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
    `message_type` TINYINT DEFAULT 1 COMMENT '消息类型：1文字 2图片 3系统消息',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：1是 0否',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX idx_station_time (`station_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电搭子聊天表';

-- ============================================
-- 4. 运营管理表结构
-- ============================================

-- 4.1 故障记录表
DROP TABLE IF EXISTS `fault_record`;
CREATE TABLE `fault_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '故障ID',
    `pile_id` BIGINT NOT NULL COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `fault_type` TINYINT NOT NULL COMMENT '故障类型：1通信故障 2硬件故障 3软件故障 4其他',
    `fault_code` VARCHAR(50) DEFAULT NULL COMMENT '故障代码',
    `fault_description` TEXT DEFAULT NULL COMMENT '故障描述',
    `severity` TINYINT DEFAULT 1 COMMENT '严重程度：1轻微 2一般 3严重 4紧急',
    `report_source` TINYINT DEFAULT 1 COMMENT '上报来源：1系统自动 2用户上报 3运维发现',
    `reporter_id` BIGINT DEFAULT NULL COMMENT '上报人ID',
    `fault_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '故障发生时间',
    `repair_status` TINYINT DEFAULT 0 COMMENT '维修状态：0待维修 1维修中 2已完成',
    `repair_person` VARCHAR(50) DEFAULT NULL COMMENT '维修人员',
    `repair_start_time` DATETIME DEFAULT NULL COMMENT '开始维修时间',
    `repair_end_time` DATETIME DEFAULT NULL COMMENT '完成维修时间',
    `repair_cost` DECIMAL(8,2) DEFAULT 0.00 COMMENT '维修成本',
    `repair_note` TEXT DEFAULT NULL COMMENT '维修备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`pile_id`) REFERENCES `charging_pile`(`id`),
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_pile_id (`pile_id`),
    INDEX idx_repair_status (`repair_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障记录表';

-- 4.2 维护计划表（预测性维护）
DROP TABLE IF EXISTS `maintenance_plan`;
CREATE TABLE `maintenance_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    `pile_id` BIGINT NOT NULL COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `plan_type` TINYINT NOT NULL COMMENT '计划类型：1预测性维护 2定期保养 3紧急维护',
    `predicted_fault_probability` DECIMAL(5,2) DEFAULT NULL COMMENT '预测故障概率(%)',
    `maintenance_content` TEXT DEFAULT NULL COMMENT '维护内容',
    `planned_time` DATETIME NOT NULL COMMENT '计划维护时间',
    `maintenance_status` TINYINT DEFAULT 0 COMMENT '状态：0待执行 1执行中 2已完成 3已取消',
    `assigned_person` VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际完成时间',
    `maintenance_note` TEXT DEFAULT NULL COMMENT '维护记录',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`pile_id`) REFERENCES `charging_pile`(`id`),
    FOREIGN KEY (`station_id`) REFERENCES `charging_station`(`id`),
    INDEX idx_pile_id (`pile_id`),
    INDEX idx_planned_time (`planned_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维护计划表';

-- 4.3 评价表
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pile_id` BIGINT NOT NULL COMMENT '充电桩ID',
    `station_id` BIGINT NOT NULL COMMENT '站点ID',
    `rating` TINYINT NOT NULL COMMENT '评分(1-5)',
    `service_rating` TINYINT DEFAULT NULL COMMENT '服务评分',
    `facility_rating` TINYINT DEFAULT NULL COMMENT '设施评分',
    `environment_rating` TINYINT DEFAULT NULL COMMENT '环境评分',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（多个用逗号分隔）',
    `content` TEXT DEFAULT NULL COMMENT '评价内容',
    `images` TEXT DEFAULT NULL COMMENT '图片URL（多个用逗号分隔）',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：1是 0否',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`order_id`) REFERENCES `charge_order`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`pile_id`) REFERENCES `charging_pile`(`id`),
    INDEX idx_station_id (`station_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 5. 插入测试数据
-- ============================================

-- 5.1 插入运营商数据
INSERT INTO `operator` (`name`, `code`, `contact_person`, `contact_phone`, `email`) VALUES
('特来电充电', 'TELD', '张三', '13800138000', 'contact@teld.com'),
('星星充电', 'STAR', '李四', '13800138001', 'contact@starcharge.com'),
('云快充', 'YKC', '王五', '13800138002', 'contact@ykcharge.com')
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 5.2 插入认证用户（users表，用于登录认证）
-- BCrypt 哈希值: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi (对应密码: 123456)
INSERT INTO `users` (`username`, `password`, `nickname`, `phone`, `email`, `user_type`, `balance`, `carbon_credits`, `status`) VALUES
-- 管理员账号
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800138000', 'admin@evcharge.com', 'ADMIN', 10000.00, 0, 'ACTIVE'),
-- 普通用户账号
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800138001', 'zhangsan@test.com', 'USER', 500.00, 1200, 'ACTIVE'),
('lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800138002', 'lisi@test.com', 'USER', 300.00, 850, 'ACTIVE'),
('wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '13800138003', 'wangwu@test.com', 'USER', 200.00, 2100, 'ACTIVE')
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 5.3 插入业务用户（user表，用于业务数据关联）
-- BCrypt 哈希值: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO `user` (`phone`, `nickname`, `avatar`, `real_name`, `car_plate`, `car_model`, `battery_capacity`, `carbon_credits`, `balance`, `status`) VALUES
('13800138000', '系统管理员', NULL, '管理员', NULL, NULL, NULL, 0, 10000.00, 1),
('13800138001', '张三', NULL, '张三', '京A12345', '特斯拉Model 3', 75.00, 1200, 500.00, 1),
('13800138002', '李四', NULL, '李四', '京B67890', '比亚迪汉EV', 85.50, 850, 300.00, 1),
('13800138003', '王五', NULL, '王五', '京C11111', '蔚来ET7', 100.00, 2100, 200.00, 1)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 5.4 插入测试充电站（多城市多站点）
INSERT INTO `charging_station`
(`operator_id`, `name`, `province`, `city`, `district`, `address`, `longitude`, `latitude`, `total_piles`, `available_piles`, `parking_fee`, `business_hours`, `has_solar`, `has_storage`, `solar_capacity`, `storage_capacity`, `status`)
VALUES
-- 北京地区
(1, '北京朝阳大悦城充电站', '北京市', '北京市', '朝阳区', '朝阳北路101号大悦城停车场', 116.4833, 39.9219, 20, 15, 5.00, '00:00-24:00', 1, 1, 100.00, 200.00, 1),
(2, '北京国贸充电站', '北京市', '北京市', '朝阳区', '建国门外大街1号国贸中心', 116.4588, 39.9097, 15, 10, 8.00, '06:00-23:00', 0, 0, 0.00, 0.00, 1),
(3, '北京西单充电站', '北京市', '北京市', '西城区', '西单北大街131号', 116.3772, 39.9134, 12, 8, 6.00, '00:00-24:00', 0, 0, 0.00, 0.00, 1),

-- 上海地区
(2, '上海浦东国际机场充电站', '上海市', '上海市', '浦东新区', '机场大道900号', 121.8054, 31.1434, 30, 25, 8.00, '00:00-24:00', 1, 0, 150.00, 0.00, 1),
(1, '上海陆家嘴充电站', '上海市', '上海市', '浦东新区', '世纪大道1号', 121.5054, 31.2454, 18, 12, 10.00, '00:00-24:00', 0, 0, 0.00, 0.00, 1),
(3, '上海南京路充电站', '上海市', '上海市', '黄浦区', '南京东路520号', 121.4872, 31.2389, 10, 6, 12.00, '07:00-22:00', 0, 0, 0.00, 0.00, 1),

-- 深圳地区
(3, '深圳南山科技园充电站', '广东省', '深圳市', '南山区', '科技园南路', 113.9419, 22.5346, 25, 20, 6.00, '00:00-24:00', 1, 1, 120.00, 150.00, 1),
(1, '深圳宝安机场充电站', '广东省', '深圳市', '宝安区', '领航四路', 113.8205, 22.6393, 28, 22, 7.00, '00:00-24:00', 1, 0, 80.00, 0.00, 1),
(2, '深圳福田中心区充电站', '广东省', '深圳市', '福田区', '深南大道', 114.0579, 22.5455, 16, 11, 8.00, '00:00-24:00', 0, 0, 0.00, 0.00, 1),

-- 广州地区
(2, '广州天河城充电站', '广东省', '广州市', '天河区', '天河路208号', 113.3308, 23.1367, 14, 9, 6.00, '08:00-23:00', 0, 0, 0.00, 0.00, 1),

-- 杭州地区
(3, '杭州西湖文化广场充电站', '浙江省', '杭州市', '下城区', '中山北路口', 120.1634, 30.2875, 12, 8, 5.00, '00:00-24:00', 1, 0, 60.00, 0.00, 1),

-- 成都地区
(1, '成都春熙路充电站', '四川省', '成都市', '锦江区', '春熙路', 104.0813, 30.6610, 15, 10, 5.00, '07:00-23:00', 0, 0, 0.00, 0.00, 1)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 5.4 批量插入充电桩（为主要站点创建充电桩）

-- 北京朝阳大悦城充电站（20个桩）
INSERT INTO `charging_pile` (`station_id`, `pile_no`, `pile_name`, `pile_type`, `power`, `voltage`, `current`, `connector_type`, `price_peak`, `price_flat`, `price_valley`, `service_fee`, `status`, `qr_code`, `support_v2g`)
SELECT
    cs.id,
    CONCAT('TELD-BJ-DYCY-', LPAD(n, 3, '0')),
    CONCAT(n, '号充电桩'),
    CASE WHEN n <= 15 THEN 1 WHEN n <= 18 THEN 3 ELSE 2 END,
    CASE WHEN n <= 15 THEN 120.00 WHEN n <= 18 THEN 350.00 ELSE 7.00 END,
    CASE WHEN n <= 15 THEN 750 WHEN n <= 18 THEN 1000 ELSE 220 END,
    CASE WHEN n <= 15 THEN 160.00 WHEN n <= 18 THEN 350.00 ELSE 32.00 END,
    '国标GB/T',
    CASE WHEN n <= 15 THEN 1.20 WHEN n <= 18 THEN 1.50 ELSE 0.60 END,
    CASE WHEN n <= 15 THEN 0.80 WHEN n <= 18 THEN 1.00 ELSE 0.50 END,
    CASE WHEN n <= 15 THEN 0.40 WHEN n <= 18 THEN 0.50 ELSE 0.30 END,
    CASE WHEN n <= 15 THEN 0.50 WHEN n <= 18 THEN 0.80 ELSE 0.20 END,
    CASE WHEN n <= 15 THEN 1 WHEN n <= 17 THEN 2 ELSE 1 END,
    CONCAT('https://qr.evcharge.com/', MD5(CONCAT('TELD-BJ-DYCY-', n))),
    CASE WHEN n <= 15 THEN 1 ELSE 0 END
FROM charging_station cs
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
) nums
WHERE cs.name = '北京朝阳大悦城充电站'
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 上海浦东国际机场充电站（30个桩）
INSERT INTO `charging_pile` (`station_id`, `pile_no`, `pile_name`, `pile_type`, `power`, `voltage`, `current`, `connector_type`, `price_peak`, `price_flat`, `price_valley`, `service_fee`, `status`, `qr_code`, `support_v2g`)
SELECT
    cs.id,
    CONCAT('STAR-SH-AIRPORT-', LPAD(n, 3, '0')),
    CONCAT(n, '号充电桩'),
    CASE WHEN n <= 20 THEN 3 ELSE 1 END,
    CASE WHEN n <= 20 THEN 350.00 ELSE 120.00 END,
    CASE WHEN n <= 20 THEN 1000 ELSE 750 END,
    CASE WHEN n <= 20 THEN 350.00 ELSE 160.00 END,
    '国标GB/T',
    CASE WHEN n <= 20 THEN 1.50 ELSE 1.20 END,
    CASE WHEN n <= 20 THEN 1.00 ELSE 0.80 END,
    CASE WHEN n <= 20 THEN 0.50 ELSE 0.40 END,
    CASE WHEN n <= 20 THEN 0.80 ELSE 0.50 END,
    CASE WHEN n <= 25 THEN 1 ELSE 2 END,
    CONCAT('https://qr.evcharge.com/', MD5(CONCAT('STAR-SH-AIRPORT-', n))),
    CASE WHEN n <= 20 THEN 1 ELSE 1 END
FROM charging_station cs
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

-- 深圳南山科技园充电站（25个桩）
INSERT INTO `charging_pile` (`station_id`, `pile_no`, `pile_name`, `pile_type`, `power`, `voltage`, `current`, `connector_type`, `price_peak`, `price_flat`, `price_valley`, `service_fee`, `status`, `qr_code`, `support_v2g`)
SELECT
    cs.id,
    CONCAT('YKC-SZ-TECH-', LPAD(n, 3, '0')),
    CONCAT(n, '号充电桩'),
    CASE WHEN n <= 18 THEN 1 ELSE 3 END,
    CASE WHEN n <= 18 THEN 120.00 ELSE 350.00 END,
    CASE WHEN n <= 18 THEN 750 ELSE 1000 END,
    CASE WHEN n <= 18 THEN 160.00 ELSE 350.00 END,
    '国标GB/T',
    CASE WHEN n <= 18 THEN 1.20 ELSE 1.50 END,
    CASE WHEN n <= 18 THEN 0.80 ELSE 1.00 END,
    CASE WHEN n <= 18 THEN 0.40 ELSE 0.50 END,
    CASE WHEN n <= 18 THEN 0.50 ELSE 0.80 END,
    CASE WHEN n <= 20 THEN 1 ELSE 2 END,
    CONCAT('https://qr.evcharge.com/', MD5(CONCAT('YKC-SZ-TECH-', n))),
    CASE WHEN n > 18 THEN 1 ELSE 0 END
FROM charging_station cs
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
) nums
WHERE cs.name = '深圳南山科技园充电站'
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ============================================
-- 6. 创建视图
-- ============================================

-- 6.1 站点实时状态视图
DROP VIEW IF EXISTS `v_station_realtime_status`;
CREATE VIEW `v_station_realtime_status` AS
SELECT
    s.id AS station_id,
    s.name AS station_name,
    s.city,
    s.address,
    s.longitude,
    s.latitude,
    s.total_piles,
    COUNT(CASE WHEN p.status = 1 THEN 1 END) AS available_piles,
    COUNT(CASE WHEN p.status = 2 THEN 1 END) AS charging_piles,
    COUNT(CASE WHEN p.status = 4 THEN 1 END) AS fault_piles,
    MIN(p.price_valley) AS min_price,
    MAX(p.price_peak) AS max_price,
    s.parking_fee,
    s.has_solar,
    s.has_storage
FROM charging_station s
LEFT JOIN charging_pile p ON s.id = p.station_id
WHERE s.status = 1
GROUP BY s.id;

-- 6.2 运营商统计视图
DROP VIEW IF EXISTS `v_operator_statistics`;
CREATE VIEW `v_operator_statistics` AS
SELECT
    o.id AS operator_id,
    o.name AS operator_name,
    COUNT(DISTINCT s.id) AS total_stations,
    COUNT(p.id) AS total_piles,
    SUM(CASE WHEN p.status = 1 THEN 1 ELSE 0 END) AS available_piles,
    COALESCE(SUM(co.total_fee), 0) AS total_revenue,
    COALESCE(SUM(co.charge_amount), 0) AS total_charge_amount
FROM operator o
LEFT JOIN charging_station s ON o.id = s.operator_id
LEFT JOIN charging_pile p ON s.id = p.station_id
LEFT JOIN charge_order co ON p.id = co.pile_id AND co.payment_status = 1
GROUP BY o.id;

-- ============================================
-- 7. 索引优化
-- ============================================
-- 为高频查询字段添加组合索引
ALTER TABLE charge_order ADD INDEX idx_user_status (`user_id`, `order_status`);
ALTER TABLE charge_order ADD INDEX idx_pile_time (`pile_id`, `start_time`);
ALTER TABLE fault_record ADD INDEX idx_station_severity (`station_id`, `severity`);

-- ============================================
-- 6. 业务测试数据
-- ============================================

-- 6.1 周边服务推荐数据
INSERT INTO `nearby_service` (`station_id`, `service_name`, `service_type`, `distance`, `address`, `phone`, `avg_consume_time`, `recommended_wait_time_min`, `recommended_wait_time_max`, `coupon_info`, `rating`, `status`) VALUES
-- 北京朝阳大悦城站点 (station_id=1)
(1, '星巴克咖啡', 1, 150, '朝阳大悦城1层', '010-12345678', 20, 15, 40, '买一送一优惠券', 4.5, 1),
(1, '海底捞火锅', 1, 300, '朝阳大悦城5层', '010-12345679', 90, 60, 120, '8折优惠券', 4.8, 1),
(1, '万达影城', 2, 200, '朝阳大悦城6层', '010-12345680', 120, 90, 180, '电影票9.9元', 4.3, 1),
(1, '优衣库', 3, 100, '朝阳大悦城2层', '010-12345681', 40, 30, 60, '满200减30', 4.6, 1),
(1, '按摩椅休息区', 4, 50, '朝阳大悦城B2层', NULL, 15, 10, 30, '免费体验10分钟', 4.2, 1),
-- 上海浦东机场站点
(4, '肯德基', 1, 200, 'T2航站楼B1层', '021-87654321', 30, 20, 45, '汉堡套餐8折', 4.1, 1),
(4, '麦当劳', 1, 180, 'T2航站楼B1层', '021-87654322', 25, 15, 40, '第二杯半价', 4.2, 1),
(4, '日上免税店', 3, 300, 'T2航站楼出发层', '021-87654323', 60, 45, 90, '95折优惠', 4.7, 1),
-- 深圳南山科技园站点
(7, '喜茶', 1, 100, '科技园TCL大厦1层', '0755-88881234', 15, 10, 30, '新品8折', 4.6, 1),
(7, '瑞幸咖啡', 1, 80, '科技园南区', '0755-88881235', 10, 5, 20, '9.9元特惠', 4.4, 1),
(7, '盒马鲜生', 3, 250, '科技园万象天地', '0755-88881236', 45, 30, 60, '满100减20', 4.5, 1)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 6.2 代充师傅数据
INSERT INTO `valet_charger` (`name`, `phone`, `id_card`, `avatar`, `service_area`, `driving_license`, `vehicle_plate`, `total_orders`, `completed_orders`, `rating`, `status`, `is_certified`) VALUES
('刘师傅', '13900139001', '110101199001011234', NULL, '北京市朝阳区', NULL, '京A88888', 156, 150, 4.9, 1, 1),
('陈师傅', '13900139002', '310101199201021234', NULL, '上海市浦东新区', NULL, '沪A66666', 98, 95, 4.8, 1, 1),
('黄师傅', '13900139003', '440301199301031234', NULL, '深圳市南山区', NULL, '粤B77777', 210, 205, 4.7, 2, 1),
('赵师傅', '13900139004', '110101199401041234', NULL, '北京市海淀区', NULL, '京B99999', 45, 42, 4.6, 3, 1),
('孙师傅', '13900139005', '330101199501051234', NULL, '杭州市西湖区', NULL, '浙A55555', 78, 75, 4.8, 1, 1)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 6.3 充电订单测试数据（已完成订单）
INSERT INTO `charge_order` (`order_no`, `user_id`, `pile_id`, `station_id`, `start_time`, `end_time`, `predicted_duration`, `actual_duration`, `start_soc`, `end_soc`, `charge_amount`, `electricity_fee`, `service_fee`, `parking_fee`, `total_fee`, `carbon_reduction`, `carbon_credit_earned`, `order_status`, `payment_status`, `is_v2g`) VALUES
('CO202501250001', 2, 1, 1, '2025-01-25 08:30:00', '2025-01-25 09:15:00', 50, 45, 20, 80, 45.00, 36.00, 22.50, 3.75, 62.25, 35.33, 353, 3, 1, 0),
('CO202501250002', 3, 2, 1, '2025-01-25 10:00:00', '2025-01-25 10:50:00', 55, 50, 15, 85, 59.50, 71.40, 29.75, 4.17, 105.32, 46.71, 467, 3, 1, 0),
('CO202501250003', 4, 21, 4, '2025-01-25 14:00:00', '2025-01-25 14:30:00', 35, 30, 30, 90, 60.00, 90.00, 48.00, 4.00, 142.00, 47.10, 471, 3, 1, 0),
('CO202501240001', 2, 51, 7, '2025-01-24 19:00:00', '2025-01-24 19:45:00', 50, 45, 25, 80, 41.25, 49.50, 20.63, 4.50, 74.63, 32.38, 324, 3, 1, 0),
('CO202501240002', 3, 1, 1, '2025-01-24 07:00:00', '2025-01-24 07:40:00', 45, 40, 10, 70, 51.00, 20.40, 25.50, 3.33, 49.23, 40.04, 400, 3, 1, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 6.4 支付记录测试数据
INSERT INTO `payment` (`order_id`, `order_no`, `payment_no`, `user_id`, `payment_method`, `payment_amount`, `transaction_id`, `payment_status`, `paid_time`) VALUES
(1, 'CO202501250001', 'PAY202501250001', 2, 1, 62.25, 'WX4200001234567890', 1, '2025-01-25 09:16:00'),
(2, 'CO202501250002', 'PAY202501250002', 3, 2, 105.32, 'ALI2025012500001234', 1, '2025-01-25 10:51:00'),
(3, 'CO202501250003', 'PAY202501250003', 4, 3, 142.00, NULL, 1, '2025-01-25 14:31:00'),
(4, 'CO202501240001', 'PAY202501240001', 2, 1, 74.63, 'WX4200001234567891', 1, '2025-01-24 19:46:00'),
(5, 'CO202501240002', 'PAY202501240002', 3, 3, 49.23, NULL, 1, '2025-01-24 07:41:00')
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 6.5 碳积分记录测试数据
INSERT INTO `carbon_credit_record` (`user_id`, `order_id`, `credit_type`, `credit_change`, `balance_after`, `description`) VALUES
(2, 1, 1, 353, 1553, '充电45.00 kWh，获得碳积分'),
(3, 2, 1, 467, 1317, '充电59.50 kWh，获得碳积分'),
(4, 3, 1, 471, 2571, '充电60.00 kWh，获得碳积分'),
(2, 4, 1, 324, 1524, '充电41.25 kWh，获得碳积分'),
(3, 5, 1, 400, 850, '充电51.00 kWh，获得碳积分'),
(2, NULL, 4, 100, 1200, '新用户注册奖励'),
(3, NULL, 4, 100, 950, '新用户注册奖励'),
(4, NULL, 4, 100, 2100, '新用户注册奖励'),
(2, NULL, 3, -500, 1053, '兑换10元充电券')
ON DUPLICATE KEY UPDATE balance_after = balance_after;

-- 6.6 能源数据测试数据（北京朝阳大悦城站点，带光伏和储能）
INSERT INTO `energy_data` (`station_id`, `record_time`, `solar_generation`, `grid_purchase`, `battery_charge`, `battery_discharge`, `battery_soc`, `total_consumption`) VALUES
(1, '2025-01-25 06:00:00', 0.00, 50.00, 10.00, 0.00, 45, 40.00),
(1, '2025-01-25 08:00:00', 15.50, 80.00, 5.00, 0.00, 50, 90.50),
(1, '2025-01-25 10:00:00', 45.20, 120.00, 0.00, 10.00, 45, 175.20),
(1, '2025-01-25 12:00:00', 68.80, 150.00, 0.00, 25.00, 32, 243.80),
(1, '2025-01-25 14:00:00', 72.50, 180.00, 0.00, 30.00, 17, 282.50),
(1, '2025-01-25 16:00:00', 35.30, 160.00, 0.00, 15.00, 10, 210.30),
(1, '2025-01-25 18:00:00', 5.20, 200.00, 25.00, 0.00, 35, 180.20),
(1, '2025-01-25 20:00:00', 0.00, 180.00, 30.00, 0.00, 55, 150.00),
(1, '2025-01-25 22:00:00', 0.00, 100.00, 40.00, 0.00, 75, 60.00),
-- 深圳南山科技园站点
(7, '2025-01-25 08:00:00', 22.50, 100.00, 8.00, 0.00, 55, 114.50),
(7, '2025-01-25 12:00:00', 85.20, 180.00, 0.00, 20.00, 40, 285.20),
(7, '2025-01-25 16:00:00', 42.80, 150.00, 0.00, 18.00, 28, 210.80),
(7, '2025-01-25 20:00:00', 0.00, 120.00, 35.00, 0.00, 58, 85.00);

-- 6.7 V2G记录测试数据（在支持V2G的充电桩上）
INSERT INTO `v2g_record` (`user_id`, `pile_id`, `station_id`, `record_type`, `start_time`, `end_time`, `energy_amount`, `electricity_price`, `amount`, `battery_health_before`, `battery_health_after`, `profit`) VALUES
(4, 1, 1, 2, '2025-01-25 18:30:00', '2025-01-25 19:30:00', 15.00, 1.20, 18.00, 98, 98, 12.00),
(2, 21, 4, 2, '2025-01-25 19:00:00', '2025-01-25 20:00:00', 20.00, 1.50, 30.00, 97, 97, 20.00),
(4, 1, 1, 1, '2025-01-25 02:00:00', '2025-01-25 03:30:00', 30.00, 0.40, 12.00, 98, 98, 0.00),
(2, 21, 4, 1, '2025-01-24 23:00:00', '2025-01-25 01:00:00', 40.00, 0.50, 20.00, 97, 97, 0.00);

-- 6.8 故障记录测试数据
INSERT INTO `fault_record` (`pile_id`, `station_id`, `fault_type`, `fault_code`, `fault_description`, `severity`, `report_source`, `reporter_id`, `fault_time`, `repair_status`, `repair_person`, `repair_start_time`, `repair_end_time`, `repair_cost`, `repair_note`) VALUES
(17, 1, 2, 'ERR_CONNECTOR_001', '充电枪连接器接触不良', 2, 2, 2, '2025-01-24 15:30:00', 2, '李维修', '2025-01-24 16:00:00', '2025-01-24 17:30:00', 150.00, '更换充电枪头接口'),
(26, 4, 1, 'ERR_COMM_002', '通信模块断连', 3, 1, NULL, '2025-01-25 10:00:00', 1, '王维修', '2025-01-25 11:00:00', NULL, 0.00, '正在检查通信模块'),
(55, 7, 3, 'ERR_SOFTWARE_001', '计费系统异常', 1, 3, NULL, '2025-01-25 08:00:00', 0, NULL, NULL, NULL, 0.00, NULL);

-- 6.9 维护计划测试数据
INSERT INTO `maintenance_plan` (`pile_id`, `station_id`, `plan_type`, `predicted_fault_probability`, `maintenance_content`, `planned_time`, `maintenance_status`, `assigned_person`, `actual_start_time`, `actual_end_time`, `maintenance_note`) VALUES
(1, 1, 2, NULL, '定期保养：清洁充电枪、检查线缆、测试充电功率', '2025-02-01 09:00:00', 0, '张保养', NULL, NULL, NULL),
(5, 1, 1, 45.50, 'AI预测：充电模块温度异常，建议提前维护', '2025-01-28 10:00:00', 0, '李维修', NULL, NULL, NULL),
(21, 4, 1, 72.30, 'AI预测：健康度评分下降较快，建议紧急维护', '2025-01-26 14:00:00', 0, '王维修', NULL, NULL, NULL),
(51, 7, 2, NULL, '定期保养：检查接地、测试绝缘', '2025-02-05 09:00:00', 0, '赵保养', NULL, NULL, NULL);

-- ============================================
-- 8. 数据验证查询
-- ============================================

SELECT '==================== 数据验证 ====================' AS '';

-- 8.1 运营商数据
SELECT '=== 运营商数据 ===' AS '';
SELECT id, name, code, contact_person, contact_phone FROM operator;

-- 8.2 用户数据
SELECT '=== 用户数据 ===' AS '';
SELECT id, phone, nickname, car_plate, car_model, battery_capacity, balance, carbon_credits FROM `user`;

-- 8.3 充电站数据
SELECT '=== 充电站数据 ===' AS '';
SELECT id, name, city, district, address, total_piles, available_piles, has_solar, has_storage FROM charging_station;

-- 8.4 充电桩统计
SELECT '=== 充电桩统计 ===' AS '';
SELECT
    cs.name AS station_name,
    COUNT(cp.id) AS total_piles,
    SUM(CASE WHEN cp.status = 1 THEN 1 ELSE 0 END) AS available_piles,
    SUM(CASE WHEN cp.status = 2 THEN 1 ELSE 0 END) AS charging_piles,
    SUM(CASE WHEN cp.pile_type = 1 THEN 1 ELSE 0 END) AS fast_charge,
    SUM(CASE WHEN cp.pile_type = 2 THEN 1 ELSE 0 END) AS slow_charge,
    SUM(CASE WHEN cp.pile_type = 3 THEN 1 ELSE 0 END) AS super_charge,
    SUM(CASE WHEN cp.support_v2g = 1 THEN 1 ELSE 0 END) AS v2g_support
FROM charging_station cs
LEFT JOIN charging_pile cp ON cs.id = cp.station_id
GROUP BY cs.id, cs.name
ORDER BY cs.id;

-- 8.5 视图验证
SELECT '=== 站点实时状态视图 ===' AS '';
SELECT * FROM v_station_realtime_status LIMIT 5;

SELECT '=== 运营商统计视图 ===' AS '';
SELECT * FROM v_operator_statistics;

-- ============================================
-- 9. 完成信息
-- ============================================
SELECT '==================== 初始化完成 ====================' AS '';
SELECT CONCAT('数据库名: ev_charging_system') AS info;
SELECT CONCAT('总表数: ', COUNT(*)) AS info FROM information_schema.tables WHERE table_schema = 'ev_charging_system' AND table_type = 'BASE TABLE';
SELECT CONCAT('总视图数: ', COUNT(*)) AS info FROM information_schema.views WHERE table_schema = 'ev_charging_system';
SELECT CONCAT('运营商数: ', COUNT(*)) AS info FROM operator;
SELECT CONCAT('用户数: ', COUNT(*)) AS info FROM `user`;
SELECT CONCAT('充电站数: ', COUNT(*)) AS info FROM charging_station;
SELECT CONCAT('充电桩数: ', COUNT(*)) AS info FROM charging_pile;

SELECT '数据库初始化完成！所有表结构、测试数据、视图和索引已创建。' AS message;
