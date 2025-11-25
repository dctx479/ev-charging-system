import request from '@/utils/request'

/**
 * 获取所有充电站列表（用于筛选）
 */
export function getStationList() {
  return request({
    url: '/stations',
    method: 'get'
  })
}

/**
 * 获取订单列表
 * @param {Object} params - 查询参数 {orderNo, orderStatus, paymentStatus, stationId, startTime, endTime, page, size}
 */
export function getOrderList(params) {
  return request({
    url: '/admin/orders',
    method: 'get',
    params
  })
}

/**
 * 导出订单Excel
 * @param {Object} params - 查询参数
 */
export function exportOrders(params) {
  return request({
    url: '/admin/orders/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 获取订单详情
 * @param {Number} id - 订单ID
 */
export function getOrderDetail(id) {
  return request({
    url: `/admin/orders/detail/${id}`,
    method: 'get'
  })
}

/**
 * 更新订单状态
 * @param {Number} id - 订单ID
 * @param {Number} status - 订单状态 1进行中 2已完成 3已取消 4异常
 */
export function updateOrderStatus(id, status) {
  return request({
    url: `/admin/orders/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 订单退款
 * @param {Number} id - 订单ID
 * @param {Object} data - 退款数据 {refundReason, refundAmount}
 */
export function refundOrder(id, data) {
  return request({
    url: `/admin/orders/${id}/refund`,
    method: 'post',
    data
  })
}
