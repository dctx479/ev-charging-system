from flask import Flask, request, jsonify
from flask_cors import CORS
from functools import wraps
import hmac
import joblib
import numpy as np
import os
import logging
import time
import threading
import math
from collections import defaultdict

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 安全修复: 限制请求体大小（1MB）
app.config['MAX_CONTENT_LENGTH'] = 1 * 1024 * 1024

# 安全修复: 限制CORS来源
allowed_origins = os.getenv('CORS_ORIGINS', 'http://localhost:5173,http://localhost:5174,http://localhost:8080').split(',')
CORS(app, origins=allowed_origins)

# API密钥认证（可选，通过环境变量启用）
API_KEY = os.getenv('AI_API_KEY', '')
API_KEY_ENABLED = os.getenv('AI_API_KEY_ENABLED', 'false').lower() == 'true'

# 限流配置
RATE_LIMIT_ENABLED = os.getenv('RATE_LIMIT_ENABLED', 'true').lower() == 'true'
RATE_LIMIT_REQUESTS = int(os.getenv('RATE_LIMIT_REQUESTS', '100'))  # 每分钟请求数
RATE_LIMIT_WINDOW = int(os.getenv('RATE_LIMIT_WINDOW', '60'))  # 时间窗口（秒）

# 简单的内存限流器（线程安全）
rate_limit_store = defaultdict(list)
_rate_limit_lock = threading.Lock()
_last_cleanup_time = 0
_CLEANUP_INTERVAL = 300  # 每5分钟清理一次过期IP

def get_client_ip():
    """获取客户端IP"""
    if request.headers.get('X-Forwarded-For'):
        return request.headers.get('X-Forwarded-For').split(',')[0].strip()
    return request.remote_addr or 'unknown'

def _cleanup_stale_ips():
    """清理长时间无请求的IP条目，防止内存泄漏"""
    global _last_cleanup_time
    current_time = time.time()
    if current_time - _last_cleanup_time < _CLEANUP_INTERVAL:
        return
    _last_cleanup_time = current_time
    stale_ips = [
        ip for ip, timestamps in rate_limit_store.items()
        if not timestamps or current_time - max(timestamps) >= RATE_LIMIT_WINDOW
    ]
    for ip in stale_ips:
        del rate_limit_store[ip]

def rate_limit_check():
    """检查是否超过限流（线程安全）"""
    if not RATE_LIMIT_ENABLED:
        return True

    client_ip = get_client_ip()
    current_time = time.time()

    with _rate_limit_lock:
        # 定期清理过期IP条目
        _cleanup_stale_ips()

        # 清理当前IP的过期记录
        rate_limit_store[client_ip] = [
            t for t in rate_limit_store[client_ip]
            if current_time - t < RATE_LIMIT_WINDOW
        ]

        # 检查请求数
        if len(rate_limit_store[client_ip]) >= RATE_LIMIT_REQUESTS:
            return False

        rate_limit_store[client_ip].append(current_time)
        return True

def require_api_key(f):
    """API密钥验证装饰器"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if API_KEY_ENABLED:
            provided_key = request.headers.get('X-API-Key')
            if not provided_key or not hmac.compare_digest(provided_key, API_KEY):
                logger.warning(f'未授权的API访问尝试: IP={get_client_ip()}')
                return jsonify({
                    'code': 401,
                    'message': '未授权访问',
                    'data': None
                }), 401
        return f(*args, **kwargs)
    return decorated_function

def check_rate_limit(f):
    """限流装饰器"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not rate_limit_check():
            logger.warning(f'请求过于频繁: IP={get_client_ip()}')
            return jsonify({
                'code': 429,
                'message': '请求过于频繁，请稍后重试',
                'data': None
            }), 429
        return f(*args, **kwargs)
    return decorated_function

# 模型路径
DURATION_MODEL_PATH = 'models/charge_duration_model.pkl'
FAULT_MODEL_PATH = 'models/fault_prediction_model.pkl'

# 加载模型（如果存在）
duration_model = None
fault_model = None

try:
    if os.path.exists(DURATION_MODEL_PATH):
        duration_model = joblib.load(DURATION_MODEL_PATH)
        logger.info('充电时长预测模型加载成功')
    else:
        logger.warning('充电时长预测模型不存在，请先训练模型')
except Exception as e:
    logger.error(f'充电时长预测模型加载失败: {e}')

try:
    if os.path.exists(FAULT_MODEL_PATH):
        fault_model = joblib.load(FAULT_MODEL_PATH)
        logger.info('故障预测模型加载成功')
    else:
        logger.warning('故障预测模型不存在，请先训练模型')
except Exception as e:
    logger.error(f'故障预测模型加载失败: {e}')


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
    # 验证模型是否正常工作
    status = 'healthy'
    try:
        if duration_model:
            test_features = np.array([[60, 20, 80, 60, 25]])
            prediction = duration_model.predict(test_features)[0]
            if prediction <= 0 or prediction > 100:
                status = 'degraded'
    except Exception:
        status = 'unhealthy'

    return jsonify({
        'status': status,
        'service': 'ai-service',
        'models': {
            'charge_duration': duration_model is not None,
            'fault_prediction': fault_model is not None
        }
    }), 200 if status != 'unhealthy' else 503


