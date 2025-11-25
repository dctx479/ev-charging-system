# EV Charging Station AI Service

## 项目简介

基于 Flask 和 Scikit-learn 的充电站 AI 预测服务，提供充电时长预测和故障预测功能。

## 技术栈

- Python 3.8+
- Flask 3.0
- Scikit-learn 1.3
- Pandas 2.1
- NumPy 1.26

## 项目结构

```
ai-service/
├── models/                           # 训练好的模型文件
│   ├── charge_duration_model.pkl    # 充电时长预测模型
│   └── fault_prediction_model.pkl   # 故障预测模型
├── train/                            # 模型训练脚本
│   ├── train_duration_model.py      # 训练充电时长模型
│   └── train_fault_model.py         # 训练故障预测模型
├── utils/                            # 工具类
│   └── data_generator.py            # Mock 数据生成器
├── app.py                            # Flask 主程序
├── requirements.txt                  # Python 依赖
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 训练模型（可选）

```bash
# 训练充电时长预测模型
python train/train_duration_model.py

# 训练故障预测模型
python train/train_fault_model.py
```

### 3. 启动服务

```bash
python app.py
```

服务将在 http://localhost:5000 启动

## API 接口

### 1. 充电时长预测

**接口**: `POST /api/ai/predict/duration`

**请求参数**:
```json
{
  "battery_capacity": 60,      // 电池容量 (kWh)
  "current_soc": 20,            // 当前电量百分比 (0-100)
  "target_soc": 80,             // 目标电量百分比 (0-100)
  "pile_power": 60,             // 充电桩功率 (kW)
  "temperature": 25             // 环境温度 (°C)
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预测成功",
  "data": {
    "duration_hours": 0.72,
    "duration_minutes": 43.2,
    "estimated_cost": 64.8
  }
}
```

### 2. 故障预测

**接口**: `POST /api/ai/predict/fault`

**请求参数**:
```json
{
  "total_charging_times": 100,        // 累计充电次数
  "total_charging_energy": 5000,      // 累计充电电量 (kWh)
  "current_power": 50,                 // 当前功率 (kW)
  "voltage": 380,                      // 电压 (V)
  "current": 130,                      // 电流 (A)
  "temperature": 45,                   // 温度 (°C)
  "days_since_maintenance": 30         // 距离上次维护天数
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预测成功",
  "data": {
    "fault_probability": 0.2543,
    "risk_level": "LOW",
    "risk_text": "低风险",
    "suggestions": []
  }
}
```

## 模型说明

### 充电时长预测模型

- **算法**: 随机森林回归 (Random Forest Regressor)
- **特征**: 电池容量、当前电量、目标电量、充电桩功率、环境温度
- **目标**: 预测充电时长（小时）

### 故障预测模型

- **算法**: 随机森林分类 (Random Forest Classifier)
- **特征**: 累计充电次数、累计充电电量、当前功率、电压、电流、温度、距离上次维护天数
- **目标**: 预测是否会发生故障（二分类）

## 数据生成

项目提供了 Mock 数据生成器，用于生成训练数据：

```bash
python utils/data_generator.py
```

## 注意事项

1. 模型文件较大，未包含在代码库中，需要先训练模型
2. 如果模型文件不存在，API 将使用简单的计算规则作为备选方案
3. 生产环境建议使用真实数据重新训练模型
4. 可以根据实际业务需求调整模型参数和特征

## 后续改进

- [ ] 使用深度学习模型提升预测精度
- [ ] 增加更多特征（如历史充电记录、用户行为等）
- [ ] 实现模型的在线学习和更新
- [ ] 添加模型监控和性能追踪
- [ ] 支持批量预测接口
