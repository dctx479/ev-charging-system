import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  },
  responseType: 'json'
})

// Duplicate request prevention - track pending requests
const pendingRequests = new Map()

function generateReqKey(config) {
  const { method, url, params, data } = config
  return [method, url, JSON.stringify(params), JSON.stringify(data)].join('&')
}

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // Check if request is already pending
    const reqKey = generateReqKey(config)
    if (pendingRequests.has(reqKey)) {
      const controller = pendingRequests.get(reqKey)
      controller.abort()
    }

    // Create AbortController for this request
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(reqKey, controller)

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

function clearAdminAuth() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_info')
  localStorage.removeItem('admin_token_expire')
}

// 401 处理去重：避免短时间内多次触发重定向
let lastLoginRedirectTime = 0
const LOGIN_REDIRECT_DEBOUNCE = 1000

function shouldRedirectToLogin() {
  const now = Date.now()
  if (now - lastLoginRedirectTime > LOGIN_REDIRECT_DEBOUNCE) {
    lastLoginRedirectTime = now
    return true
  }
  return false
}

export function resetLoginRedirectTime() {
  lastLoginRedirectTime = 0
}

request.interceptors.response.use(
  response => {
    // Remove from pending requests
    const reqKey = generateReqKey(response.config)
    pendingRequests.delete(reqKey)

    // blob 类型响应（文件下载）直接返回，不做业务码判断
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        clearAdminAuth()
        if (router.currentRoute.value.path !== '/login' && shouldRedirectToLogin()) {
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        }
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    // Remove from pending requests on error
    if (error.config) {
      const reqKey = generateReqKey(error.config)
      pendingRequests.delete(reqKey)
    }

    // Skip error message for cancelled/aborted requests
    if (axios.isCancel(error) || error.name === 'CanceledError') {
      return Promise.reject(error)
    }

    // 增强错误处理：区分不同HTTP状态码
    const status = error.response?.status
    const serverMsg = error.response?.data?.message

    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearAdminAuth()
      if (router.currentRoute.value.path !== '/login' && shouldRedirectToLogin()) {
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    } else if (status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else if (status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error(serverMsg || '服务器错误，请稍后重试')
    } else {
      ElMessage.error(serverMsg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
