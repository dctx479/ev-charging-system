import request from '@/utils/request'

/**
 * 获取仪表板统计数据
 * @returns {Promise} {todayChargeAmount, todayRevenue, onlinePileCount, faultPileCount}
 */
export function getDashboardStats() {
  return request({
    url: '/admin/statistics/dashboard',
    method: 'get'
  })
}

/**
 * 获取充电趋势数据
 * @param {Number} days - 天数 (默认7天)
 * @returns {Promise} {dates: [], chargeAmounts: []}
 */
export function getChargeTrend(days = 7) {
  return request({
    url: '/admin/statistics/trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取充电桩状态分布
 * @returns {Promise} [{name: '空闲', value: 50}, {name: '充电中', value: 30}, ...]
 */
export function getPileStatusDistribution() {
  return request({
    url: '/admin/statistics/pile-status',
    method: 'get'
  })
}

/**
 * 获取站点收入排行
 * @param {Number} limit - 返回数量 (默认Top 5)
 * @returns {Promise} [{stationName: '', revenue: 0}, ...]
 */
export function getStationRanking(limit = 5) {
  return request({
    url: '/admin/statistics/station-ranking',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取订单统计
 * @param {String} startDate - 开始日期
 * @param {String} endDate - 结束日期
 */
export function getOrderStatistics(startDate, endDate) {
  return request({
    url: '/admin/statistics/orders',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取收入统计
 * @param {String} period - 统计周期: day, week, month, year
 */
export function getRevenueStatistics(period = 'day') {
  return request({
    url: '/admin/statistics/revenue',
    method: 'get',
    params: { period }
  })
}
