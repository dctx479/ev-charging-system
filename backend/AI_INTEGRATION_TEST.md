# AI预测服务集成测试指南

## 概述
本文档提供后端集成AI预测服务的测试说明。

## 前置条件

### 1. 启动AI服务
```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

AI服务将在 `http://localhost:5000` 运行

### 2. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 运行

## API接口测试

### 1. 充电时长预测

**请求URL**: `POST http://localhost:8080/api/api/ai/predict/duration`

**请求头**:
```
Content-Type: application/json
```

**请求体示例**:
```json
{
  "battery_capacity": 75,
  "current_soc": 20,
  "target_soc": 80,
  "charge_power": 120,
  "temperature": 25
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "duration": 32.5,
    "charge_amount": 45.0,
    "estimated_cost": 45.0
  }
}
```

**测试用例**:

1. **正常充电预测**
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "battery_capacity": 75,
    "current_soc": 20,
    "target_soc": 80,
    "charge_power": 120,
    "temperature": 25
  }'
```

2. **快充场景**
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "battery_capacity": 60,
    "current_soc": 10,
    "target_soc": 90,
    "charge_power": 350,
    "temperature": 20
  }'
```

3. **低温充电**
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "battery_capacity": 80,
    "current_soc": 15,
    "target_soc": 85,
    "charge_power": 60,
    "temperature": -5
  }'
```

### 2. 故障预测

**请求URL**: `POST http://localhost:8080/api/api/ai/predict/fault`

**请求头**:
```
Content-Type: application/json
```

**请求体示例**:
```json
{
  "total_charge_count": 500,
  "total_charge_amount": 15000,
  "days_since_last_maintenance": 45,
  "health_score": 65,
  "avg_daily_usage": 8.5,
  "voltage_fluctuation": 2.3,
  "fault_history_count": 2
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fault_probability": 45.8,
    "will_fault": false,
    "suggestion": "中风险，建议3天内安排维护",
    "risk_level": "中风险"
  }
}
```

**测试用例**:

1. **低风险场景**
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/fault \
  -H "Content-Type: application/json" \
  -d '{
    "total_charge_count": 200,
    "total_charge_amount": 5000,
    "days_since_last_maintenance": 10,
    "health_score": 95,
    "avg_daily_usage": 5.0,
    "voltage_fluctuation": 0.5,
    "fault_history_count": 0
  }'
```

2. **高风险场景**
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/fault \
  -H "Content-Type: application/json" \
  -d '{
    "total_charge_count": 2000,
    "total_charge_amount": 50000,
    "days_since_last_maintenance": 180,
    "health_score": 45,
    "avg_daily_usage": 15.0,
    "voltage_fluctuation": 5.0,
    "fault_history_count": 8
  }'
```

### 3. AI服务健康检查

**请求URL**: `GET http://localhost:8080/api/api/ai/health`

**响应示例**:
```json
{
  "code": 200,
  "message": "AI服务运行正常",
  "data": true
}
```

**测试命令**:
```bash
curl -X GET http://localhost:8080/api/api/ai/health
```

## 错误处理测试

### 1. AI服务不可用
关闭AI服务后测试:
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "battery_capacity": 75,
    "current_soc": 20,
    "target_soc": 80,
    "charge_power": 120
  }'
```

**预期响应**:
```json
{
  "code": 500,
  "message": "充电时长预测失败: AI服务暂时不可用，请稍后重试",
  "data": null
}
```

### 2. 参数验证错误
```bash
curl -X POST http://localhost:8080/api/api/ai/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "battery_capacity": 75,
    "current_soc": 80,
    "target_soc": 20,
    "charge_power": 120
  }'
```

**预期响应**:
```json
{
  "code": 400,
  "message": "目标电量必须大于当前电量",
  "data": null
}
```

## 集成测试要点

### 1. 超时处理
- 连接超时: 5秒
- 读取超时: 10秒
- 验证超时后是否返回友好错误消息

### 2. 并发测试
- 使用JMeter或ApacheBench测试100并发请求
- 验证AI服务和后端服务的性能

### 3. 数据验证
- 验证所有必填字段
- 验证数值范围（如SOC为0-100）
- 验证逻辑关系（如目标电量>当前电量）

## 常见问题

### 问题1: AI服务连接失败
**症状**: 返回"AI服务暂时不可用"

**解决方案**:
1. 检查AI服务是否启动: `curl http://localhost:5000/health`
2. 检查端口是否被占用: `netstat -ano | findstr 5000`
3. 检查防火墙设置

### 问题2: 参数验证失败
**症状**: 返回400错误

**解决方案**:
1. 检查请求体JSON格式是否正确
2. 检查字段名是否使用下划线（如battery_capacity）
3. 检查数值范围是否合法

### 问题3: 返回空响应
**症状**: AI服务返回null

**解决方案**:
1. 检查AI模型文件是否存在
2. 检查Flask返回的JSON格式
3. 查看后端日志获取详细错误信息

## 性能基准

### 预期响应时间
- 充电时长预测: < 500ms
- 故障预测: < 500ms
- 健康检查: < 100ms

### 资源使用
- AI服务内存占用: < 500MB
- 后端服务内存占用: < 1GB
- CPU使用率: < 20% (单核)

## 下一步

完成测试后，可以：
1. 在订单服务中集成充电时长预测
2. 在故障管理中集成故障预测
3. 添加预测结果缓存（Redis）
4. 实现预测历史记录功能
