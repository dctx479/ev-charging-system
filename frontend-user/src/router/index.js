import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Auth/Register.vue'),
    meta: { title: '用户注册' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requiresAuth: false }
  },
  {
    path: '/station/:id',
    name: 'StationDetail',
    component: () => import('@/views/StationDetail.vue'),
    meta: { title: '充电站详情', requiresAuth: false }
  },
  {
    path: '/piles/:stationId',
    name: 'PileList',
    component: () => import('@/views/PileList.vue'),
    meta: { title: '充电桩列表', requiresAuth: false }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '我的', requiresAuth: true }
  },
  {
    path: '/credit/center',
    name: 'CreditCenter',
    component: () => import('@/views/Credit/CreditCenter.vue'),
    meta: { title: '积分中心', requiresAuth: true }
  },
  {
    path: '/credit/history',
    name: 'CreditHistory',
    component: () => import('@/views/Credit/CreditHistory.vue'),
    meta: { title: '积分记录', requiresAuth: true }
  },
  {
    path: '/credit/shop',
    name: 'CreditShop',
    component: () => import('@/views/Credit/CreditShop.vue'),
    meta: { title: '积分商城', requiresAuth: true }
  },
  {
    path: '/credit/exchange/records',
    name: 'ExchangeRecords',
    component: () => import('@/views/Credit/ExchangeRecords.vue'),
    meta: { title: '兑换记录', requiresAuth: true }
  },
  {
    path: '/credit/statistics',
    name: 'CreditStatistics',
    component: () => import('@/views/Credit/CreditStatistics.vue'),
    meta: { title: '积分统计', requiresAuth: true }
  },

  {
    path: '/order/create',
    name: 'CreateOrder',
    component: () => import('@/views/Order/CreateOrder.vue'),
    meta: { title: '开始充电', requiresAuth: true }
  },
  {
    path: '/order/charging',
    name: 'ChargingProgress',
    component: () => import('@/views/Order/ChargingProgress.vue'),
    meta: { title: '充电中', requiresAuth: true }
  },
  {
    path: '/order/list',
    redirect: '/orders'  // 重定向到规范路径
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/Order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/order/detail',
    name: 'OrderDetail',
    component: () => import('@/views/Order/OrderDetail.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/queue/status',
    name: 'QueueStatus',
    component: () => import('@/views/Queue/QueueStatus.vue'),
    meta: { title: '排队状态', requiresAuth: true }
  },
  // V2G双向充电
  {
    path: '/v2g/center',
    name: 'V2GCenter',
    component: () => import('@/views/V2G/V2GCenter.vue'),
    meta: { title: 'V2G中心', requiresAuth: true }
  },
  {
    path: '/v2g/discharge',
    name: 'V2GDischarge',
    component: () => import('@/views/V2G/V2GDischarge.vue'),
    meta: { title: 'V2G双向充电', requiresAuth: true }
  },

  // 能源监控
  {
    path: '/energy/monitor',
    name: 'EnergyMonitor',
    component: () => import('@/views/Energy/EnergyMonitor.vue'),
    meta: { title: '能源监控', requiresAuth: false }
  },
  // 充电搭子聊天
  {
    path: '/chat/buddy',
    name: 'ChargingBuddy',
    component: () => import('@/views/Chat/ChargingBuddy.vue'),
    meta: { title: '充电搭子', requiresAuth: true }
  },
  // 代充服务
  {
    path: '/valet/service',
    name: 'ValetService',
    component: () => import('@/views/Valet/ValetService.vue'),
    meta: { title: '代充服务', requiresAuth: true }
  },
  {
    path: '/valet/order',
    name: 'ValetOrder',
    component: () => import('@/views/Valet/ValetOrder.vue'),
    meta: { title: '创建代充订单', requiresAuth: true }
  },

  {
    path: '/valet/orders',
    name: 'ValetOrderList',
    component: () => import('@/views/Valet/ValetOrderList.vue'),
    meta: { title: '代充订单', requiresAuth: true }
  },
  {
    path: '/valet/charger/dashboard',
    name: 'ValetChargerDashboard',
    component: () => import('@/views/Valet/ValetChargerDashboard.vue'),
    meta: { title: '代充工作台', requiresAuth: true }
  },

  // 周边服务
  {
    path: '/nearby/service',
    name: 'NearbyService',
    component: () => import('@/views/Nearby/NearbyService.vue'),
    meta: { title: '周边服务', requiresAuth: false }
  },
  {
    path: '/nearby/search',
    name: 'NearbyPoiSearch',
    component: () => import('@/views/Nearby/NearbyPoiSearch.vue'),
    meta: { title: '周边服务搜索', requiresAuth: false }
  },
  // 用户信息
  {
    path: '/user/info',
    name: 'UserInfo',
    component: () => import('@/views/User/UserInfo.vue'),
    meta: { title: '个人信息', requiresAuth: true }
  },
  {
    path: '/user/password',
    name: 'ChangePassword',
    component: () => import('@/views/User/ChangePassword.vue'),
    meta: { title: '修改密码', requiresAuth: true }
  },
  // 设置
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '设置', requiresAuth: true }
  },
  // 余额管理
  {
    path: '/wallet/balance',
    name: 'Balance',
    component: () => import('@/views/Wallet/Balance.vue'),
    meta: { title: '账户余额', requiresAuth: true }
  },
  // 404 兜底路由（必须放在最后）
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/Home.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 从 JWT payload 中提取 userType（无需第三方库，仅解码 Base64）
function getUserTypeFromToken(token) {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload))
    return decoded.userType || null
  } catch {
    return null
  }
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 设置页面标题
  document.title = to.meta.title || '充电站管理系统'

  // 需要登录的页面
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      // token 存在但已过期时，清除登录状态
      if (userStore.token) {
        userStore.logout()
      }
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 验证用户角色：普通用户前端仅允许 USER 类型
    const userType = getUserTypeFromToken(userStore.token)
    if (userType !== 'USER') {
      // 非USER角色不允许访问用户前端，强制登出
      userStore.logout()
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  }

  next()
})

export default router
