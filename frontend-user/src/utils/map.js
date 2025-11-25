/**
 * 高德地图工具类
 * 需要在 index.html 中引入高德地图 JS API
 */

// 高德地图 API Key（需要替换为实际的 Key）
export const AMAP_KEY = 'your-amap-key-here'

/**
 * 初始化地图
 * @param {string} containerId 地图容器ID
 * @param {object} options 地图配置选项
 * @returns {object} 地图实例
 */
export function initMap(containerId, options = {}) {
  if (!window.AMap) {
    console.error('高德地图 API 未加载')
    return null
  }

  const defaultOptions = {
    zoom: 13,
    center: [116.397428, 39.90923], // 默认中心点（北京）
    viewMode: '3D',
    ...options
  }

  return new window.AMap.Map(containerId, defaultOptions)
}

/**
 * 添加标记点
 * @param {object} map 地图实例
 * @param {number} lng 经度
 * @param {number} lat 纬度
 * @param {object} options 标记配置
 * @returns {object} 标记实例
 */
export function addMarker(map, lng, lat, options = {}) {
  if (!map || !window.AMap) return null

  return new window.AMap.Marker({
    position: [lng, lat],
    map,
    ...options
  })
}

/**
 * 获取当前位置
 * @returns {Promise} 位置信息 {lng, lat}
 */
export function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    if (!window.AMap) {
      reject(new Error('高德地图 API 未加载'))
      return
    }

    window.AMap.plugin('AMap.Geolocation', function() {
      const geolocation = new window.AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000
      })

      geolocation.getCurrentPosition((status, result) => {
        if (status === 'complete') {
          resolve({
            lng: result.position.lng,
            lat: result.position.lat,
            address: result.formattedAddress
          })
        } else {
          reject(new Error('定位失败'))
        }
      })
    })
  })
}

/**
 * 计算两点之间的距离
 * @param {number} lng1 第一个点的经度
 * @param {number} lat1 第一个点的纬度
 * @param {number} lng2 第二个点的经度
 * @param {number} lat2 第二个点的纬度
 * @returns {number} 距离（米）
 */
export function calculateDistance(lng1, lat1, lng2, lat2) {
  if (!window.AMap) return 0

  const point1 = new window.AMap.LngLat(lng1, lat1)
  const point2 = new window.AMap.LngLat(lng2, lat2)

  return point1.distance(point2)
}

/**
 * 格式化距离显示
 * @param {number} distance 距离（米）
 * @returns {string} 格式化后的距离
 */
export function formatDistance(distance) {
  if (distance < 1000) {
    return `${Math.round(distance)}米`
  } else {
    return `${(distance / 1000).toFixed(2)}公里`
  }
}
