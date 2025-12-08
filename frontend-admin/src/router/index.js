import { createRouter, createWebHistory } from 'vue-router'

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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  const isAuthenticated = !!token

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      // 未登录，重定向到登录页，并保存原目标路由
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
