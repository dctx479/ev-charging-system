-- 操作日志表（审计日志）
-- 用于记录系统中的关键操作，满足安全审计、问题排查和合规要求

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    module VARCHAR(50) NOT NULL COMMENT '操作模块（用户管理、充电站管理等）',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型（CREATE、UPDATE、DELETE、REVIEW等）',
    description VARCHAR(500) COMMENT '操作描述',
    method VARCHAR(10) COMMENT '请求方法（GET、POST、PUT、DELETE）',
    url VARCHAR(200) COMMENT '请求URL',
    params TEXT COMMENT '请求参数（JSON格式，敏感信息已脱敏）',
    ip VARCHAR(50) COMMENT 'IP地址',
    status VARCHAR(20) NOT NULL COMMENT '操作状态（SUCCESS、FAILURE）',
    error_msg VARCHAR(1000) COMMENT '错误信息（失败时记录）',
    duration BIGINT COMMENT '执行时长（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_module (module) COMMENT '模块索引',
    INDEX idx_operation_type (operation_type) COMMENT '操作类型索引',
    INDEX idx_status (status) COMMENT '状态索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 插入示例数据
INSERT INTO operation_log (user_id, username, module, operation_type, description, method, url, params, ip, status, duration, create_time) VALUES
(1, 'admin', '用户管理', 'REVIEW', '审核用户', 'POST', '/admin/users/2/review', '[{"approved":true}]', '192.168.1.100', 'SUCCESS', 125, NOW()),
(1, 'admin', '充电站管理', 'CREATE', '创建充电站', 'POST', '/admin/stations', '[{"name":"测试充电站"}]', '192.168.1.100', 'SUCCESS', 230, NOW()),
(2, 'user01', '用户中心', 'UPDATE', '更新个人信息', 'PUT', '/users/me', '[{"nickname":"新昵称"}]', '192.168.1.101', 'SUCCESS', 89, NOW()),
(1, 'admin', '充电桩管理', 'UPDATE', '更新充电桩状态', 'PUT', '/admin/piles/1/status', '[{"status":"OFFLINE"}]', '192.168.1.100', 'SUCCESS', 156, NOW()),
(1, 'admin', '用户管理', 'UPDATE', '更新用户状态', 'PUT', '/admin/users/3/status', '[{"status":"SUSPENDED"}]', '192.168.1.100', 'FAILURE', 78, NOW());
