import request from '@/utils/request'

// 开始V2G放电
export function startDischarge(data) {
  return request({
    url: '/v2g/discharge/start',
    method: 'post',
    data
  })
}

// 停止V2G放电
export function stopDischarge(data) {
  return request({
    url: '/v2g/discharge/stop',
    method: 'post',
    data
  })
}

// 获取V2G记录
export function getV2GRecords() {
  return request({
    url: '/v2g/records',
    method: 'get'
  })
}

// 获取V2G统计数据
export function getV2GStatistics() {
  return request({
    url: '/v2g/statistics',
    method: 'get'
  })
}

// 获取电价预测
export function getPriceForecast() {
  return request({
    url: '/v2g/price/forecast',
    method: 'get'
  })
}

// 设置放电策略
export function setDischargeStrategy(data) {
  return request({
    url: '/v2g/strategy/set',
    method: 'post',
    data
  })
}

// 获取V2G收益预估
export function getProfitEstimate(params) {
  return request({
    url: '/v2g/profit-estimate',
    method: 'get',
    params
  })
}
