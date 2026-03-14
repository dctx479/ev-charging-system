"""
故障预测模型训练脚本
使用随机森林分类模型预测充电桩故障
"""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
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

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_generator import generate_fault_data


def train_fault_model():
    """训练故障预测模型"""
    logger.info('开始训练故障预测模型...')

    # 生成训练数据
    logger.info('生成训练数据...')
    data = generate_fault_data(n_samples=5000)
    df = pd.DataFrame(data)

    # 特征和标签
    feature_columns = [
        'total_charge_count', 'total_charge_amount', 'days_since_last_maintenance',
        'health_score', 'avg_daily_usage', 'voltage_fluctuation', 'fault_history_count'
    ]
    X = df[feature_columns]
    y = df['is_fault']

    # 分割数据集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    logger.info(f'训练集大小: {len(X_train)}')
    logger.info(f'测试集大小: {len(X_test)}')
    logger.info(f'故障样本比例: {y.mean():.2%}')

    # 训练模型
    logger.info('训练随机森林分类模型（n_estimators=100）...')
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        n_jobs=-1,
        class_weight='balanced'  # 处理类别不平衡
    )
    model.fit(X_train, y_train)
    logger.info('模型训练完成')

    # 评估模型
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]

    logger.info('='* 50)
    logger.info('模型评估结果:')
    logger.info('\n分类报告:')
    logger.info(classification_report(y_test, y_pred, target_names=['正常', '故障']))

    logger.info('\n混淆矩阵:')
    cm = confusion_matrix(y_test, y_pred)
    logger.info(str(cm))
    logger.info(f'  预测正常  预测故障')
    logger.info(f'实际正常: {cm[0][0]:5d}    {cm[0][1]:5d}')
    logger.info(f'实际故障: {cm[1][0]:5d}    {cm[1][1]:5d}')

    roc_auc = roc_auc_score(y_test, y_pred_proba)
    accuracy = (y_pred == y_test).mean()
    logger.info(f'\nROC AUC Score: {roc_auc:.4f}')
    logger.info(f'准确率 (Accuracy): {accuracy:.2%}')

    # 检查是否满足要求
    if accuracy > 0.80:
        logger.info('✓ 模型满足性能要求 (准确率 > 80%)')
    else:
        logger.warning(f'✗ 模型未满足性能要求 (准确率 {accuracy:.2%} <= 80%)')

    logger.info('='* 50)

    # 特征重要性
    feature_importance = pd.DataFrame({
        'feature': feature_columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    logger.info('\n特征重要性:')
    logger.info(feature_importance.to_string())

    # 保存模型
    model_dir = 'models'
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)
        logger.info(f'创建模型目录: {model_dir}')

    model_path = os.path.join(model_dir, 'fault_prediction_model.pkl')
    joblib.dump(model, model_path)
    logger.info(f'模型已保存至: {model_path}')

    # 保存模型元数据
    metadata = {
        'model_type': 'RandomForestClassifier',
        'n_estimators': 100,
        'features': feature_columns,
        'metrics': {
            'accuracy': round(float(accuracy), 4),
            'roc_auc': round(float(roc_auc), 4),
            'confusion_matrix': cm.tolist()
        },
        'train_samples': len(X_train),
        'test_samples': len(X_test),
        'fault_ratio': round(float(y.mean()), 4)
    }
    metadata_path = os.path.join(model_dir, 'fault_prediction_metadata.pkl')
    joblib.dump(metadata, metadata_path)
    logger.info(f'模型元数据已保存至: {metadata_path}')

    # 测试预测
    logger.info('\n执行测试预测...')
    test_samples = [
        [1500, 90000, 150, 45, 18, 35, 8],  # 高风险样本
        [200, 10000, 30, 85, 5, 10, 1]      # 低风险样本
    ]
    for i, sample in enumerate(test_samples, 1):
        fault_proba = model.predict_proba([sample])[0][1]
        risk_level = "高风险" if fault_proba > 0.7 else ("中风险" if fault_proba > 0.4 else "低风险")
        logger.info(f'样本 {i}: 故障概率 = {fault_proba:.2%} ({risk_level})')

    return model


if __name__ == '__main__':
    train_fault_model()
    logger.info('\n训练完成！')
