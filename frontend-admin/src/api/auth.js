import request from '@/utils/request'

/**
 * 管理员登录
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取管理员信息
 */
export function getAdminInfo() {
  return request({
    url: '/auth/user/info',
    method: 'get'
  })
}

/**
 * 管理员登出
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
