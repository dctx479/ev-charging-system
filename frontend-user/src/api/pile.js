import request from '@/utils/request'

/**
 * 获取充电站的充电桩列表
 * @param {number} stationId 充电站ID
 */
export function getPilesByStation(stationId) {
  return request({
    url: `/piles/station/${stationId}`,
    method: 'get'
  })
}

/**
 * 获取充电站的可用充电桩
 * @param {number} stationId 充电站ID
 */
export function getAvailablePiles(stationId) {
  return request({
    url: `/piles/station/${stationId}/available`,
    method: 'get'
  })
}

/**
 * 获取充电桩详情
 * @param {number} id 充电桩ID
 */
export function getPileDetail(id) {
  return request({
    url: `/piles/${id}`,
    method: 'get'
  })
}

/**
 * 获取充电桩列表（支持按状态/关键词过滤）
 * @param {Object} params 查询参数 {page, size, status, keyword}
 */
export function getPileList(params) {
  return request({
    url: '/piles',
    method: 'get',
    params
  })
}
