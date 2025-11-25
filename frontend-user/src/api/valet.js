import request from '@/utils/request'

// 代充师傅注册
export function registerCharger(data) {
  return request({
    url: '/valet/charger/register',
    method: 'post',
    data
  })
}

// 获取代充师傅列表
export function getChargerList(params) {
  return request({
    url: '/valet/charger/list',
    method: 'get',
    params
  })
}

// 更新代充师傅状态
export function updateChargerStatus(chargerId, status) {
  return request({
    url: `/valet/charger/${chargerId}/status`,
    method: 'put',
    params: { status }
  })
}

// 创建代充订单
export function createValetOrder(data) {
  return request({
    url: '/valet/order/create',
    method: 'post',
    data
  })
}

// 接单
export function acceptOrder(orderNo) {
  return request({
    url: `/valet/order/${orderNo}/accept`,
    method: 'post'
  })
}

// 完成订单
export function completeOrder(orderNo, data) {
  return request({
    url: `/valet/order/${orderNo}/complete`,
    method: 'post',
    data
  })
}

// 获取我的代充订单
export function getMyValetOrders(params) {
  return request({
    url: '/valet/order/my',
    method: 'get',
    params
  })
}

// 获取师傅的订单
export function getChargerOrders(chargerId) {
  return request({
    url: `/valet/order/charger/${chargerId}`,
    method: 'get'
  })
}
