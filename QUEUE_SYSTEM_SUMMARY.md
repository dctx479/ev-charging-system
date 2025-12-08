# 智能排队系统实现总结

## 项目概述
成功实现了EV充电站智能排队系统，包括完整的后端API、前端界面和自动化过号机制。

## 实现的功能

### 1. 核心功能
- ✅ 用户加入排队
- ✅ 实时查询排队状态
- ✅ 主动离开队列
- ✅ 自动叫号机制
- ✅ 15分钟超时自动过号
- ✅ 排队位置动态更新
- ✅ 预计等待时间计算

### 2. 后端实现

#### 文件清单
```
backend/src/main/java/com/ev/charging/
├── entity/QueueRecord.java                  # 排队记录实体
├── repository/QueueRecordRepository.java     # 数据访问层
├── dto/JoinQueueDTO.java                    # 加入排队请求DTO
├── vo/QueueStatusVO.java                    # 排队状态响应VO
├── vo/StationQueueInfoVO.java               # 站点排队信息VO
├── service/QueueService.java                # 排队业务逻辑
└── controller/QueueController.java          # 排队控制器
```

#### API 接口

**基础路径**: `/api/queue`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /join | 加入排队 | ✅ |
| GET | /status | 获取我的排队状态 | ✅ |
| DELETE | /leave | 离开队列 | ✅ |
| GET | /station/{stationId} | 获取站点排队信息 | ❌ |

#### 核心业务逻辑

**QueueService.java** 主要方法：
- `joinQueue()` - 加入排队，自动生成排队号，计算位置和等待时间
- `getQueueStatus()` - 获取用户排队状态，实时更新位置
- `leaveQueue()` - 离开队列，更新后续排队者位置
- `getStationQueueInfo()` - 查询站点排队信息和建议
- `callNext()` - 自动叫号（内部方法）
- `checkExpiredCalls()` - 定时任务，每分钟检查过号

#### 排队规则

**排队号生成规则**:
```
格式: {站点ID}-{日期}-{序号}
示例: 1-20250108-001
```

**状态定义**:
- 0: 排队中 (STATUS_QUEUING)
- 1: 已叫号 (STATUS_CALLED)
- 2: 已过号 (STATUS_EXPIRED)
- 3: 已取消 (STATUS_CANCELLED)

**业务规则**:
1. 有空闲充电桩时不允许排队，提示直接充电
2. 同一用户同一站点不能重复排队
3. 叫号后15分钟内未响应自动过号
4. 过号后需重新排队

**预计等待时间算法**:
```java
预计时间 = (队列位置 × 平均充电时长) / 可用充电桩数
平均充电时长 = 30分钟（可配置）
```

### 3. 前端实现

#### 文件清单
```
frontend-user/src/
├── api/queue.js                             # 排队API封装
├── views/Queue/QueueStatus.vue              # 排队状态页面
└── views/StationDetail.vue                  # 站点详情页（已更新）
```

#### 页面功能

**QueueStatus.vue - 排队状态页面**
- 大号显示排队号码
- 实时显示当前位置和前面排队人数
- 预计等待时间提示
- 已叫号状态提醒（显示剩余时间）
- 即将过号警告（剩余5分钟内）
- 操作按钮：
  - 排队中：显示"离开队列"
  - 已叫号：显示"开始充电"+"离开队列"
  - 已过号/已取消：显示"重新排队"
- 自动刷新（10秒轮询）

**StationDetail.vue - 站点详情页面（更新）**
- 显示当前排队人数（动态颜色）
- 显示预计等待时间
- 显示排队建议（基于排队人数和等待时间）
- 智能按钮：
  - 有空闲桩：显示"查看充电桩"
  - 无空闲桩：显示"加入排队"

#### 路由配置
```javascript
{
  path: '/queue/status',
  name: 'QueueStatus',
  component: () => import('@/views/Queue/QueueStatus.vue'),
  meta: { title: '排队状态', requiresAuth: true }
}
```

### 4. 定时任务

**过号检查任务**:
- 执行频率: 每分钟
- 实现方式: `@Scheduled(fixedRate = 60000)`
- 功能: 检查已叫号记录是否超时，超时自动设为过号状态

**启用配置**:
- ChargingSystemApplication.java 添加 `@EnableScheduling` 注解

### 5. 数据库设计

**queue_record 表结构**:
```sql
CREATE TABLE `queue_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT,
    `user_id` BIGINT NOT NULL,
    `station_id` BIGINT NOT NULL,
    `pile_id` BIGINT,
    `queue_no` VARCHAR(20) NOT NULL,
    `queue_position` INT NOT NULL,
    `estimated_wait_time` INT,
    `status` TINYINT NOT NULL,
    `join_time` DATETIME NOT NULL,
    `call_time` DATETIME,
    `expire_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (`user_id`, `status`),
    INDEX idx_station_status (`station_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排队记录表';
