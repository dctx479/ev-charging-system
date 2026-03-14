import axios from 'axios'
import { showToast, showDialog } from 'vant'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  },
  responseType: 'json'
})

// 防重复提交机制
const pendingRequests = new Map()

function getRequestKey(config) {
  return `${config.method?.toUpperCase()}:${config.url}:${JSON.stringify(config.data || config.params || '')}`
}

function addPendingRequest(config) {
  const key = getRequestKey(config)
  if (pendingRequests.has(key)) {
    return false // 请求正在进行中
  }
  const controller = new AbortController()
  config.signal = controller.signal
  pendingRequests.set(key, controller)
  return true
}

function removePendingRequest(config) {
  const key = getRequestKey(config)
  pendingRequests.delete(key)
}

// 401 处理去重：使用时间戳来避免短时间内多次触发，避免全局标志造成的永久锁定
let lastLoginRedirectTime = 0
const LOGIN_REDIRECT_DEBOUNCE = 1000 // 1秒内只触发一次

function shouldRedirectToLogin() {
  const now = Date.now()
  if (now - lastLoginRedirectTime > LOGIN_REDIRECT_DEBOUNCE) {
    lastLoginRedirectTime = now
    return true
  }
  return false
}

// 重置登录重定向时间戳（在用户成功登录后调用）
function resetLoginRedirectTime() {
  lastLoginRedirectTime = 0
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 防重复提交（仅对 POST/PUT/DELETE 请求）
    if (['post', 'put', 'delete'].includes(config.method?.toLowerCase())) {
      if (!addPendingRequest(config)) {
        return Promise.reject(new Error('请勿重复提交'))
      }
    }

    // 从 localStorage 获取 token
    const token = localStorage.getItem('user_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 确保请求头包含正确的 Content-Type（FormData时由浏览器自动设置）
    if (!config.headers['Content-Type'] && !(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    // FormData 时删除 Content-Type，让浏览器自动添加 boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  error => {
    if (import.meta.env.DEV) {
      console.error('请求错误:', error)
    }
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 清理待处理请求
    removePendingRequest(response.config)

    const res = response.data

    // 如果返回的状态码不是 200，则判断为错误
    if (res.code !== 200) {
      showToast({
        message: res.message || '请求失败',
        type: 'fail'
      })

      // 401: 未授权，跳转到登录页
      if (res.code === 401 && shouldRedirectToLogin()) {
        handleLoginExpired()
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    } else {
      return res
    }
  },
  async error => {
    // 清理待处理请求（如果有config）
    if (error.config) {
      removePendingRequest(error.config)
    }

    if (import.meta.env.DEV) {
      console.error('响应错误:', error)
    }

    let message = '网络错误'
    if (error.response) {
      const { status } = error.response

      // 防止错误拦截中的 401 错误消息和响应拦截中的重复
      if (status === 401) {
        if (shouldRedirectToLogin()) {
          handleLoginExpired()
        }
        return Promise.reject(error)
      }

      switch (status) {
        case 403:
          message = '拒绝访问，权限不足'
          break

        case 404:
          message = '请求地址不存在'
          break

        case 429:
          // 限流处理
          message = '请求过于频繁，请稍后再试'
          const retryAfter = error.response.headers['retry-after']
          if (retryAfter) {
            message += `（请等待${retryAfter}秒）`
          }
          showDialog({
            title: '请求过于频繁',
            message: message,
            confirmButtonText: '我知道了'
          })
          break

        case 500:
          message = '服务器内部错误'
          break

        case 502:
          message = '网关错误'
          break

        case 503:
          message = '服务暂时不可用'
          break

        default:
          message = error.response.data?.message || '请求失败'
      }
    } else if (error.message) {
      if (error.message.includes('timeout')) {
        message = '请求超时，请检查网络连接'
      } else if (error.message.includes('Network')) {
        message = '网络连接失败，请检查网络设置'
      } else if (error.message === '请勿重复提交') {
        // 防重复提交错误，不显示 toast
        return Promise.reject(error)
      }
    }

    // 对于429状态码，不重复显示toast（已经显示了dialog）
    if (error.response?.status !== 429) {
      showToast({
        message,
        type: 'fail',
        duration: 2000
      })
    }

    return Promise.reject(error)
  }
)

// 清理用户认证相关的所有缓存（避免与store形成循环依赖）
function clearUserAuthCache() {
  localStorage.removeItem('user_token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('user_token_expire')

  // 清理关联缓存（user_开头的缓存项）
  const keysToRemove = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && (key.startsWith('cache_') || key.startsWith('user_') || key.startsWith('order_'))) {
      keysToRemove.push(key)
    }
  }
  keysToRemove.forEach(key => localStorage.removeItem(key))
}

// 统一处理登录过期逻辑
function handleLoginExpired() {
  // 清理所有关联缓存
  clearUserAuthCache()

  // 获取当前路由路径用于返回
  const currentPath = router.currentRoute.value.path
  const redirectPath = currentPath !== '/login' ? currentPath : null

  showDialog({
    title: '登录过期',
    message: '您的登录已过期，请重新登录',
    confirmButtonText: '去登录',
    allowHtml: false
  }).then(() => {
    // 使用 router 跳转，避免页面硬刷新
    // 这样 Pinia store 会在登录页面重新初始化
    if (redirectPath) {
      router.push({
        path: '/login',
        query: { redirect: redirectPath }
      })
    } else {
      router.push('/login')
    }
  }).catch(() => {
    // 用户取消确认时，仍然进行跳转
    router.push('/login')
  })
}

export default request
export { resetLoginRedirectTime }
