import request from '@/utils/request'

/**
 * 创建订单（开始充电）
 */
export function createOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

/**
 * 结束充电
 */
export function endCharging(orderId, data) {
  return request({
    url: `/orders/${orderId}/end`,
    method: 'put',
    data
  })
}

/**
 * 获取订单列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（从0开始）
 * @param {number} params.size - 每页数量
 * @param {number} [params.orderStatus] - 订单状态：1待支付 2充电中 3已完成 4已取消 5异常
 * @param {number} [params.paymentStatus] - 支付状态：0未支付 1已支付 2退款中 3已退款
 * @param {string} [params.orderNo] - 订单号（模糊搜索）
 * @param {string} [params.startTime] - 开始时间（格式：YYYY-MM-DD HH:mm:ss）
 * @param {string} [params.endTime] - 结束时间（格式：YYYY-MM-DD HH:mm:ss）
 */
export function getOrderList(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(orderId) {
  return request({
    url: `/orders/${orderId}`,
    method: 'get'
  })
}

/**
 * 取消订单
 */
export function cancelOrder(orderId) {
  return request({
    url: `/orders/${orderId}/cancel`,
    method: 'put'
  })
}

/**
 * 支付订单
 */
export function payOrder(orderId, data) {
  return request({
    url: `/orders/${orderId}/pay`,
    method: 'post',
    data
  })
}

/**
 * 获取当前进行中的订单
 */
export function getCurrentOrder() {
  return request({
    url: '/orders/current',
    method: 'get'
  })
}
