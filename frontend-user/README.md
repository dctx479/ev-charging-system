# EV Charging Station Management System - User Frontend

## 项目简介

基于 Vue 3 + Vant 4 的电动汽车充电站管理系统用户端（移动端）。

## 技术栈

- Vue 3.4
- Vite 5.0
- Vue Router 4.2
- Pinia 2.1
- Vant 4.8
- Axios 1.6

## 项目结构

```
frontend-user/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   │   ├── auth.js    # 认证相关
│   │   ├── station.js # 充电站相关
│   │   └── pile.js    # 充电桩相关
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   │   ├── StationCard.vue  # 充电站卡片
│   │   └── PileCard.vue     # 充电桩卡片
│   ├── router/         # 路由配置
│   ├── store/          # 状态管理
│   │   └── user.js    # 用户状态
│   ├── utils/          # 工具类
│   │   ├── request.js # Axios 封装
│   │   └── map.js     # 地图工具
│   ├── views/          # 页面组件
│   │   ├── Login.vue         # 登录页
│   │   ├── Home.vue          # 首页
│   │   ├── StationDetail.vue # 充电站详情
│   │   └── PileList.vue      # 充电桩列表
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问地址：http://localhost:5173

### 3. 构建生产版本

```bash
npm run build
```

### 4. 预览生产版本

```bash
npm run preview
```

## 主要功能

### 已实现功能

- 用户登录/注册
- 充电站列表展示
- 充电站搜索
- 附近充电站查询（基于定位）
- 充电站详情查看
- 充电桩列表查看
- 充电桩状态展示

### 待实现功能

- 充电订单管理
- 在线支付
- 实时充电状态监控
- 充电历史记录
- 用户个人中心
- 消息通知

## 配置说明

### 代理配置

开发环境下，所有 `/api` 开头的请求会代理到后端服务 `http://localhost:8080`。

可在 `vite.config.js` 中修改：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 高德地图配置

如需使用地图功能，需要：

1. 在 [高德开放平台](https://lbs.amap.com/) 申请 Web 端 API Key
2. 在 `src/utils/map.js` 中替换 `AMAP_KEY`
3. 在 `index.html` 中引入高德地图 JS API

```html
<script src="https://webapi.amap.com/maps?v=2.0&key=YOUR_KEY"></script>
```

## 组件库

项目使用 Vant 4 移动端组件库，已按需引入以下组件：

- Button（按钮）
- Cell/CellGroup（单元格）
- Form/Field（表单）
- Tabbar（标签栏）
- Icon（图标）
- Card（卡片）
- Tag（标签）
- Search（搜索）
- List（列表）
- PullRefresh（下拉刷新）
- NavBar（导航栏）
- Popup（弹出层）
- Toast（轻提示）
- Dialog（弹窗）
- Loading（加载）
- Empty（空状态）
- Divider（分割线）
- Image（图片）
- Lazyload（懒加载）

更多组件请参考：https://vant-ui.github.io/vant/

## API 接口

### 认证接口

- POST `/api/auth/login` - 登录
- POST `/api/auth/register` - 注册
- GET `/api/auth/user/info` - 获取用户信息
- POST `/api/auth/logout` - 退出

### 充电站接口

- GET `/api/stations` - 获取充电站列表
- GET `/api/stations/:id` - 获取充电站详情
- GET `/api/stations/search` - 搜索充电站
- GET `/api/stations/nearby` - 查询附近充电站

### 充电桩接口

- GET `/api/piles/station/:stationId` - 获取充电站的充电桩
- GET `/api/piles/station/:stationId/available` - 获取可用充电桩
- GET `/api/piles/:id` - 获取充电桩详情

## 注意事项

1. 需要先启动后端服务
2. Token 存储在 localStorage
3. 移动端适配：使用 viewport meta 标签
4. 图片懒加载：使用 Vant Lazyload 指令
5. 路由守卫：需登录的页面会自动跳转到登录页

## 浏览器支持

- Chrome >= 87
- Safari >= 13
- iOS Safari >= 13
- Android Browser >= 87

## 开发建议

1. 使用 Chrome DevTools 的移动设备模拟器进行开发
2. 建议开启设备工具栏的响应式模式
3. 推荐设备尺寸：iPhone 12 Pro (390 x 844)
