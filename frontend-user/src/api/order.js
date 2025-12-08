import request from '@/utils/request'

/**
 * 创建订单（开始充电）
 */
export function createOrder(data) {
  return request({
    url: '/api/orders',
    method: 'post',
    data
  })
}

/**
 * 结束充电
 */
export function endCharging(orderId, data) {
  return request({
    url: `/api/orders/${orderId}/end`,
    method: 'put',
    data
  })
}

/**
 * 获取订单列表
 */
export function getOrderList(params) {
  return request({
    url: '/api/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(orderId) {
  return request({
    url: `/api/orders/${orderId}`,
    method: 'get'
  })
}

/**
 * 取消订单
 */
export function cancelOrder(orderId) {
  return request({
    url: `/api/orders/${orderId}/cancel`,
    method: 'put'
  })
}

/**
 * 支付订单
 */
export function payOrder(orderId, data) {
  return request({
    url: `/api/orders/${orderId}/pay`,
    method: 'post',
    data
  })
}
