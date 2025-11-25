import request from '@/utils/request'

// 获取实时能源数据
export function getRealtimeEnergy(params) {
  return request({
    url: '/energy/realtime',
    method: 'get',
    params
  })
}

// 获取能源历史数据
export function getEnergyHistory(params) {
  return request({
    url: '/energy/history',
    method: 'get',
    params
  })
}

// 获取能源统计数据
export function getEnergyStatistics(params) {
  return request({
    url: '/energy/statistics',
    method: 'get',
    params
  })
}
