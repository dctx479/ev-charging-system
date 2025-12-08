import request from '@/utils/request'

/**
 * 获取故障列表
 * @param {Object} params - 查询参数 {stationId, repairStatus, faultType, page, size}
 */
export function getFaultList(params) {
  return request({
    url: '/admin/faults',
    method: 'get',
    params
  })
}

/**
 * 上报故障
 * @param {Object} data - 故障数据
 */
export function reportFault(data) {
  return request({
    url: '/admin/faults',
    method: 'post',
    data
  })
}

/**
 * 更新维修状态
 * @param {Number} id - 故障ID
 * @param {Object} data - 更新数据 {repairStatus, repairPerson, repairStartTime, repairEndTime}
 */
export function updateRepairStatus(id, data) {
  return request({
    url: `/admin/faults/${id}/repair`,
    method: 'put',
    data
  })
}

/**
 * 获取故障详情
 * @param {Number} id - 故障ID
 */
export function getFaultDetail(id) {
  return request({
    url: `/admin/faults/${id}`,
    method: 'get'
  })
}

/**
 * 删除故障记录
 * @param {Number} id - 故障ID
 */
export function deleteFault(id) {
  return request({
    url: `/admin/faults/${id}`,
    method: 'delete'
  })
}

/**
 * 获取故障统计
 */
export function getFaultStatistics() {
  return request({
    url: '/admin/faults/statistics',
    method: 'get'
  })
}
