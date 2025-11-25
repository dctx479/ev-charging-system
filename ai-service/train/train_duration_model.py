"""
充电时长预测模型训练脚本
使用随机森林回归模型预测充电时长
"""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import joblib
import sys
import os

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_generator import generate_duration_data


def train_duration_model():
    """训练充电时长预测模型"""
    print('开始训练充电时长预测模型...')

    # 生成训练数据
    print('生成训练数据...')
    data = generate_duration_data(n_samples=10000)
    df = pd.DataFrame(data)

    # 特征和标签
    X = df[['battery_capacity', 'current_soc', 'target_soc', 'pile_power', 'temperature']]
    y = df['duration_hours']

    # 分割数据集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    print(f'训练集大小: {len(X_train)}')
    print(f'测试集大小: {len(X_test)}')

    # 训练模型
    print('训练随机森林回归模型...')
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)

    # 评估模型
    y_pred = model.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    r2 = r2_score(y_test, y_pred)

    print('='* 50)
    print('模型评估结果:')
    print(f'均方误差 (MSE): {mse:.4f}')
    print(f'均方根误差 (RMSE): {rmse:.4f}')
    print(f'决定系数 (R²): {r2:.4f}')
    print('='* 50)

    # 特征重要性
    feature_importance = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    print('\n特征重要性:')
    print(feature_importance)

    # 保存模型
    model_dir = 'models'
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)

    model_path = os.path.join(model_dir, 'charge_duration_model.pkl')
    joblib.dump(model, model_path)
    print(f'\n模型已保存至: {model_path}')

    # 测试预测
    print('\n测试预测:')
    test_sample = np.array([[60, 20, 80, 60, 25]])  # 60kWh电池，20%充到80%，60kW充电桩，25度
    predicted_duration = model.predict(test_sample)[0]
    print(f'输入: 60kWh电池, 20%->80%, 60kW充电桩, 25°C')
    print(f'预测充电时长: {predicted_duration:.2f} 小时 ({predicted_duration*60:.0f} 分钟)')

    return model


if __name__ == '__main__':
    train_duration_model()
    print('\n训练完成！')
