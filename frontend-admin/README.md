# EV Charging Station Management System - Admin Frontend

## 项目简介

基于 Vue 3 + Element Plus 的电动汽车充电站管理系统管理后台。

## 技术栈

- Vue 3.4
- Vite 5.0
- Vue Router 4.2
- Pinia 2.1
- Element Plus 2.5
- ECharts 5.4
- Axios 1.6

## 快速开始

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

访问地址：http://localhost:5174

### 构建生产版本
```bash
npm run build
```

## 主要功能

- 数据概览仪表盘
- 充电桩管理
- 故障监控与处理
- 统计分析与图表展示
- WebSocket 实时数据推送

## 注意事项

1. 需要先启动后端服务
2. 管理员 Token 存储在 localStorage
3. 使用 WebSocket 进行实时数据通信
