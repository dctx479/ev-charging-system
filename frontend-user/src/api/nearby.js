import request from '@/utils/request'

// 根据等待时间推荐周边服务
export function getRecommendedServices(stationId, waitTime) {
  return request({
    url: '/nearby/recommend',
    method: 'get',
    params: { stationId, waitTime }
  })
}

// 获取站点所有周边服务
export function getAllServices(stationId) {
  return request({
    url: `/nearby/station/${stationId}`,
    method: 'get'
  })
}

// 根据服务类型获取周边服务
export function getServicesByType(stationId, serviceType) {
  return request({
    url: `/nearby/station/${stationId}/type/${serviceType}`,
    method: 'get'
  })
}

// 根据距离获取周边服务
export function getServicesByDistance(stationId, maxDistance = 1000) {
  return request({
    url: `/nearby/station/${stationId}/distance`,
    method: 'get',
    params: { maxDistance }
  })
}

// ==================== 高德地图POI搜索 ====================

// 搜索周边POI（通用）
export function searchNearbyPoi(longitude, latitude, types, radius = 1000, limit = 20) {
  return request({
    url: '/nearby/poi/search',
    method: 'get',
    params: { longitude, latitude, types, radius, limit }
  })
}

// 搜索周边餐饮
export function searchNearbyRestaurants(longitude, latitude, radius = 1000) {
  return request({
    url: '/nearby/poi/restaurants',
    method: 'get',
    params: { longitude, latitude, radius }
  })
}

// 搜索周边购物
export function searchNearbyShopping(longitude, latitude, radius = 1000) {
  return request({
    url: '/nearby/poi/shopping',
    method: 'get',
    params: { longitude, latitude, radius }
  })
}

// 搜索周边生活服务
export function searchNearbyLifeService(longitude, latitude, radius = 1000) {
  return request({
    url: '/nearby/poi/life-service',
    method: 'get',
    params: { longitude, latitude, radius }
  })
}

// 搜索周边休闲娱乐
export function searchNearbyEntertainment(longitude, latitude, radius = 1000) {
  return request({
    url: '/nearby/poi/entertainment',
    method: 'get',
    params: { longitude, latitude, radius }
  })
}

// 搜索周边酒店
export function searchNearbyHotels(longitude, latitude, radius = 1000) {
  return request({
    url: '/nearby/poi/hotels',
    method: 'get',
    params: { longitude, latitude, radius }
  })
}
