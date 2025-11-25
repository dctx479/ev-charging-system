import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataAnalysis' }
      },
      {
        path: 'piles',
        name: 'PileManagement',
        component: () => import('@/views/PileManagement.vue'),
        meta: { title: '充电桩管理', icon: 'Connection' }
      },
      {
        path: 'faults',
        name: 'FaultManagement',
        component: () => import('@/views/FaultManagement.vue'),
        meta: { title: '故障管理', icon: 'WarnTriangleFilled' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计分析', icon: 'TrendCharts' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
