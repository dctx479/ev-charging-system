from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import os

app = Flask(__name__)
CORS(app)

# 模型路径
DURATION_MODEL_PATH = 'models/charge_duration_model.pkl'
FAULT_MODEL_PATH = 'models/fault_prediction_model.pkl'

# 加载模型（如果存在）
duration_model = None
fault_model = None

if os.path.exists(DURATION_MODEL_PATH):
    duration_model = joblib.load(DURATION_MODEL_PATH)
    print('充电时长预测模型加载成功')
else:
    print('充电时长预测模型不存在，请先训练模型')

if os.path.exists(FAULT_MODEL_PATH):
    fault_model = joblib.load(FAULT_MODEL_PATH)
    print('故障预测模型加载成功')
else:
    print('故障预测模型不存在，请先训练模型')


@app.route('/')
def index():
    return jsonify({
        'message': 'EV Charging AI Service',
        'version': '1.0.0',
        'endpoints': [
            '/api/ai/predict/duration',
            '/api/ai/predict/fault'
        ]
    })


@app.route('/api/ai/predict/duration', methods=['POST'])
def predict_duration():
    """
    预测充电时长
    输入参数:
    - battery_capacity: 电池容量 (kWh)
    - current_soc: 当前电量百分比 (0-100)
    - target_soc: 目标电量百分比 (0-100)
    - pile_power: 充电桩功率 (kW)
    - temperature: 环境温度 (°C)
    """
    try:
        data = request.json

        # 提取特征
        features = np.array([[
            data.get('battery_capacity', 60),
            data.get('current_soc', 20),
            data.get('target_soc', 80),
            data.get('pile_power', 60),
            data.get('temperature', 25)
        ]])

        # 使用模型预测（如果模型存在）
        if duration_model:
            duration = duration_model.predict(features)[0]
        else:
            # 简单计算（作为备选方案）
            battery_capacity = data.get('battery_capacity', 60)
            current_soc = data.get('current_soc', 20)
            target_soc = data.get('target_soc', 80)
            pile_power = data.get('pile_power', 60)

            # 需要充电的电量
            charge_amount = battery_capacity * (target_soc - current_soc) / 100
            # 预计充电时长（小时），考虑充电效率
            duration = (charge_amount / pile_power) * 1.2  # 1.2是充电效率系数

        # 转换为分钟
        duration_minutes = round(duration * 60, 2)

        return jsonify({
            'code': 200,
            'message': '预测成功',
            'data': {
                'duration_hours': round(duration, 2),
                'duration_minutes': duration_minutes,
                'estimated_cost': round(duration * pile_power * 1.5, 2)  # 假设电价1.5元/kWh
            }
        })

    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'预测失败: {str(e)}',
            'data': None
        }), 500


@app.route('/api/ai/predict/fault', methods=['POST'])
def predict_fault():
    """
    预测充电桩故障概率
    输入参数:
    - total_charging_times: 累计充电次数
    - total_charging_energy: 累计充电电量 (kWh)
    - current_power: 当前功率 (kW)
    - voltage: 电压 (V)
    - current: 电流 (A)
    - temperature: 温度 (°C)
    - days_since_maintenance: 距离上次维护天数
    """
    try:
        data = request.json

        # 提取特征
        features = np.array([[
            data.get('total_charging_times', 100),
            data.get('total_charging_energy', 5000),
            data.get('current_power', 50),
            data.get('voltage', 380),
            data.get('current', 130),
            data.get('temperature', 45),
            data.get('days_since_maintenance', 30)
        ]])

        # 使用模型预测（如果模型存在）
        if fault_model:
            fault_probability = fault_model.predict_proba(features)[0][1]
        else:
            # 简单规则（作为备选方案）
            temperature = data.get('temperature', 45)
            days_since_maintenance = data.get('days_since_maintenance', 30)
            total_charging_times = data.get('total_charging_times', 100)

            # 简单的故障概率计算
            temp_risk = max(0, (temperature - 50) / 50)  # 温度超过50度增加风险
            maintenance_risk = min(1, days_since_maintenance / 90)  # 维护间隔风险
            usage_risk = min(1, total_charging_times / 1000)  # 使用次数风险

            fault_probability = (temp_risk + maintenance_risk + usage_risk) / 3

        # 判断风险等级
        if fault_probability < 0.3:
            risk_level = 'LOW'
            risk_text = '低风险'
        elif fault_probability < 0.6:
            risk_level = 'MEDIUM'
            risk_text = '中风险'
        else:
            risk_level = 'HIGH'
            risk_text = '高风险'

        # 建议
        suggestions = []
        if fault_probability > 0.5:
            suggestions.append('建议尽快安排维护')
        if data.get('temperature', 0) > 55:
            suggestions.append('温度过高，建议检查散热系统')
        if data.get('days_since_maintenance', 0) > 60:
            suggestions.append('距离上次维护时间较长，建议定期检查')

        return jsonify({
            'code': 200,
            'message': '预测成功',
            'data': {
                'fault_probability': round(fault_probability, 4),
                'risk_level': risk_level,
                'risk_text': risk_text,
                'suggestions': suggestions
            }
        })

    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'预测失败: {str(e)}',
            'data': None
        }), 500


if __name__ == '__main__':
    print('='* 50)
    print('EV Charging AI Service Starting...')
    print('API Base URL: http://localhost:5000/api/ai')
    print('='* 50)
    app.run(host='0.0.0.0', port=5000, debug=True)
