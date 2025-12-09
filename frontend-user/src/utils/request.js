import axios from 'axios'
import { showToast } from 'vant'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    // 确保请求头包含正确的 Content-Type
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data

    // 如果返回的状态码不是 200，则判断为错误
    if (res.code !== 200) {
      showToast({
        message: res.message || '请求失败',
        type: 'fail'
      })

      // 401: 未授权，跳转到登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    } else {
      return res
    }
  },
  error => {
    console.error('响应错误:', error)

    let message = '网络错误'
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器错误'
          break
        default:
          message = error.response.data?.message || '请求失败'
      }
    } else if (error.message) {
      if (error.message.includes('timeout')) {
        message = '请求超时'
      } else if (error.message.includes('Network')) {
        message = '网络连接失败'
      }
    }

    showToast({
      message,
      type: 'fail'
    })

    return Promise.reject(error)
  }
)

export default request
