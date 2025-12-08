import request from '@/utils/request'

/**
 * 获取积分余额
 * @param {number} userId 用户ID
 */
export function getCreditBalance(userId) {
  return request({
    url: '/api/credits/balance',
    method: 'get',
    params: { userId }
  })
}

/**
 * 获取积分记录
 * @param {object} params 查询参数 {userId, creditType}
 */
export function getCreditHistory(params) {
  return request({
    url: '/api/credits/history',
    method: 'get',
    params
  })
}

/**
 * 每日签到
 * @param {number} userId 用户ID
 */
export function dailyCheckIn(userId) {
  return request({
    url: '/api/credits/checkin',
    method: 'post',
    params: { userId }
  })
}

/**
 * 获取积分统计
 * @param {number} userId 用户ID
 */
export function getCreditStatistics(userId) {
  return request({
    url: '/api/credits/statistics',
    method: 'get',
    params: { userId }
  })
}

/**
 * 兑换积分
 * @param {number} userId 用户ID
 * @param {object} data 兑换信息 {amount, description}
 */
export function redeemCredits(userId, data) {
  return request({
    url: '/api/credits/redeem',
    method: 'post',
    params: { userId },
    data
  })
}
