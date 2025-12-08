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
    meta: { title: '我的', requiresAuth: false }
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
    name: 'OrderList',
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 设置页面标题
  document.title = to.meta.title || '充电站管理系统'

  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else {
    next()
  }
})

export default router
