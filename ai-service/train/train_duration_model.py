"""
充电时长预测模型训练脚本
使用随机森林回归模型预测充电时长
"""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
import joblib
import sys
import os
import logging

# 设置输出编码为UTF-8
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_generator import generate_duration_data


def train_duration_model():
    """训练充电时长预测模型"""
    logger.info('开始训练充电时长预测模型...')

    # 生成训练数据
    logger.info('生成训练数据...')
    data = generate_duration_data(n_samples=10000)
    df = pd.DataFrame(data)

    # 特征和标签
    X = df[['battery_capacity', 'current_soc', 'target_soc', 'pile_power', 'temperature']]
    y = df['duration_hours']

    # 分割数据集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    logger.info(f'训练集大小: {len(X_train)}')
    logger.info(f'测试集大小: {len(X_test)}')

    # 训练模型
    logger.info('训练随机森林回归模型（n_estimators=200）...')
    model = RandomForestRegressor(
        n_estimators=200,
        max_depth=15,
        min_samples_split=5,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)
    logger.info('模型训练完成')

    # 交叉验证评估
    logger.info('执行5折交叉验证...')
    cv_scores = cross_val_score(model, X, y, cv=5, scoring='r2')
    logger.info(f'5折交叉验证 R² 均值: {cv_scores.mean():.4f} (±{cv_scores.std():.4f})')

    # 评估模型
    y_pred = model.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    r2 = r2_score(y_test, y_pred)

    # 计算MAE（分钟）
    mae_minutes = np.mean(np.abs((y_test - y_pred) * 60))

    logger.info('='* 50)
    logger.info('模型评估结果:')
    logger.info(f'均方误差 (MSE): {mse:.4f}')
    logger.info(f'均方根误差 (RMSE): {rmse:.4f}')
    logger.info(f'平均绝对误差 (MAE): {mae_minutes:.2f} 分钟')
    logger.info(f'决定系数 (R²): {r2:.4f}')

    # 检查是否满足要求
    if mae_minutes < 5 and r2 > 0.85:
        logger.info('✓ 模型满足性能要求 (MAE < 5分钟, R² > 0.85)')
    else:
        logger.warning('✗ 模型未满足性能要求')
        if mae_minutes >= 5:
            logger.warning(f'  - MAE ({mae_minutes:.2f}分钟) >= 5分钟')
        if r2 <= 0.85:
            logger.warning(f'  - R² ({r2:.4f}) <= 0.85')

    logger.info('='* 50)

    # 特征重要性
    feature_importance = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    logger.info('\n特征重要性:')
    logger.info(feature_importance.to_string())

    # 保存模型
    model_dir = 'models'
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)
        logger.info(f'创建模型目录: {model_dir}')

    model_path = os.path.join(model_dir, 'charge_duration_model.pkl')
    joblib.dump(model, model_path)
    logger.info(f'模型已保存至: {model_path}')

    # 保存模型元数据
    metadata = {
        'model_type': 'RandomForestRegressor',
        'n_estimators': 200,
        'features': list(X.columns),
        'metrics': {
            'mse': round(mse, 4),
            'rmse': round(rmse, 4),
            'mae_minutes': round(mae_minutes, 2),
            'r2': round(r2, 4),
            'cv_r2_mean': round(cv_scores.mean(), 4),
            'cv_r2_std': round(cv_scores.std(), 4)
        },
        'train_samples': len(X_train),
        'test_samples': len(X_test)
    }
    metadata_path = os.path.join(model_dir, 'charge_duration_metadata.pkl')
    joblib.dump(metadata, metadata_path)
    logger.info(f'模型元数据已保存至: {metadata_path}')

    # 测试预测
    logger.info('\n执行测试预测...')
    test_sample = np.array([[60, 20, 80, 60, 25]])  # 60kWh电池，20%充到80%，60kW充电桩，25度
    predicted_duration = model.predict(test_sample)[0]
    logger.info(f'输入: 60kWh电池, 20%->80%, 60kW充电桩, 25°C')
    logger.info(f'预测充电时长: {predicted_duration:.2f} 小时 ({predicted_duration*60:.0f} 分钟)')

    return model


if __name__ == '__main__':
    train_duration_model()
    logger.info('\n训练完成！')
