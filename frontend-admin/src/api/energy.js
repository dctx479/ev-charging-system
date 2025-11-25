import request from '@/utils/request'

// Helper function to convert period to date range
function getPeriodDateRange(period) {
  const now = new Date()
  let startTime = new Date()

  switch (period) {
    case 'today':
      startTime.setHours(0, 0, 0, 0)
      break
    case 'week':
      startTime.setDate(now.getDate() - 7)
      startTime.setHours(0, 0, 0, 0)
      break
    case 'month':
      startTime.setDate(now.getDate() - 30)
      startTime.setHours(0, 0, 0, 0)
      break
    default:
      startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  }

  const startStr = startTime.toISOString().split('T')[0] + ' ' + startTime.toTimeString().split(' ')[0]
  const endStr = now.toISOString().split('T')[0] + ' ' + now.toTimeString().split(' ')[0]

  return { startTime: startStr, endTime: endStr }
}

// 获取能源数据
export function getEnergyData(params) {
  // Handle stations data with dedicated endpoint
  if (params.type === 'stations') {
    return request({
      url: '/energy/stations',
      method: 'get'
    })
  }

  // Convert period to startTime/endTime if period is provided
  const convertedParams = { ...params }

  if (params.period && !params.startTime) {
    const dateRange = getPeriodDateRange(params.period)
    convertedParams.startTime = dateRange.startTime
    convertedParams.endTime = dateRange.endTime
    delete convertedParams.period
  }

  // Remove type parameter if present - it was used by frontend logic only
  delete convertedParams.type

  return request({
    url: '/energy/history',
    method: 'get',
    params: convertedParams
  })
}

// 获取能源统计
export function getEnergyStatistics(params) {
  return request({
    url: '/energy/statistics',
    method: 'get',
    params
  })
}

// 获取站点能源概览
export function getStationEnergyOverview(stationId) {
  return request({
    url: '/energy/realtime',
    method: 'get',
    params: { stationId }
  })
}
