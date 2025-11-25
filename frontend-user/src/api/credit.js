import request from '@/utils/request'

/**
 * 获取积分余额
 */
export function getCreditBalance() {
  return request({
    url: '/credits/balance',
    method: 'get'
  })
}

/**
 * 获取积分记录
 * @param {object} params 查询参数 {creditType}
 */
export function getCreditHistory(params) {
  return request({
    url: '/credits/history',
    method: 'get',
    params
  })
}

/**
 * 每日签到
 */
export function dailyCheckIn() {
  return request({
    url: '/credits/checkin',
    method: 'post'
  })
}

/**
 * 获取积分统计
 */
export function getCreditStatistics() {
  return request({
    url: '/credits/statistics',
    method: 'get'
  })
}

/**
 * 获取待发放积分
 */
export function getPendingCredits() {
  return request({
    url: '/credits/pending',
    method: 'get'
  })
}

/**
 * 兑换积分
 * @param {object} data 兑换信息 {amount, description}
 */
export function redeemCredits(data) {
  return request({
    url: '/credits/redeem',
    method: 'post',
    data
  })
}

/**
 * 获取可兑换商品列表
 * @param {object} params 查询参数 {category}
 */
export function getCreditProducts(params) {
  return request({
    url: '/credit-products/products',
    method: 'get',
    params
  })
}

/**
 * 兑换商品
 * @param {object} data 兑换信息 {productId, address, receiverName, receiverPhone}
 */
export function exchangeProduct(data) {
  return request({
    url: '/credit-products/exchange',
    method: 'post',
    data
  })
}

/**
 * 获取兑换记录
 */
export function getExchangeRecords() {
  return request({
    url: '/credit-products/exchange/records',
    method: 'get'
  })
}



