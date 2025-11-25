import request from '@/utils/request'

/**
 * 获取所有营业中的充电站
 */
export function getActiveStations() {
  return request({
    url: '/stations',
    method: 'get'
  })
}

/**
 * 获取充电站详情
 * @param {number} id 充电站ID
 */
export function getStationDetail(id) {
  return request({
    url: `/stations/${id}`,
    method: 'get'
  })
}

/**
 * 搜索充电站
 * @param {string} keyword 关键词
 */
export function searchStations(keyword) {
  return request({
    url: '/stations/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 查询附近的充电站
 * @param {number} latitude 纬度
 * @param {number} longitude 经度
 * @param {number} radius 搜索半径（千米）
 */
export function getNearbyStations(latitude, longitude, radius = 5) {
  return request({
    url: '/stations/nearby',
    method: 'get',
    params: { latitude, longitude, radius }
  })
}
