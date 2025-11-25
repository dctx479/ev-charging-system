"""
Mock 数据生成器
用于生成训练数据
"""

import numpy as np
import pandas as pd


def generate_duration_data(n_samples=10000):
    """
    生成充电时长训练数据
    """
    np.random.seed(42)

    data = []
    for _ in range(n_samples):
        # 电池容量 (40-100 kWh)
        battery_capacity = np.random.uniform(40, 100)

        # 当前电量 (5-90%)
        current_soc = np.random.uniform(5, 90)

        # 目标电量 (current_soc+10 ~ 100%)
        target_soc = np.random.uniform(min(current_soc + 10, 95), 100)

        # 充电桩功率 (7, 30, 60, 120, 180 kW)
        pile_power = np.random.choice([7, 30, 60, 120, 180])

        # 环境温度 (-10 ~ 40°C)
        temperature = np.random.uniform(-10, 40)

        # 计算充电时长
        charge_amount = battery_capacity * (target_soc - current_soc) / 100

        # 充电效率（受温度影响）
        if temperature < 0:
            efficiency = 0.75
        elif temperature < 10:
            efficiency = 0.85
        elif temperature < 30:
            efficiency = 0.95
        else:
            efficiency = 0.90

        # 理论充电时长（小时）
        duration_hours = (charge_amount / pile_power) / efficiency

        # 添加一些随机噪声
        duration_hours += np.random.normal(0, 0.1)

        data.append({
            'battery_capacity': battery_capacity,
            'current_soc': current_soc,
            'target_soc': target_soc,
            'pile_power': pile_power,
            'temperature': temperature,
            'duration_hours': max(0.1, duration_hours)  # 确保大于0
        })

    return data


def generate_fault_data(n_samples=10000):
    """
    生成故障预测训练数据
    """
    np.random.seed(42)

    data = []
    for _ in range(n_samples):
        # 累计充电次数 (0-2000)
        total_charging_times = np.random.randint(0, 2000)

        # 累计充电电量 (0-100000 kWh)
        total_charging_energy = total_charging_times * np.random.uniform(20, 80)

        # 当前功率 (0-200 kW)
        current_power = np.random.uniform(0, 200)

        # 电压 (350-420 V)
        voltage = np.random.uniform(350, 420)

        # 电流 (0-300 A)
        current = np.random.uniform(0, 300)

        # 温度 (20-80°C)
        temperature = np.random.uniform(20, 80)

        # 距离上次维护天数 (0-180)
        days_since_maintenance = np.random.randint(0, 180)

        # 故障概率计算（基于规则）
        fault_score = 0

        # 高使用次数增加故障风险
        if total_charging_times > 1500:
            fault_score += 0.3
        elif total_charging_times > 1000:
            fault_score += 0.15

        # 高温增加故障风险
        if temperature > 70:
            fault_score += 0.4
        elif temperature > 60:
            fault_score += 0.2

        # 长时间未维护增加风险
        if days_since_maintenance > 120:
            fault_score += 0.3
        elif days_since_maintenance > 90:
            fault_score += 0.15

        # 电压异常
        if voltage < 360 or voltage > 410:
            fault_score += 0.2

        # 添加随机因素
        fault_score += np.random.uniform(-0.1, 0.1)

        # 根据故障分数确定是否故障
        is_fault = 1 if fault_score > 0.6 else 0

        data.append({
            'total_charging_times': total_charging_times,
            'total_charging_energy': total_charging_energy,
            'current_power': current_power,
            'voltage': voltage,
            'current': current,
            'temperature': temperature,
            'days_since_maintenance': days_since_maintenance,
            'is_fault': is_fault
        })

    return data


if __name__ == '__main__':
    # 测试数据生成
    print('生成充电时长数据...')
    duration_data = generate_duration_data(100)
    df_duration = pd.DataFrame(duration_data)
    print(df_duration.head())
    print(f'\n数据统计:\n{df_duration.describe()}')

    print('\n' + '='*50 + '\n')

    print('生成故障预测数据...')
    fault_data = generate_fault_data(100)
    df_fault = pd.DataFrame(fault_data)
    print(df_fault.head())
    print(f'\n数据统计:\n{df_fault.describe()}')
    print(f'\n故障比例: {df_fault["is_fault"].mean():.2%}')
