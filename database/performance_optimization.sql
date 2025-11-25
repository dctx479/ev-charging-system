-- ============================================
-- 性能优化SQL脚本
-- 包含索引创建、慢查询配置等
-- ============================================

-- ============================================
-- 1. 启用慢查询日志（可选，用于性能分析）
-- ============================================
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;  -- 记录执行时间超过1秒的查询
-- SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';

-- ============================================
-- 2. charging_station 表索引优化
-- ============================================

-- 位置索引：支持附近充电站查询（经纬度范围查询）
CREATE INDEX idx_station_location ON charging_station(latitude, longitude);

-- 状态索引：筛选营业中的站点
CREATE INDEX idx_station_status ON charging_station(status);

-- 评分索引：rating 为 @Transient 计算字段，不在DDL中，跳过
-- CREATE INDEX idx_station_rating ON charging_station(rating DESC);

-- 复合索引：rating 为 @Transient 字段，跳过
-- CREATE INDEX idx_station_status_rating ON charging_station(status, rating DESC);

-- ============================================
-- 3. charging_pile 表索引优化
-- ============================================

-- 站点索引：查询某站点的所有充电桩
CREATE INDEX idx_pile_station ON charging_pile(station_id);

-- 站点+状态复合索引：查询站点可用桩
CREATE INDEX idx_pile_station_status ON charging_pile(station_id, status);

-- 类型索引：按类型筛选充电桩
CREATE INDEX idx_pile_type ON charging_pile(pile_type);

-- 健康度索引：故障预测查询
CREATE INDEX idx_pile_health ON charging_pile(health_score);

-- 状态索引：全局状态筛选
CREATE INDEX idx_pile_status ON charging_pile(status);

-- V2G支持索引：筛选支持V2G的充电桩
CREATE INDEX idx_pile_v2g ON charging_pile(support_v2g);

-- ============================================
-- 4. charge_order 表索引优化
-- ============================================

-- 用户订单索引：用户订单列表（按时间倒序）
CREATE INDEX idx_order_user_time ON charge_order(user_id, create_time DESC);

-- 状态索引：按状态查询订单
CREATE INDEX idx_order_status_time ON charge_order(order_status, create_time DESC);

-- 充电桩索引：查询充电桩使用历史
CREATE INDEX idx_order_pile_time ON charge_order(pile_id, create_time DESC);

-- 用户+状态复合索引：用户订单筛选（最常用）
CREATE INDEX idx_order_user_status_time ON charge_order(user_id, order_status, create_time DESC);

-- 支付状态索引：查询未支付订单
CREATE INDEX idx_order_payment_status ON charge_order(payment_status, create_time DESC);

-- 充电站索引：查询站点订单统计
CREATE INDEX idx_order_station_time ON charge_order(station_id, create_time DESC);

-- 订单号唯一索引（已在实体中定义，这里确认）
-- CREATE UNIQUE INDEX idx_order_no ON charge_order(order_no);

-- ============================================
-- 5. queue_record 表索引优化
-- ============================================

-- 站点排队索引：查询站点排队情况（按实际列名）
CREATE INDEX idx_queue_station_status ON queue_record(station_id, queue_status);

-- 用户排队索引：用户排队记录
CREATE INDEX idx_queue_user_time ON queue_record(user_id, join_time DESC);

-- 状态+加入时间索引：查询活跃排队记录、过号检查
CREATE INDEX idx_queue_status_time ON queue_record(queue_status, join_time DESC);

-- 过号检查索引：定时任务按 assigned_time 查过期叫号
CREATE INDEX idx_queue_assigned_time ON queue_record(queue_status, assigned_time);

-- ============================================
-- 6. carbon_credit_record 表索引优化
-- ============================================

-- 用户积分索引：用户积分记录
CREATE INDEX idx_credit_user_time ON carbon_credit_record(user_id, create_time DESC);

-- 类型索引：按类型筛选积分记录（DDL实际列名 credit_type）
CREATE INDEX idx_credit_type ON carbon_credit_record(credit_type);

-- 订单索引：查询订单关联的积分记录
CREATE INDEX idx_credit_order ON carbon_credit_record(order_id);

-- ============================================
-- 7. fault_record 表索引优化
-- ============================================

-- 充电桩故障索引：查询充电桩故障历史（按实际列名 fault_time）
CREATE INDEX idx_fault_pile_time ON fault_record(pile_id, fault_time DESC);

-- 维修状态索引：查询未处理故障（按实际列名 repair_status）
CREATE INDEX idx_fault_status ON fault_record(repair_status);

-- 严重程度索引：按严重程度筛选
CREATE INDEX idx_fault_severity ON fault_record(severity);

-- 复合索引：维修状态+严重程度+时间
CREATE INDEX idx_fault_status_severity_time ON fault_record(repair_status, severity, fault_time DESC);

-- 站点+维修状态索引：管理端按站点筛选故障
CREATE INDEX idx_fault_station_status ON fault_record(station_id, repair_status);

-- ============================================
-- 8. maintenance_plan 表索引优化
-- ============================================

-- 充电桩维护索引：查询充电桩维护计划
CREATE INDEX idx_maintenance_pile ON maintenance_plan(pile_id);

-- 状态索引：查询待执行维护计划
CREATE INDEX idx_maintenance_status ON maintenance_plan(maintenance_status);

-- 计划时间索引：按计划时间排序（DDL实际列名 planned_time）
CREATE INDEX idx_maintenance_plan_time ON maintenance_plan(planned_time);

-- ============================================
-- 9. v2g_record 表索引优化
-- ============================================

-- 用户V2G记录索引
CREATE INDEX idx_v2g_user_time ON v2g_record(user_id, start_time DESC);

