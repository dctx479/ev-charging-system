<template>
  <div class="nearby-poi-search">
    <van-nav-bar
      title="周边服务搜索"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    />

    <div class="content">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <van-search
          v-model="searchKeyword"
          placeholder="搜索餐饮、娱乐、购物等"
          show-action
          @search="handleSearch"
        >
          <template #action>
            <div @click="handleSearch">搜索</div>
          </template>
        </van-search>
      </div>

      <!-- 快捷分类按钮 -->
      <div class="quick-categories">
        <van-button
          v-for="cat in quickCategories"
          :key="cat.value"
          :type="activeCategory === cat.value ? 'primary' : 'default'"
          size="small"
          round
          @click="selectCategory(cat.value)"
        >
          <van-icon :name="cat.icon" />
          {{ cat.label }}
        </van-button>
      </div>

      <!-- 距离筛选 -->
      <div class="distance-filter">
        <span class="filter-label">距离:</span>
        <van-button
          v-for="dist in distanceOptions"
          :key="dist.value"
          :type="selectedRadius === dist.value ? 'primary' : 'default'"
          size="mini"
          plain
          @click="selectDistance(dist.value)"
        >
          {{ dist.label }}
        </van-button>
      </div>

      <!-- POI列表 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="poiList.length > 0" class="poi-list">
            <van-card
              v-for="poi in poiList"
              :key="poi.id"
              :title="poi.name"
              :thumb="poi.photos && poi.photos.length > 0 ? poi.photos[0] : ''"
              @click="handlePoiClick(poi)"
            >
              <template #tags>
                <van-tag type="primary" size="small">{{ poi.type }}</van-tag>
                <van-tag v-if="poi.rating" type="success" size="small" style="margin-left: 4px;">
                  <van-icon name="star" /> {{ poi.rating }}
                </van-tag>
              </template>

              <template #desc>
                <div class="poi-info">
                  <div class="poi-address">
                    <van-icon name="location-o" />
                    {{ poi.address }}
                  </div>
                  <div class="poi-meta">
                    <span class="poi-distance">
                      <van-icon name="guide-o" />
                      {{ formatDistance(poi.distance) }}
                    </span>
                    <span v-if="poi.businessHours" class="poi-hours">
                      <van-icon name="clock-o" />
                      {{ poi.businessHours }}
                    </span>
                  </div>
                  <div v-if="poi.tel" class="poi-contact">
                    <van-icon name="phone-o" />
                    {{ poi.tel }}
                  </div>
                </div>
              </template>

              <template #footer>
                <van-button size="small" type="primary" @click.stop="handleCall(poi.tel)" v-if="poi.tel">
                  拨打电话
                </van-button>
                <van-button size="small" type="success" @click.stop="handleNavigate(poi)">
                  导航
                </van-button>
              </template>
            </van-card>
          </div>

          <van-empty v-else-if="!loading" description="暂无周边服务" />
        </van-list>
      </van-pull-refresh>
    </div>

    <!-- 地图弹窗 -->
    <van-popup
      v-model:show="showMap"
      position="bottom"
      :style="{ height: '70%' }"
      round
    >
      <div class="map-popup">
        <div class="map-header">
          <div class="map-title">
            <div class="poi-name">{{ selectedPoi?.name }}</div>
            <div class="poi-address-small">{{ selectedPoi?.address }}</div>
          </div>
          <van-icon name="cross" @click="showMap = false" />
        </div>
        <div id="poi-map" class="poi-map-container"></div>
        <div class="map-actions">
          <van-button type="primary" block round @click="handleNavigateFromMap">
            <van-icon name="guide-o" />
            导航到这里
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showDialog } from 'vant'
import request from '@/utils/request'

const route = useRoute()

// 当前位置（从路由参数获取）
const currentLongitude = ref(route.query.longitude || '116.397428')
const currentLatitude = ref(route.query.latitude || '39.90923')
const stationName = ref(route.query.stationName || '当前位置')

// 快捷分类
const quickCategories = [
  { label: '全部', value: 'all', icon: 'apps-o' },
  { label: '美食', value: 'restaurants', icon: 'food-o' },
  { label: '咖啡', value: 'coffee', icon: 'coffee-o' },
  { label: '电影', value: 'cinema', icon: 'video-o' },
  { label: '购物', value: 'shopping', icon: 'shopping-cart-o' },
  { label: '娱乐', value: 'entertainment', icon: 'music-o' },
  { label: '酒店', value: 'hotels', icon: 'hotel-o' }
]

// 距离选项
const distanceOptions = [
  { label: '500m', value: 500 },
  { label: '1km', value: 1000 },
  { label: '3km', value: 3000 },
  { label: '5km', value: 5000 }
]

// 搜索和分类
const searchKeyword = ref('')
const activeCategory = ref('all')
const selectedRadius = ref(1000)

// POI列表
const poiList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 地图相关
const showMap = ref(false)
const selectedPoi = ref(null)
let mapInstance = null

