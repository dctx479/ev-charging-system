import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logout as authLogout } from '@/api/auth'

export const useAdminStore = defineStore('admin', () => {
  // 安全的JSON解析函数
  const safeJSONParse = (jsonString, defaultValue = null) => {
    try {
      return jsonString ? JSON.parse(jsonString) : defaultValue
    } catch (error) {
      console.error('JSON解析失败:', error)
      return defaultValue
    }
  }

  const token = ref(localStorage.getItem('admin_token') || '')
  const adminInfo = ref(safeJSONParse(localStorage.getItem('admin_info')))

  /**
   * 登录并保存token（带过期时间）
   * @param {string} newToken - JWT token
   * @param {object} info - 管理员信息
   * @param {number} expiresIn - token有效期（秒），默认24小时
   */
  function login(newToken, info, expiresIn = 24 * 60 * 60) {
    token.value = newToken
    adminInfo.value = info

    // 计算token过期时间（毫秒）
    const expireTime = Date.now() + expiresIn * 1000

    localStorage.setItem('admin_token', newToken)
    localStorage.setItem('admin_info', JSON.stringify(info))
    localStorage.setItem('admin_token_expire', expireTime.toString())
  }

  /**
   * 登出并清除所有存储
   */
  function logout() {
    // 调用后端API使token失效（忽略失败，确保前端一定清除状态）
    authLogout().catch(() => {})
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    localStorage.removeItem('admin_token_expire')

    // 清理关联缓存（admin开头或cache开头的缓存项）
    const keysToRemove = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && (key.startsWith('cache_') || key.startsWith('admin_') || key.startsWith('dashboard_'))) {
        keysToRemove.push(key)
      }
    }
    keysToRemove.forEach(key => localStorage.removeItem(key))
  }

  /**
   * 检查token是否有效
   * @returns {boolean}
   */
  function isTokenValid() {
    if (!token.value) return false

    // 方法1：检查保存的过期时间（毫秒）
    const expireTime = localStorage.getItem('admin_token_expire')
    if (expireTime) {
      // 提前60秒认为过期，避免边界问题
      const BUFFER_MS = 60 * 1000
      return Date.now() < parseInt(expireTime) - BUFFER_MS
    }

    // 方法2：解析JWT中的exp字段（秒级）
    try {
      const payload = token.value.split('.')[1]
      const decoded = JSON.parse(atob(payload))
      if (decoded.exp) {
        const BUFFER_SECONDS = 60
        const now = Math.floor(Date.now() / 1000)
        return decoded.exp > now + BUFFER_SECONDS
      }
    } catch (error) {
      console.error('Token解析失败:', error)
    }

    // 如果没有过期时间记录，认为token无效（安全性考虑）
    return false
  }

  return {
    token,
    adminInfo,
    login,
    logout,
    isTokenValid
  }
})
