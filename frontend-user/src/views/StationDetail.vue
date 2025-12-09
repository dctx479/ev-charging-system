<template>
  <div class="station-detail">
    <van-nav-bar
      title="充电站详情"
      left-arrow
      @click-left="onClickLeft"
    />

    <div v-if="loading" class="loading-container">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <div v-else-if="station" class="content">
      <!-- 充电站图片 -->
      <van-image
        v-if="station.imageUrl"
        :src="station.imageUrl"
        height="200"
        fit="cover"
      />

      <!-- 充电站信息 -->
      <van-cell-group inset>
        <van-cell :title="station.name" :label="station.address">
          <template #icon>
            <van-icon name="shop-o" size="20" style="margin-right: 10px;" />
          </template>
        </van-cell>

        <van-cell title="营业时间" :value="station.businessHours" />
        <van-cell title="联系电话" :value="station.phone" is-link />

        <van-cell title="充电桩数量">
          <template #value>
            <span>可用 {{ station.availablePiles }}/{{ station.totalPiles }}</span>
          </template>
        </van-cell>

        <van-cell title="评分">
          <template #value>
            <van-rate v-model="station.rating" :size="16" readonly />
            <span style="margin-left: 5px;">{{ station.rating }}</span>
          </template>
        </van-cell>

        <van-cell
          v-if="station.distance"
          title="距离"
          :value="station.distanceText"
        />
      </van-cell-group>

      <!-- 充电站描述 -->
      <van-cell-group v-if="station.description" inset>
        <van-cell title="充电站介绍" :label="station.description" />
      </van-cell-group>

      <!-- 地图展示 -->
      <div class="map-section">
        <div id="station-map" class="map-container"></div>
        <!-- 距离显示 -->
        <div v-if="distanceToStation" class="distance-badge">
          <van-icon name="location-o" size="12" />
          <span>{{ distanceToStation }}</span>
        </div>
        <!-- 导航按钮 -->
        <van-button
          type="primary"
          size="small"
          round
          icon="guide-o"
          class="nav-button"
          @click="handleNavigation"
        >
          导航到这里
        </van-button>
      </div>

      <!-- 排队信息 -->
      <van-cell-group v-if="queueInfo" inset title="排队信息">
        <van-cell title="当前排队人数">
          <template #value>
            <span :style="{ color: queueInfo.queueCount > 5 ? '#ff6b6b' : '#07c160' }">
              {{ queueInfo.queueCount }}人
            </span>
          </template>
        </van-cell>
        <van-cell
          v-if="queueInfo.queueCount > 0"
          title="预计等待时间"
          :value="`约${queueInfo.averageWaitTime}分钟`"
        />
        <van-cell title="建议" :label="queueInfo.suggestion">
          <template #icon>
            <van-icon
              :name="queueInfo.recommendQueue ? 'success' : 'warning-o'"
              :color="queueInfo.recommendQueue ? '#07c160' : '#ff976a'"
              size="18"
              style="margin-right: 8px;"
            />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <van-button
          v-if="station.availablePiles > 0"
          type="primary"
          size="large"
          round
          @click="goToPileList"
        >
          查看充电桩
        </van-button>
        <van-button
          v-else
          type="warning"
          size="large"
          round
          :loading="joiningQueue"
          @click="handleJoinQueue"
        >
          加入排队
        </van-button>
      </div>
    </div>

    <van-empty v-else description="充电站不存在" />

    <!-- 导航方式选择面板 -->
    <van-action-sheet
      v-model:show="showNavSheet"
      :actions="navActions"
      cancel-text="取消"
      description="选择导航方式"
      @select="onNavSelect"
    />

    <!-- 地图应用选择面板 -->
    <van-action-sheet
      v-model:show="showMapSheet"
      :actions="mapActions"
      cancel-text="取消"
      description="选择地图应用"
      @select="onMapSelect"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast, showSuccessToast, closeToast } from 'vant'
import { getStationDetail } from '@/api/station'
import { getStationQueueInfo, joinQueue } from '@/api/queue'
import { initMap, addMarker, getCurrentPosition, calculateDistance, formatDistance } from '@/utils/map'

const router = useRouter()
const route = useRoute()

const station = ref(null)
const loading = ref(true)
const queueInfo = ref(null)
const joiningQueue = ref(false)
const mapInstance = ref(null) // 地图实例
const markerInstance = ref(null) // 标记实例
const userPosition = ref(null) // 用户当前位置
const distanceToStation = ref(null) // 到充电站的距离
const showNavSheet = ref(false) // 导航方式选择面板
const showMapSheet = ref(false) // 地图应用选择面板
const selectedNavMode = ref('') // 选中的导航方式
const isGettingPosition = ref(false) // 是否正在获取位置