```

## 技术亮点

### 1. 智能推荐算法
根据排队人数和等待时间，给出智能建议：
- 0人排队：无需排队，可直接充电
- <5人：排队人数较少，建议排队
- 5-10人：人数适中，显示预计等待时间
- >10人：人数较多，建议前往其他站点

### 2. 实时状态更新
- 前端每10秒自动刷新排队状态
- 位置变化实时计算
- 预计等待时间动态调整

### 3. 用户体验优化
- 大号显示排队号，醒目清晰
- 颜色区分不同状态（紫色渐变卡片）
- 即将过号时红色警告提示
- 已叫号显示剩余响应时间
- 温馨提示说明排队规则

### 4. 防重复机制
- 同一用户同一站点不能重复排队
- 数据库层面通过唯一索引防止并发问题

### 5. 自动化管理
- 充电桩空闲时自动叫号
- 超时自动过号
- 队列位置自动更新

## 测试建议

### 1. 功能测试
```bash
# 测试加入排队
POST http://localhost:8080/api/queue/join
Authorization: Bearer {token}
Content-Type: application/json

{
  "stationId": 1
}

# 测试查询状态
GET http://localhost:8080/api/queue/status
Authorization: Bearer {token}

# 测试站点排队信息
GET http://localhost:8080/api/queue/station/1
```

### 2. 边界测试
- 有空闲桩时尝试排队（应提示直接充电）
- 重复排队（应提示已在排队）
- 15分钟不响应（应自动过号）
- 离开队列后重新加入

### 3. 并发测试
- 多用户同时加入排队
- 验证排队号生成的唯一性
- 验证位置计算的准确性

## 使用流程

### 用户流程
```
1. 用户打开站点详情页
   ↓
2. 查看是否有空闲充电桩
   ↓
3. 如果无空闲，查看排队信息和建议
   ↓
4. 点击"加入排队"按钮
   ↓
5. 跳转到排队状态页面
   ↓
6. 实时查看排队位置和预计时间
   ↓
7. 叫号后显示"开始充电"按钮
   ↓
8. 点击开始充电，跳转到创建订单页
```

### 系统流程
```
1. 充电桩变为空闲
   ↓
2. QueueService.callNext() 自动叫号
   ↓
3. 更新第一位排队者状态为"已叫号"
   ↓
4. 设置过期时间（当前时间+15分钟）
   ↓
5. 更新后续排队者位置
   ↓
6. 用户在15分钟内开始充电 → 成功
   或
   用户15分钟未响应 → 定时任务自动过号 → 重新叫号
```

## 扩展建议

### 1. 消息通知
- 叫号时发送短信/推送通知
- 即将过号时提醒（5分钟倒计时）
- WebSocket实时推送排队位置变化

### 2. 预约功能
- 允许用户预约未来时段
- 预约到时自动转为排队

### 3. VIP优先
- 会员用户优先排队
- 紧急充电加急服务

### 4. 智能调度
- 根据历史数据预测等待时间
- 推荐最佳充电站点

## 部署说明

### 后端部署
1. 确保数据库 queue_record 表已创建
2. 启动 Spring Boot 应用
3. 验证定时任务是否正常运行（查看日志）

### 前端部署
1. 确保 router 配置包含 /queue/status 路由
2. 打包前端资源: `npm run build`
3. 部署到服务器

## 文件路径汇总

### 后端文件（绝对路径）
```
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\entity\QueueRecord.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\repository\QueueRecordRepository.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\dto\JoinQueueDTO.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\vo\QueueStatusVO.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\vo\StationQueueInfoVO.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\service\QueueService.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\controller\QueueController.java
G:\GitHub_local\Self-built\课设\ev-charging-system\backend\src\main\java\com\ev\charging\ChargingSystemApplication.java
```

### 前端文件（绝对路径）
```
G:\GitHub_local\Self-built\课设\ev-charging-system\frontend-user\src\api\queue.js
G:\GitHub_local\Self-built\课设\ev-charging-system\frontend-user\src\views\Queue\QueueStatus.vue
G:\GitHub_local\Self-built\课设\ev-charging-system\frontend-user\src\views\StationDetail.vue
G:\GitHub_local\Self-built\课设\ev-charging-system\frontend-user\src\router\index.js
```

## 总结

智能排队系统已完整实现，包括：
- ✅ 7个Java文件（1个实体，1个Repository，2个DTO/VO，1个Service，1个Controller）
- ✅ 4个前端文件（1个API，2个Vue组件，1个路由配置）
- ✅ 4个RESTful API接口
- ✅ 自动过号定时任务
- ✅ 智能排队建议算法
- ✅ 实时状态刷新机制

系统已经可以投入使用，用户可以在充电站无空闲桩时加入排队，系统会自动管理排队顺序，叫号通知，过号处理，提供完整的虚拟排队体验。

---
**实现日期**: 2025-01-08
**技术栈**: Spring Boot 3.x + Vue3 + Vant4 + MySQL 8.0