@app.route('/api/ai/predict/duration', methods=['POST'])
@require_api_key
@check_rate_limit
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

        # 验证数值有效性（防止NaN/Inf输入）
        values = [battery_capacity, current_soc, target_soc, charge_power, temperature]
        if any(math.isnan(v) or math.isinf(v) for v in values):
            return jsonify({
                'code': 400,
                'message': '参数包含无效数值（NaN或Infinity）',
                'data': None
            }), 400

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
            # 模型训练标签为 duration_hours，需转换为分钟
            duration_minutes = duration_model.predict(features)[0] * 60
        else:
            # 简单计算（作为备选方案）
            # 需要充电的电量
            charge_amount = battery_capacity * (target_soc - current_soc) / 100
            # 预计充电时长（小时），考虑充电效率90%
            duration_hours = (charge_amount / charge_power) / 0.9
            # 转换为分钟
            duration_minutes = duration_hours * 60

        # 确保返回合理的数值
        duration_minutes = round(max(duration_minutes, 0), 2)

        # 计算费用（简化的峰谷平电价）
        charge_amount = battery_capacity * (target_soc - current_soc) / 100
        # 使用加权平均电价: 谷0.4(8h) + 平0.8(9h) + 峰1.2(7h) ≈ 0.78元/kWh, 取0.8作为简化估算
        avg_price_per_kwh = 0.8
        estimated_cost = round(charge_amount * avg_price_per_kwh, 2)

        logger.info(f'充电时长预测: battery={battery_capacity}kWh, power={charge_power}kW, duration={duration_minutes}min')

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
        logger.warning(f'参数格式错误: {e}')
        return jsonify({
            'code': 400,
            'message': '参数格式错误',
            'data': None
        }), 400
    except Exception as e:
        # 安全修复: 不暴露具体异常信息
        logger.error(f'充电时长预测失败: {e}', exc_info=True)
        return jsonify({
            'code': 500,
            'message': '服务内部错误，请稍后重试',
            'data': None
        }), 500


@app.route('/api/ai/predict/fault', methods=['POST'])
@require_api_key
@check_rate_limit
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

        # 参数范围验证
        health_score = float(data['health_score'])
        days_since_last_maintenance = float(data['days_since_last_maintenance'])

        # 验证数值有效性（防止NaN/Inf输入）
        all_values = [
            float(data['total_charge_count']), float(data['total_charge_amount']),
            days_since_last_maintenance, health_score,
            float(data['avg_daily_usage']), float(data['voltage_fluctuation']),
            float(data['fault_history_count'])
        ]
        if any(math.isnan(v) or math.isinf(v) for v in all_values):
            return jsonify({
                'code': 400,
                'message': '参数包含无效数值（NaN或Infinity）',
                'data': None
            }), 400

        if not (0 <= health_score <= 100):
            return jsonify({
                'code': 400,
                'message': '健康度评分必须在0-100之间',
                'data': None
            }), 400

        if days_since_last_maintenance < 0:
            return jsonify({
                'code': 400,
                'message': '维护天数不能为负数',
                'data': None
            }), 400

        # 提取特征
        features = np.array([[
            float(data['total_charge_count']),
            float(data['total_charge_amount']),
            days_since_last_maintenance,
            health_score,
            float(data['avg_daily_usage']),
            float(data['voltage_fluctuation']),
            int(data['fault_history_count'])
        ]])

        # 使用模型预测（如果模型存在）
        if fault_model:
            fault_probability = fault_model.predict_proba(features)[0][1]
        else:
            # 简单规则（作为备选方案）
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

        if health_score < 60:
            suggestions.append('健康度评分偏低，建议检查充电桩状态')
        if days_since_last_maintenance > 90:
            suggestions.append('距离上次维护时间较长，建议尽快进行定期检查')
        if int(data['fault_history_count']) > 3:
            suggestions.append('历史故障次数较多，建议重点关注')
        if float(data['voltage_fluctuation']) > 30:
            suggestions.append('电压波动较大，建议检查电力系统')

        logger.info(f'故障预测: health_score={health_score}, probability={fault_probability:.2%}')

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
        logger.warning(f'参数格式错误: {e}')
        return jsonify({
            'code': 400,
            'message': '参数格式错误',
            'data': None
        }), 400
    except Exception as e:
        # 安全修复: 不暴露具体异常信息
        logger.error(f'故障预测失败: {e}', exc_info=True)
        return jsonify({
            'code': 500,
            'message': '服务内部错误，请稍后重试',
            'data': None
        }), 500


if __name__ == '__main__':
    debug_mode = os.getenv('FLASK_ENV', 'production') == 'development'
    logger.info('='* 50)
    logger.info('EV Charging AI Service Starting...')
    logger.info(f'API Base URL: http://localhost:5000/api/ai')
    logger.info(f'Debug Mode: {debug_mode}')
    logger.info(f'API Key Auth: {"Enabled" if API_KEY_ENABLED else "Disabled"}')
    logger.info(f'Rate Limiting: {"Enabled" if RATE_LIMIT_ENABLED else "Disabled"}')
    logger.info('='* 50)
    app.run(host='0.0.0.0', port=5000, debug=debug_mode)
