import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { logout as authLogout } from '@/api/auth'

// 安全的JSON解析函数
function safeJsonParse(jsonString, defaultValue = null) {
  try {
    return JSON.parse(jsonString)
  } catch (error) {
    console.error('JSON解析失败:', error)
    return defaultValue
  }
}

// 验证token是否有效（检查是否过期）
function isTokenValid(token) {
  if (!token) return false

  try {
    // 方法1：检查保存的过期时间（毫秒）
    const expireTime = localStorage.getItem('user_token_expire')
    if (expireTime) {
      // 提前60秒认为过期，避免边界问题
      const BUFFER_MS = 60 * 1000
      return Date.now() < parseInt(expireTime) - BUFFER_MS
    }

    // 方法2：JWT token格式: header.payload.signature
    const parts = token.split('.')
    if (parts.length !== 3) return false

    // 解码payload部分
    const payload = JSON.parse(atob(parts[1]))

    // 检查是否有过期时间
    if (!payload.exp) return false // 没有过期时间的token视为无效

    // 检查是否过期（exp是秒级时间戳，提前60秒认为过期以避免边界问题）
    const BUFFER_SECONDS = 60
    const now = Math.floor(Date.now() / 1000)
    return payload.exp > now + BUFFER_SECONDS
  } catch (error) {
    console.error('Token验证失败:', error)
    return false
  }
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('user_token') || '')
  const userInfo = ref(safeJsonParse(localStorage.getItem('userInfo'), null))

  // 计算属性：结合token存在性与有效性
  const isLoggedIn = computed(() => isTokenValid(token.value))
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const phone = computed(() => userInfo.value?.phone)
  const balance = computed(() => userInfo.value?.balance)
  const carbonCredits = computed(() => userInfo.value?.carbonCredits)

  // 设置 token
  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('user_token', newToken)
  }

  // 设置用户信息
  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 登录
  function login(newToken, info, expiresIn = 24 * 60 * 60) {
    setToken(newToken)
    setUserInfo(info)

    // 计算token过期时间（毫秒）
    const expireTime = Date.now() + expiresIn * 1000
    localStorage.setItem('user_token_expire', expireTime.toString())
  }

  // 登出
  async function logout() {
    // 通知后端将token加入黑名单
    try {
      await authLogout()
    } catch (error) {
      // 即使后端调用失败也继续清理本地状态
      console.error('后端登出失败:', error)
    }
    token.value = ''
    userInfo.value = null
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

  // 更新用户信息
  function updateUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  // 验证token是否有效
  function validateToken() {
    return isTokenValid(token.value)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userId,
    username,
    phone,
    balance,
    carbonCredits,
    setToken,
    setUserInfo,
    login,
    logout,
    updateUserInfo,
    validateToken
  }
})
