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

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_generator import generate_fault_data


def train_fault_model():
    """训练故障预测模型"""
    print('开始训练故障预测模型...')

    # 生成训练数据
    print('生成训练数据...')
    data = generate_fault_data(n_samples=10000)
    df = pd.DataFrame(data)

    # 特征和标签
    feature_columns = [
        'total_charging_times', 'total_charging_energy', 'current_power',
        'voltage', 'current', 'temperature', 'days_since_maintenance'
    ]
    X = df[feature_columns]
    y = df['is_fault']

    # 分割数据集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f'训练集大小: {len(X_train)}')
    print(f'测试集大小: {len(X_test)}')
    print(f'故障样本比例: {y.mean():.2%}')

    # 训练模型
    print('训练随机森林分类模型...')
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        n_jobs=-1,
        class_weight='balanced'  # 处理类别不平衡
    )
    model.fit(X_train, y_train)

    # 评估模型
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]

    print('='* 50)
    print('模型评估结果:')
    print('\n分类报告:')
    print(classification_report(y_test, y_pred, target_names=['正常', '故障']))

    print('\n混淆矩阵:')
    cm = confusion_matrix(y_test, y_pred)
    print(cm)

    roc_auc = roc_auc_score(y_test, y_pred_proba)
    print(f'\nROC AUC Score: {roc_auc:.4f}')
    print('='* 50)

    # 特征重要性
    feature_importance = pd.DataFrame({
        'feature': feature_columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    print('\n特征重要性:')
    print(feature_importance)

    # 保存模型
    model_dir = 'models'
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)

    model_path = os.path.join(model_dir, 'fault_prediction_model.pkl')
    joblib.dump(model, model_path)
    print(f'\n模型已保存至: {model_path}')

    # 测试预测
    print('\n测试预测:')
    test_samples = [
        [500, 25000, 55, 380, 145, 60, 45],  # 高风险样本
        [100, 5000, 50, 380, 130, 35, 15]    # 低风险样本
    ]
    for i, sample in enumerate(test_samples, 1):
        fault_proba = model.predict_proba([sample])[0][1]
        print(f'\n样本 {i}: 故障概率 = {fault_proba:.2%}')

    return model


if __name__ == '__main__':
    train_fault_model()
    print('\n训练完成！')
