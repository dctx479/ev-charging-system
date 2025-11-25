import request from '@/utils/request'

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

/**
 * 更新用户信息
 * @param {object} data 用户信息 {nickname, email, carModel, batteryCapacity}
 */
export function updateUserInfo(data) {
  return request({
    url: '/users/me',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param {object} data 密码信息 {oldPassword, newPassword}
 */
export function changePassword(data) {
  return request({
    url: '/users/password',
    method: 'put',
    data
  })
}

/**
 * 上传用户头像
 * @param {FormData} formData 包含头像文件的FormData
 */
export function uploadAvatar(formData) {
  return request({
    url: '/users/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': undefined  // 让浏览器自动设置multipart boundary
    }
  })
}
