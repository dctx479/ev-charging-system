# EV Charging Station AI Service

## 项目简介

基于 Flask 和 Scikit-learn 的充电站 AI 预测服务，提供充电时长预测和故障预测功能。

## 技术栈

- Python 3.13+
- Flask 3.0
- Scikit-learn 1.7+
- Pandas 2.0+
- NumPy 1.24+

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
├── test_api.py                       # API测试脚本
├── requirements.txt                  # Python 依赖
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 训练模型

**必须先训练模型才能启动服务！**

```bash
# 训练充电时长预测模型（10000条数据）
python train/train_duration_model.py

# 训练故障预测模型（5000条数据）
python train/train_fault_model.py
```

**模型性能指标**:
- 充电时长模型: MAE < 5分钟, R² > 0.85 ✓
- 故障预测模型: 准确率 > 80% ✓

### 3. 启动服务

```bash
python app.py
```

服务将在 http://localhost:5000 启动

### 4. 测试API

```bash
python test_api.py
```

## API 接口

### 健康检查

**接口**: `GET /health`

**响应示例**:
```json
{
  "status": "healthy",
  "service": "ai-service",
  "models": {
    "charge_duration": true,
    "fault_prediction": true
  }
}
```

### 1. 充电时长预测

**接口**: `POST /api/ai/predict/duration`

**请求参数**:
```json
{
  "battery_capacity": 75,      // 电池容量 (kWh), 必填
  "current_soc": 20,            // 当前电量百分比 (0-100), 必填
  "target_soc": 80,             // 目标电量百分比 (0-100), 必填
  "charge_power": 120,          // 充电功率 (kW), 必填
  "temperature": 25             // 环境温度 (°C), 可选，默认25
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预测成功",
  "data": {
    "duration": 37.5,           // 充电时长（分钟）
    "charge_amount": 45.0,      // 充电电量（kWh）
    "estimated_cost": 36.0      // 预估费用（元）
  }
}
```

**特性**:
- 输入特征: battery_capacity, current_soc, target_soc, charge_power, temperature
- 预测精度: MAE < 5分钟, R² > 0.85
- 参数验证: 自动验证输入参数范围

### 2. 故障预测

**接口**: `POST /api/ai/predict/fault`

**请求参数**:
```json
{
  "total_charge_count": 1500,              // 累计充电次数, 必填
  "total_charge_amount": 90000,            // 累计充电电量 (kWh), 必填
  "days_since_last_maintenance": 150,      // 距离上次维护天数, 必填
  "health_score": 45,                      // 健康度评分 (0-100), 必填
  "avg_daily_usage": 18,                   // 平均每日使用次数, 必填
  "voltage_fluctuation": 35,               // 电压波动 (V), 必填
  "fault_history_count": 8                 // 历史故障次数, 必填
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "预测成功",
  "data": {
    "fault_probability": 95.50,           // 故障概率（%）
    "will_fault": true,                   // 是否会故障（7天内）
    "risk_level": "HIGH",                 // 风险等级: LOW/MEDIUM/HIGH
    "risk_text": "高风险",
    "suggestion": "高风险，建议立即安排维护",
    "suggestions": [                      // 详细建议列表
      "高风险，建议立即安排维护",
      "健康度评分偏低，建议检查充电桩状态",
      "距离上次维护时间较长，建议尽快进行定期检查",
      "历史故障次数较多，建议重点关注",
      "电压波动较大，建议检查电力系统"
    ]
  }
}
```

**特性**:
- 输入特征: total_charge_count, total_charge_amount, days_since_last_maintenance, health_score, avg_daily_usage, voltage_fluctuation, fault_history_count
- 预测精度: 准确率 > 80%（实际达到91.40%）
- 智能建议: 根据不同风险等级提供维护建议

## 模型说明

### 充电时长预测模型

- **算法**: 随机森林回归 (Random Forest Regressor)
- **训练数据**: 10000条样本
- **特征数量**: 5个
- **模型参数**:
  - n_estimators=200
  - max_depth=15
  - min_samples_split=5
  - min_samples_leaf=2
- **性能指标**:
  - MAE: 4.88分钟 ✓
  - R²: 0.9865 ✓
- **特征重要性**:
  1. pile_power (57%)
  2. current_soc (19%)
  3. target_soc (16%)
  4. battery_capacity (7%)
  5. temperature (1%)

### 故障预测模型

- **算法**: 随机森林分类 (Random Forest Classifier)
- **训练数据**: 5000条样本
- **特征数量**: 7个
- **模型参数**:
  - n_estimators=100
  - max_depth=10
  - class_weight='balanced'
- **性能指标**:
  - 准确率: 91.40% ✓
  - ROC AUC: 0.9727
  - 精确率: 91%
  - 召回率: 95%（故障类）
- **特征重要性**:
  1. health_score (24%)
  2. total_charge_count (16%)
  3. voltage_fluctuation (16%)
  4. days_since_last_maintenance (14%)
  5. fault_history_count (13%)
  6. total_charge_amount (9%)
  7. avg_daily_usage (8%)

## 数据生成

项目提供了 Mock 数据生成器，用于生成训练数据：

```bash
python utils/data_generator.py
```

**充电时长数据生成规则**:
- 电池容量: 40-100 kWh
- 当前电量: 5-90%
- 目标电量: current_soc+10 ~ 100%
- 充电功率: 7/30/60/120/180 kW
- 环境温度: -10 ~ 40°C
- 考虑温度对充电效率的影响
- 添加3%随机噪声

**故障预测数据生成规则**:
- 基于多因素综合评分确定故障风险
- 考虑使用次数、健康度、维护间隔、电压波动、历史故障等因素
- 故障样本比例: 约60%

## 注意事项

1. **模型训练**: 首次使用必须先训练模型，模型文件保存在 `models/` 目录
2. **Python版本**: 需要Python 3.13+，低版本可能导致scikit-learn安装失败
3. **编码问题**: Windows系统需要设置UTF-8编码输出
4. **生产环境**: 建议使用真实业务数据重新训练模型以提高精度
5. **模型更新**: 可以定期用新数据重新训练模型以保持准确性

## 错误处理

API提供完善的错误处理机制:
- 400: 参数缺失或格式错误
- 500: 服务器内部错误

所有错误响应格式:
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## 后续改进

- [ ] 使用深度学习模型提升预测精度
- [ ] 增加更多特征（如历史充电记录、用户行为等）
- [ ] 实现模型的在线学习和更新
- [ ] 添加模型监控和性能追踪
- [ ] 支持批量预测接口
- [ ] 添加模型版本管理
- [ ] 集成模型可解释性工具（SHAP）

## 开发者

EV Charging System Team
