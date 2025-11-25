import request from '@/utils/request'

/**
 * 加入排队
 * @param {object} data 排队信息
 */
export function joinQueue(data) {
  return request({
    url: '/queue/join',
    method: 'post',
    data
  })
}

/**
 * 获取我的排队状态
 */
export function getQueueStatus() {
  return request({
    url: '/queue/status',
    method: 'get'
  })
}

/**
 * 离开队列
 */
export function leaveQueue() {
  return request({
    url: '/queue/leave',
    method: 'delete'
  })
}

/**
 * 获取站点排队信息
 * @param {number} stationId 充电站ID
 */
export function getStationQueueInfo(stationId) {
  return request({
    url: `/queue/station/${stationId}`,
    method: 'get'
  })
}
