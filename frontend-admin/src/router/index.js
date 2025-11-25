import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/store/admin'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataAnalysis', requiresAuth: true }
      },
      {
        path: 'piles',
        name: 'PileManagement',
        component: () => import('@/views/PileManagement.vue'),
        meta: { title: '充电桩管理', icon: 'Connection', requiresAuth: true }
      },
      {
        path: 'faults',
        name: 'FaultManagement',
        component: () => import('@/views/FaultManagement.vue'),
        meta: { title: '故障管理', icon: 'WarnTriangleFilled', requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计分析', icon: 'TrendCharts', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderManagement',
        component: () => import('@/views/OrderManagement.vue'),
        meta: { title: '订单管理', icon: 'Document', requiresAuth: true }
      },
      {
        path: 'v2g',
        name: 'V2GManagement',
        component: () => import('@/views/V2GManagement.vue'),
        meta: { title: 'V2G管理', icon: 'Connection', requiresAuth: true }
      },
      {
        path: 'energy',
        name: 'EnergyMonitoring',
        component: () => import('@/views/EnergyMonitoring.vue'),
        meta: { title: '能源监控', icon: 'Monitor', requiresAuth: true }
      },
      {
        path: 'valet',
        name: 'ValetManagement',
        component: () => import('@/views/ValetManagement.vue'),
        meta: { title: '代充管理', icon: 'User', requiresAuth: true }
      },
      {
        path: 'users/valet-chargers',
        name: 'ValetChargerManagement',
        component: () => import('@/views/User/ValetChargerManagement.vue'),
        meta: { title: '代充师傅管理', icon: 'UserFilled', requiresAuth: true }
      },
      {
        path: 'users/operators',
        name: 'OperatorManagement',
        component: () => import('@/views/User/OperatorManagement.vue'),
        meta: { title: '企业运营商管理', icon: 'OfficeBuilding', requiresAuth: true }
      },
      {
        path: 'users/statistics',
        name: 'UserStatistics',
        component: () => import('@/views/User/UserStatistics.vue'),
        meta: { title: '用户统计', icon: 'DataAnalysis', requiresAuth: true }
      }
    ]
  },
  // 404 兜底路由（必须放在最后）
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/dashboard'
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
  const token = localStorage.getItem('admin_token')
  const tokenExpireTime = localStorage.getItem('admin_token_expire')

  // Token有效性验证
  const isTokenValid = () => {
    if (!token) return false

    // 检查token是否过期（提前60秒认为过期，与store一致）
    if (tokenExpireTime) {
      const expireTime = parseInt(tokenExpireTime)
      const BUFFER_MS = 60 * 1000
      if (Date.now() > expireTime - BUFFER_MS) {
        // Token已过期，同步清除 localStorage 和 Pinia store 状态
        const adminStore = useAdminStore()
        adminStore.logout()
        return false
      }
    }

    // 检查角色：管理后台仅允许 ADMIN 和 OPERATOR
    const userType = getUserTypeFromToken(token)
    if (userType !== 'ADMIN' && userType !== 'OPERATOR') {
      return false
    }

    return true
  }

  const isAuthenticated = isTokenValid()

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      // 未登录或token无效，重定向到登录页，并保存原目标路由
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
  } else {
    // 不需要认证的路由（如登录页）
    if (to.path === '/login' && isAuthenticated) {
      // 已登录用户访问登录页，重定向到 Dashboard
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
