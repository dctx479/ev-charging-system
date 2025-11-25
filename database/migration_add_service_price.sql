-- 为 valet_charger 表添加服务价格字段
ALTER TABLE `valet_charger`
ADD COLUMN `service_price` DECIMAL(10,2) DEFAULT 30.00 COMMENT '服务价格(元/次)' AFTER `rating`;

-- 更新现有数据，设置不同的价格
UPDATE `valet_charger` SET `service_price` = 25.00 WHERE `id` = 1;  -- 刘师傅
UPDATE `valet_charger` SET `service_price` = 30.00 WHERE `id` = 2;  -- 陈师傅
UPDATE `valet_charger` SET `service_price` = 28.00 WHERE `id` = 3;  -- 黄师傅
UPDATE `valet_charger` SET `service_price` = 35.00 WHERE `id` = 4;  -- 赵师傅
UPDATE `valet_charger` SET `service_price` = 32.00 WHERE `id` = 5;  -- 孙师傅

-- 为 valet_charge_order 表添加电费字段
ALTER TABLE `valet_charge_order`
ADD COLUMN `charge_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '电费' AFTER `service_fee`;
