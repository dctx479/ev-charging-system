<template>
  <div class="home-container">
    <!-- 顶部搜索栏 -->
    <div class="header">
      <van-search
        v-model="searchKeyword"
        placeholder="搜索充电站"
        shape="round"
        @search="onSearch"
      />
    </div>

    <!-- 下拉刷新 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 充电站列表 -->
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div class="station-list">
          <station-card
            v-for="station in stationList"
            :key="station.id"
            :station="station"
            @click="goToDetail(station.id)"
          />
        </div>

        <van-empty
          v-if="!loading && stationList.length === 0"
          description="暂无充电站"
        />
      </van-list>
    </van-pull-refresh>

    <!-- 底部导航栏 -->
    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/" icon="shop-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>

    <!-- 定位按钮 -->
    <div class="location-btn" @click="getNearbyStations">
      <van-icon name="location-o" size="24" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getActiveStations, getNearbyStations as fetchNearbyStations } from '@/api/station'
import StationCard from '@/components/StationCard.vue'
import { getCurrentPosition } from '@/utils/map'

const router = useRouter()

const active = ref(0)
const searchKeyword = ref('')
const stationList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 加载充电站列表
const onLoad = async () => {
  try {
    const res = await getActiveStations()
    stationList.value = res.data
    loading.value = false
    finished.value = true
  } catch (error) {
    console.error('加载失败:', error)
    loading.value = false
  }
}

// 下拉刷新
const onRefresh = async () => {
  finished.value = false
  loading.value = true
  await onLoad()
  refreshing.value = false
  showToast('刷新成功')
}

// 搜索
const onSearch = () => {
  if (!searchKeyword.value.trim()) {
    return
  }
  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value }
  })
}

// 获取附近充电站
const getNearbyStations = async () => {
  const toast = showLoadingToast({
    message: '定位中...',
    forbidClick: true,
    duration: 0
  })

  try {
    const position = await getCurrentPosition()
    closeToast()

    const res = await fetchNearbyStations(position.lat, position.lng, 5)
    stationList.value = res.data

    showToast({
      message: `找到 ${res.data.length} 个附近充电站`,
      type: 'success'
    })
  } catch (error) {
    closeToast()
    showToast({
      message: '定位失败，请检查定位权限',
      type: 'fail'
    })
  }
}

// 跳转到详情页
const goToDetail = (id) => {
  router.push(`/station/${id}`)
}

onMounted(() => {
  onLoad()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: white;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.station-list {
  padding: 12px;
}

.location-btn {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: #1989fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.4);
  cursor: pointer;
  z-index: 99;
}

.location-btn:active {
  transform: scale(0.95);
}
</style>
