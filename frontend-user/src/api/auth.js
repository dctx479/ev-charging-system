import request from '@/utils/request'

/**
 * 用户登录
 * @param {object} data 登录信息 {username, password}
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {object} data 注册信息
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息
 * 注：后端从JWT令牌中提取userId，无需前端传递
 */
export function getUserInfo() {
  return request({
    url: '/auth/user/info',
    method: 'get'
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