-- 充电桩V2G记录索引
CREATE INDEX idx_v2g_pile_time ON v2g_record(pile_id, start_time DESC);

-- 记录类型索引：按类型查询V2G活动
CREATE INDEX idx_v2g_type ON v2g_record(record_type);

-- 用户+记录类型索引：按类型统计用户V2G活动
CREATE INDEX idx_v2g_user_type ON v2g_record(user_id, record_type);

-- ============================================
-- 10. energy_data 表索引优化
-- ============================================

-- 充电站能源数据索引
CREATE INDEX idx_energy_station_time ON energy_data(station_id, record_time DESC);

-- 时间索引：按时间范围查询
CREATE INDEX idx_energy_time ON energy_data(record_time DESC);

-- ============================================
-- 11. chat_message 表索引优化
-- ============================================

-- 充电站聊天消息索引（按实际列名 create_time, is_deleted）
CREATE INDEX idx_chat_station_time ON chat_message(station_id, is_deleted, create_time DESC);

-- 发送者索引：查询用户发送的消息（按实际列名 user_id）
CREATE INDEX idx_chat_sender_time ON chat_message(user_id, create_time DESC);

-- ============================================
-- 12. nearby_service 表索引优化
-- ============================================

-- 充电站周边服务索引
CREATE INDEX idx_nearby_station ON nearby_service(station_id);

-- 服务类型索引：按类型筛选
CREATE INDEX idx_nearby_type ON nearby_service(service_type);

-- 距离索引：按距离排序
CREATE INDEX idx_nearby_distance ON nearby_service(distance);

-- ============================================
-- 13. payment 表索引优化
-- ============================================

-- 订单支付索引
CREATE INDEX idx_payment_order ON payment(order_id);

-- 用户支付索引
CREATE INDEX idx_payment_user_time ON payment(user_id, paid_time DESC);

-- 支付状态索引
CREATE INDEX idx_payment_status ON payment(payment_status);

-- ============================================
-- 14. user 表索引优化
-- ============================================

-- 手机号唯一索引（已在实体中定义，这里确认）
-- CREATE UNIQUE INDEX idx_user_phone ON user(phone);

-- 用户名索引：username 列在 users 表中，user 表无此列，跳过
-- CREATE INDEX idx_user_username ON user(username);

-- ============================================
-- 15. credit_product 表索引优化
-- ============================================

-- 状态索引：查询上架商品
CREATE INDEX idx_product_status ON credit_product(status);

-- 积分索引：按积分排序
CREATE INDEX idx_product_points ON credit_product(points_required);

-- ============================================
-- 16. credit_exchange_record 表索引优化
-- ============================================

-- 用户兑换记录索引（DDL实际列名 create_time）
CREATE INDEX idx_exchange_user_time ON credit_exchange_record(user_id, create_time DESC);

-- 商品兑换记录索引
CREATE INDEX idx_exchange_product ON credit_exchange_record(product_id);

-- ============================================
-- 17. valet_charger 表索引优化
-- ============================================

-- 状态索引：查询在线代充师傅
CREATE INDEX idx_charger_status ON valet_charger(status);

-- 充电站索引：current_station_id 不在 DDL 中，使用 station_id
-- CREATE INDEX idx_charger_station ON valet_charger(current_station_id);

-- ============================================
-- 18. valet_charge_order 表索引优化
-- ============================================

-- 用户代充订单索引
CREATE INDEX idx_valet_order_user_time ON valet_charge_order(user_id, create_time DESC);

-- 师傅代充订单索引
CREATE INDEX idx_valet_order_charger_time ON valet_charge_order(charger_id, create_time DESC);

-- 状态索引：查询待接单订单
CREATE INDEX idx_valet_order_status ON valet_charge_order(order_status);

-- ============================================
-- 19. 查看已创建的索引
-- ============================================
-- SELECT
--     TABLE_NAME,
--     INDEX_NAME,
--     GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
--     INDEX_TYPE,
--     NON_UNIQUE
-- FROM
--     INFORMATION_SCHEMA.STATISTICS
-- WHERE
--     TABLE_SCHEMA = 'ev_charging_system'
-- GROUP BY
--     TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
-- ORDER BY
--     TABLE_NAME, INDEX_NAME;

-- ============================================
-- 20. 分析表统计信息（优化查询计划）
-- ============================================
ANALYZE TABLE charging_station;
ANALYZE TABLE charging_pile;
ANALYZE TABLE charge_order;
ANALYZE TABLE queue_record;
ANALYZE TABLE carbon_credit_record;
ANALYZE TABLE fault_record;
ANALYZE TABLE maintenance_plan;
ANALYZE TABLE v2g_record;
ANALYZE TABLE energy_data;
ANALYZE TABLE chat_message;
ANALYZE TABLE nearby_service;
ANALYZE TABLE payment;
ANALYZE TABLE user;
ANALYZE TABLE credit_product;
ANALYZE TABLE credit_exchange_record;
ANALYZE TABLE valet_charger;
ANALYZE TABLE valet_charge_order;

-- ============================================
-- 21. 性能优化建议
-- ============================================
-- 1. 定期执行 ANALYZE TABLE 更新统计信息
-- 2. 监控慢查询日志，识别需要优化的查询
-- 3. 使用 EXPLAIN 分析查询计划，确保使用了索引
-- 4. 避免在索引列上使用函数，会导致索引失效
-- 5. 合理使用复合索引，遵循最左前缀原则
-- 6. 定期清理过期数据，保持表大小合理
-- 7. 考虑使用分区表处理大数据量场景

-- ============================================
-- 完成
-- ============================================