// 加载POI数据
const loadPoiData = async () => {
  try {
    let url = '/nearby/poi/search'
    const params = {
      longitude: currentLongitude.value,
      latitude: currentLatitude.value,
      radius: selectedRadius.value
    }

    // 根据分类选择不同的API
    if (activeCategory.value !== 'all') {
      url = `/nearby/poi/${activeCategory.value}`
    }

    // 如果有搜索关键词，添加到参数中
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    const res = await request({
      url,
      method: 'get',
      params
    })

    if (res.code === 200) {
      poiList.value = res.data || []
      loading.value = false
      finished.value = true
    }
  } catch (error) {
    console.error('加载POI数据失败:', error)
    showToast('加载失败，请重试')
    loading.value = false
  }
}

// 选择分类
const selectCategory = (category) => {
  if (activeCategory.value === category) return

  activeCategory.value = category
  poiList.value = []
  finished.value = false
  loading.value = true
  loadPoiData()
}

// 选择距离
const selectDistance = (radius) => {
  if (selectedRadius.value === radius) return

  selectedRadius.value = radius
  poiList.value = []
  finished.value = false
  loading.value = true
  loadPoiData()
}

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    showToast('请输入搜索关键词')
    return
  }

  poiList.value = []
  finished.value = false
  loading.value = true
  loadPoiData()
}

// 下拉刷新
const onRefresh = async () => {
  finished.value = false
  loading.value = true
  await loadPoiData()
  refreshing.value = false
  showToast('刷新成功')
}

// 加载更多
const onLoad = async () => {
  if (poiList.value.length === 0) {
    await loadPoiData()
  } else {
    finished.value = true
  }
}

// 点击POI卡片
const handlePoiClick = (poi) => {
  selectedPoi.value = poi
  showMap.value = true

  // 初始化地图
  setTimeout(() => {
    initMap(poi)
  }, 100)
}

// 拨打电话
const handleCall = (tel) => {
  if (!tel) {
    showToast('暂无联系电话')
    return
  }

  showDialog({
    title: '拨打电话',
    message: `确定拨打 ${tel} 吗？`,
    showCancelButton: true
  }).then(() => {
    window.location.href = `tel:${tel}`
  }).catch(() => {
    // 用户取消
  })
}

// 初始化地图
const initMap = (poi) => {
  if (!window.AMap) {
    showToast('地图加载失败')
    return
  }

  // 销毁旧地图实例
  if (mapInstance) {
    mapInstance.destroy()
  }

  // 创建新地图实例
  mapInstance = new window.AMap.Map('poi-map', {
    zoom: 16,
    center: [poi.longitude, poi.latitude]
  })

  // 添加POI标记
  new window.AMap.Marker({
    position: [poi.longitude, poi.latitude],
    title: poi.name,
    map: mapInstance,
    icon: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png'
  })

  // 添加当前位置标记
  new window.AMap.Marker({
    position: [currentLongitude.value, currentLatitude.value],
    title: '我的位置',
    map: mapInstance,
    icon: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png'
  })

  // 自动调整视野
  mapInstance.setFitView()
}

// 导航（从列表）
const handleNavigate = (poi) => {
  if (!poi || !poi.longitude || !poi.latitude) {
    showToast('位置信息不完整')
    return
  }

  const { longitude, latitude, name } = poi
  // 调用高德地图导航
  window.location.href = `https://uri.amap.com/navigation?to=${longitude},${latitude},${encodeURIComponent(name)}&mode=car&src=myapp&coordinate=gaode&callnative=1`
}

// 导航（从地图弹窗）
const handleNavigateFromMap = () => {
  if (!selectedPoi.value) return
  handleNavigate(selectedPoi.value)
}

// 格式化距离
const formatDistance = (distance) => {
  if (!distance) return '-'
  if (distance < 1000) {
    return `${Math.round(distance)}m`
  }
  return `${(distance / 1000).toFixed(1)}km`
}

onMounted(() => {
  loadPoiData()
})

onBeforeUnmount(() => {
  // 销毁地图实例
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.nearby-poi-search {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.content {
  padding-bottom: 20px;
}

.search-bar {
  background: white;
  padding: 8px 16px;
  margin-bottom: 8px;
}

/* 快捷分类按钮 */
.quick-categories {
  background: white;
  padding: 12px 16px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  margin-bottom: 8px;
}

.quick-categories::-webkit-scrollbar {
  display: none;
}

.quick-categories .van-button {
  flex-shrink: 0;
}

/* 距离筛选 */
.distance-filter {
  background: white;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.filter-label {
  font-size: 14px;
  color: #646566;
  font-weight: 500;
}

/* POI列表 */
.poi-list {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.poi-list .van-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.poi-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.poi-address {
  font-size: 13px;
  color: #646566;
  display: flex;
  align-items: center;
  gap: 4px;
}

.poi-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #969799;
}

.poi-distance,
.poi-hours {
  display: flex;
  align-items: center;
  gap: 4px;
}

.poi-contact {
  font-size: 13px;
  color: #1989fa;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 地图弹窗 */
.map-popup {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.map-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebedf0;
}

.map-title {
  flex: 1;
  margin-right: 16px;
}

.poi-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 4px;
}

.poi-address-small {
  font-size: 12px;
  color: #969799;
}

.poi-map-container {
  flex: 1;
  background-color: #e5e5e5;
}

.map-actions {
  padding: 16px;
  border-top: 1px solid #ebedf0;
  background: white;
}
</style>
