import request from '@/utils/request'

// 获取代充师傅列表
export function getChargerList(params) {
  return request({
    url: '/valet/charger/list',
    method: 'get',
    params
  })
}

// 审核师傅注册（更新认证状态）
export function auditCharger(chargerId, isCertified) {
  return request({
    url: `/valet/charger/${chargerId}/audit`,
    method: 'put',
    params: { isCertified }
  })
}

// 获取代充订单列表（管理端全量）
export function getValetOrderList(params) {
  return request({
    url: '/valet/order/admin/all',
    method: 'get',
    params
  })
}

// 获取代充统计数据
export function getValetStatistics() {
  return request({
    url: '/valet/statistics',
    method: 'get'
  })
}