// 导航方式列表
const navActions = [
  { name: '驾车导航', value: 'driving', icon: 'car-o' },
  { name: '步行导航', value: 'walking', icon: 'user-o' },
  { name: '骑行导航', value: 'riding', icon: 'balance-o' },
  { name: '公交导航', value: 'transit', icon: 'cluster-o' }
]

// 地图应用列表
const mapActions = [
  { name: '高德地图', value: 'amap' },
  { name: '百度地图', value: 'baidu' },
  { name: '腾讯地图', value: 'tencent' }
]

const onClickLeft = () => {
  router.back()
}

const goToPileList = () => {
  router.push(`/piles/${station.value.id}`)
}

const handleJoinQueue = async () => {
  joiningQueue.value = true
  try {
    await joinQueue({ stationId: station.value.id })
    showToast('加入排队成功')
    // 跳转到排队状态页面
    setTimeout(() => {
      router.push('/queue/status')
    }, 1000)
  } catch (error) {
    const message = error.response?.data?.message || '加入排队失败'
    showToast(message)
  } finally {
    joiningQueue.value = false
  }
}

const loadStationDetail = async () => {
  loading.value = true
  try {
    const res = await getStationDetail(route.params.id)
    station.value = res.data
    // 加载排队信息
    await loadQueueInfo()
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const loadQueueInfo = async () => {
  try {
    const res = await getStationQueueInfo(route.params.id)
    queueInfo.value = res.data
  } catch (error) {
    console.error('加载排队信息失败:', error)
  }
}

/**
 * 初始化地图
 * 在站点数据加载完成后调用
 */
const initStationMap = () => {
  if (!station.value || !station.value.longitude || !station.value.latitude) {
    console.warn('站点坐标信息不完整，无法初始化地图')
    return
  }

  // 初始化地图实例，中心点设置为充电站坐标
  mapInstance.value = initMap('station-map', {
    zoom: 16, // 街道级别缩放
    center: [station.value.longitude, station.value.latitude]
  })

  if (!mapInstance.value) {
    console.error('地图初始化失败')
    return
  }

  // 在充电站位置添加标记
  markerInstance.value = addMarker(
    mapInstance.value,
    station.value.longitude,
    station.value.latitude,
    {
      title: station.value.name,
      icon: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png'
    }
  )

  // 添加点击事件，显示充电站信息
  if (markerInstance.value && window.AMap) {
    markerInstance.value.on('click', () => {
      const infoWindow = new window.AMap.InfoWindow({
        content: `<div style="padding: 8px;">
          <h4 style="margin: 0 0 8px 0; font-size: 14px;">${station.value.name}</h4>
          <p style="margin: 0; font-size: 12px; color: #666;">${station.value.address}</p>
        </div>`
      })
      infoWindow.open(mapInstance.value, [station.value.longitude, station.value.latitude])
    })
  }
}

/**
 * 获取用户当前位置并计算距离
 */
const loadUserPosition = async () => {
  try {
    const position = await getCurrentPosition()
    userPosition.value = position

    // 计算到充电站的距离
    if (station.value && station.value.longitude && station.value.latitude) {
      const distance = calculateDistance(
        position.lng,
        position.lat,
        station.value.longitude,
        station.value.latitude
      )
      distanceToStation.value = formatDistance(distance)
    }
  } catch (error) {
    console.error('获取位置失败:', error)
    // 位置获取失败不影响其他功能
  }
}

/**
 * 点击导航按钮 - 打开导航方式选择面板
 */
const handleNavigation = async () => {
  console.log('handleNavigation called')
  console.log('Station:', station.value)

  if (!station.value || !station.value.longitude || !station.value.latitude) {
    showToast('站点坐标信息不完整')
    return
  }

  // 如果还没有获取用户位置，尝试获取
  if (!userPosition.value && !isGettingPosition.value) {
    isGettingPosition.value = true
    showLoadingToast({
      message: '正在获取位置...',
      forbidClick: true,
      duration: 0
    })

    try {
      const position = await getCurrentPosition()
      userPosition.value = position

      // 计算到充电站的距离
      const distance = calculateDistance(
        position.lng,
        position.lat,
        station.value.longitude,
        station.value.latitude
      )
      distanceToStation.value = formatDistance(distance)

      closeToast()
      showSuccessToast('位置获取成功')
    } catch (error) {
      console.error('获取位置失败:', error)
      closeToast()
      showToast('获取位置失败，将使用地图自动定位')
      // 位置获取失败不阻止导航，继续打开导航面板
    } finally {
      isGettingPosition.value = false
    }
  }

  // 打开导航方式选择面板
  console.log('Opening nav sheet')
  showNavSheet.value = true
}

/**
 * 选择导航方式后 - 打开地图应用选择面板
 */
const onNavSelect = (action) => {
  selectedNavMode.value = action.value
  showNavSheet.value = false

  // 延迟打开第二个面板，确保第一个面板关闭动画完成
  setTimeout(() => {
    showMapSheet.value = true
  }, 300)
}

/**
 * 选择地图应用后 - 执行导航跳转
 */
const onMapSelect = (action) => {
  showMapSheet.value = false

  const { longitude, latitude, name } = station.value
  const mapType = action.value
  const mode = selectedNavMode.value

  // 获取起始位置坐标（如果有）
  const hasUserPosition = userPosition.value !== null
  const fromLng = hasUserPosition ? userPosition.value.lng : null
  const fromLat = hasUserPosition ? userPosition.value.lat : null

  let navUrl = ''

  // 根据不同地图应用生成导航URL
  switch (mapType) {
    case 'amap':
      // 高德地图导航
      // mode: car(驾车), walking(步行), riding(骑行), transit(公交)
      const amapMode = {
        driving: 'car',
        walking: 'walking',
        riding: 'riding',
        transit: 'transit'
      }[mode] || 'car'

      // 如果有用户位置，添加起始地参数
      if (hasUserPosition) {
        navUrl = `https://uri.amap.com/navigation?from=${fromLng},${fromLat},${encodeURIComponent('我的位置')}&to=${longitude},${latitude},${encodeURIComponent(name)}&mode=${amapMode}`
      } else {
        navUrl = `https://uri.amap.com/navigation?to=${longitude},${latitude},${encodeURIComponent(name)}&mode=${amapMode}`
      }
      break

    case 'baidu':
      // 百度地图导航
      // mode: driving(驾车), walking(步行), riding(骑行), transit(公交)
      const baiduMode = {
        driving: 'driving',
        walking: 'walking',
        riding: 'riding',
        transit: 'transit'
      }[mode] || 'driving'

      // 如果有用户位置，添加起始地参数
      if (hasUserPosition) {
        navUrl = `http://api.map.baidu.com/direction?origin=latlng:${fromLat},${fromLng}|name:${encodeURIComponent('我的位置')}&destination=latlng:${latitude},${longitude}|name:${encodeURIComponent(name)}&mode=${baiduMode}&coord_type=gcj02&output=html`
      } else {
        navUrl = `http://api.map.baidu.com/direction?destination=latlng:${latitude},${longitude}|name:${encodeURIComponent(name)}&mode=${baiduMode}&coord_type=gcj02&output=html`
      }
      break

    case 'tencent':
      // 腾讯地图导航
      // type: drive(驾车), walk(步行), bike(骑行), bus(公交)
      const tencentMode = {
        driving: 'drive',
        walking: 'walk',
        riding: 'bike',
        transit: 'bus'
      }[mode] || 'drive'

      // 如果有用户位置，添加起始地参数
      if (hasUserPosition) {
        navUrl = `https://apis.map.qq.com/uri/v1/routeplan?type=${tencentMode}&from=${encodeURIComponent('我的位置')}&fromcoord=${fromLat},${fromLng}&to=${encodeURIComponent(name)}&tocoord=${latitude},${longitude}`
      } else {
        navUrl = `https://apis.map.qq.com/uri/v1/routeplan?type=${tencentMode}&to=${encodeURIComponent(name)}&tocoord=${latitude},${longitude}`
      }
      break
  }

  if (navUrl) {
    // 打开对应地图应用进行导航
    window.location.href = navUrl
  }
}

onMounted(async () => {
  await loadStationDetail()

  // 加载用户位置并计算距离
  loadUserPosition()

  // 等待DOM更新后初始化地图
  // 确保地图容器已经渲染
  if (station.value) {
    setTimeout(() => {
      initStationMap()
    }, 100)
  }
})

onUnmounted(() => {
  // 销毁地图实例，防止内存泄漏
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
  if (markerInstance.value) {
    markerInstance.value = null
  }
})
</script>

<style scoped>
.station-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.content {
  padding-bottom: 80px;
}

.van-cell-group {
  margin-top: 12px;
}

/* 地图展示区域 */
.map-section {
  position: relative;
  margin: 12px 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-container {
  width: 100%;
  height: 200px;
  background-color: #e5e5e5;
}

/* 距离徽章 */
.distance-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: rgba(255, 255, 255, 0.95);
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  color: #323233;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.distance-badge span {
  font-weight: 500;
}

/* 导航按钮 */
.nav-button {
  position: absolute;
  bottom: 12px;
  right: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.action-buttons {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background-color: white;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
}
</style>
