import request from '@/utils/request'

// 获取V2G记录列表（管理端：全平台）
export function getV2GRecordList(params) {
  return request({
    url: '/v2g/admin/records',
    method: 'get',
    params
  })
}

// 获取V2G统计数据（管理端：全平台，不需要 userId）
export function getV2GStatistics(params) {
  return request({
    url: '/v2g/admin/statistics',
    method: 'get',
    params
  })
}
