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
        'status': 'running',
        'models': {
            'duration_model': 'loaded' if duration_model else 'not loaded',
            'fault_model': 'loaded' if fault_model else 'not loaded'
        },
        'endpoints': [
            '/health',
            '/api/ai/predict/duration',
            '/api/ai/predict/fault'
        ]
    })


@app.route('/health')
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'healthy',
        'service': 'ai-service',
        'models': {
            'charge_duration': duration_model is not None,
            'fault_prediction': fault_model is not None
        }
    }), 200


@app.route('/api/ai/predict/duration', methods=['POST'])
def predict_duration():
    """
    预测充电时长
    输入参数:
    - battery_capacity: 电池容量 (kWh)
    - current_soc: 当前电量百分比 (0-100)
    - target_soc: 目标电量百分比 (0-100)
    - charge_power: 充电功率 (kW)
    - temperature: 环境温度 (°C), 可选，默认25
    """
    try:
        if not request.is_json:
            return jsonify({
                'code': 400,
                'message': '请求必须是JSON格式',
                'data': None
            }), 400

        data = request.json

        # 验证必需参数
        required_fields = ['battery_capacity', 'current_soc', 'target_soc', 'charge_power']
        missing_fields = [field for field in required_fields if field not in data]
        if missing_fields:
            return jsonify({
                'code': 400,
                'message': f'缺少必需参数: {", ".join(missing_fields)}',
                'data': None
            }), 400

        # 提取特征
        battery_capacity = float(data['battery_capacity'])
        current_soc = float(data['current_soc'])
        target_soc = float(data['target_soc'])
        charge_power = float(data['charge_power'])
        temperature = float(data.get('temperature', 25))

        # 参数验证
        if battery_capacity <= 0 or battery_capacity > 200:
            return jsonify({
                'code': 400,
                'message': '电池容量必须在0-200kWh之间',
                'data': None
            }), 400

        if not (0 <= current_soc <= 100):
            return jsonify({
                'code': 400,
                'message': '当前电量必须在0-100之间',
                'data': None
            }), 400

        if not (0 <= target_soc <= 100):
            return jsonify({
                'code': 400,
                'message': '目标电量必须在0-100之间',
                'data': None
            }), 400

        if target_soc <= current_soc:
            return jsonify({
                'code': 400,
                'message': '目标电量必须大于当前电量',
                'data': None
            }), 400

        if charge_power <= 0 or charge_power > 400:
            return jsonify({
                'code': 400,
                'message': '充电功率必须在0-400kW之间',
                'data': None
            }), 400

        features = np.array([[
            battery_capacity,
            current_soc,
            target_soc,
            charge_power,
            temperature
        ]])

        # 使用模型预测（如果模型存在）
        if duration_model:
            duration = duration_model.predict(features)[0]
        else:
            # 简单计算（作为备选方案）
            # 需要充电的电量
            charge_amount = battery_capacity * (target_soc - current_soc) / 100
            # 预计充电时长（小时），考虑充电效率
            duration = (charge_amount / charge_power) * 1.2  # 1.2是充电效率系数

        # 转换为分钟
        duration_minutes = round(duration * 60, 2)

        # 计算费用（简化的峰谷平电价）
        charge_amount = battery_capacity * (target_soc - current_soc) / 100
        estimated_cost = round(charge_amount * 0.8, 2)  # 假设平均电价0.8元/kWh

        return jsonify({
            'code': 200,
            'message': '预测成功',
            'data': {
                'duration': duration_minutes,
                'charge_amount': round(charge_amount, 2),
                'estimated_cost': estimated_cost
            }
        })

    except ValueError as e:
        return jsonify({
            'code': 400,
            'message': f'参数格式错误: {str(e)}',
            'data': None
        }), 400
    except Exception as e:
        return jsonify({
            'code': 500,
            'message': f'预测失败: {str(e)}',
            'data': None
        }), 500


@app.route('/api/ai/predict/fault', methods=['POST'])
def predict_fault():
    """
    预测充电桩故障概率（7天内）
    输入参数:
    - total_charge_count: 累计充电次数
    - total_charge_amount: 累计充电电量 (kWh)
    - days_since_last_maintenance: 距离上次维护天数
    - health_score: 健康度评分 (0-100)
    - avg_daily_usage: 平均每日使用次数
    - voltage_fluctuation: 电压波动 (V)
    - fault_history_count: 历史故障次数
    """
    try:
        if not request.is_json:
            return jsonify({
                'code': 400,
                'message': '请求必须是JSON格式',
                'data': None
            }), 400

        data = request.json

        # 验证必需参数
        required_fields = [
            'total_charge_count', 'total_charge_amount', 'days_since_last_maintenance',
            'health_score', 'avg_daily_usage', 'voltage_fluctuation', 'fault_history_count'
        ]
        missing_fields = [field for field in required_fields if field not in data]
        if missing_fields:
            return jsonify({
                'code': 400,
                'message': f'缺少必需参数: {", ".join(missing_fields)}',
                'data': None
            }), 400

        # 提取特征
        features = np.array([[
            float(data['total_charge_count']),
            float(data['total_charge_amount']),
            float(data['days_since_last_maintenance']),
            float(data['health_score']),
            float(data['avg_daily_usage']),
            float(data['voltage_fluctuation']),
            int(data['fault_history_count'])
        ]])

        # 使用模型预测（如果模型存在）
        if fault_model:
            fault_probability = fault_model.predict_proba(features)[0][1]
        else:
            # 简单规则（作为备选方案）
            health_score = float(data['health_score'])
            days_since_last_maintenance = float(data['days_since_last_maintenance'])
            total_charge_count = float(data['total_charge_count'])
            fault_history_count = int(data['fault_history_count'])

            # 简单的故障概率计算
            health_risk = max(0, (100 - health_score) / 100)
            maintenance_risk = min(1, days_since_last_maintenance / 180)
            usage_risk = min(1, total_charge_count / 2000)
            history_risk = min(1, fault_history_count / 10)

            fault_probability = (health_risk * 0.4 + maintenance_risk * 0.3 +
                               usage_risk * 0.2 + history_risk * 0.1)

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

        # 生成建议
        suggestions = []
        if fault_probability > 0.7:
            suggestions.append('高风险，建议立即安排维护')
        elif fault_probability > 0.4:
            suggestions.append('中风险，建议3天内安排维护')
        else:
            suggestions.append('低风险，按常规计划维护')

        if float(data['health_score']) < 60:
            suggestions.append('健康度评分偏低，建议检查充电桩状态')
        if float(data['days_since_last_maintenance']) > 90:
            suggestions.append('距离上次维护时间较长，建议尽快进行定期检查')
        if int(data['fault_history_count']) > 3:
            suggestions.append('历史故障次数较多，建议重点关注')
        if float(data['voltage_fluctuation']) > 30:
            suggestions.append('电压波动较大，建议检查电力系统')

        return jsonify({
            'code': 200,
            'message': '预测成功',
            'data': {
                'fault_probability': round(fault_probability * 100, 2),
                'will_fault': fault_probability > 0.5,
                'risk_level': risk_level,
                'risk_text': risk_text,
                'suggestion': suggestions[0] if suggestions else '正常运行',
                'suggestions': suggestions
            }
        })

    except ValueError as e:
        return jsonify({
            'code': 400,
            'message': f'参数格式错误: {str(e)}',
            'data': None
        }), 400
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
