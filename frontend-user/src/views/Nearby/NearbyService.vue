<template>
  <div class="nearby-service">
    <van-nav-bar
      title="周边服务"
      left-arrow
      @click-left="$router.back()"
    />

    <!-- 等待时间提示 -->
    <div class="wait-time-banner" v-if="waitTime">
      <van-icon name="clock-o" size="20" />
      <div class="banner-content">
        <div class="wait-text">预计等待时间</div>
        <div class="wait-value">{{ waitTime }} 分钟</div>
      </div>
      <div class="banner-tip">为您推荐附近可以逛的地方</div>
    </div>

    <!-- 服务类型筛选 -->
    <van-tabs v-model:active="activeType" @change="onTypeChange">
      <van-tab title="推荐" name="recommend" />
      <van-tab title="餐饮" name="1" />
      <van-tab title="咖啡厅" name="2" />
      <van-tab title="购物" name="3" />
      <van-tab title="娱乐" name="4" />
      <van-tab title="其他" name="5" />
    </van-tabs>

    <!-- 服务列表 -->
    <div class="service-list">
      <div
        v-for="service in services"
        :key="service.id"
        class="service-card"
        @click="goToServiceDetail(service)"
      >
        <!-- 服务图片 -->
        <div class="service-image">
          <van-image
            width="100%"
            height="100"
            fit="cover"
            :src="service.imageUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
          />
          <van-tag class="service-type-tag" :type="getServiceTypeColor(service.serviceType)">
            {{ getServiceTypeText(service.serviceType) }}
          </van-tag>
        </div>

        <!-- 服务信息 -->
        <div class="service-info">
          <div class="service-name">{{ service.serviceName }}</div>
          <div class="service-desc">{{ service.description }}</div>

          <!-- 距离和营业时间 -->
          <div class="service-meta">
            <div class="meta-item">
              <van-icon name="location-o" />
              <span>{{ service.distance }}m</span>
            </div>
            <div class="meta-item">
              <van-icon name="clock-o" />
              <span>{{ service.businessHours }}</span>
            </div>
          </div>

          <!-- 推荐理由 -->
          <div class="recommend-reason" v-if="service.recommendReason">
            <van-tag type="success" plain size="mini">
              {{ service.recommendReason }}
            </van-tag>
          </div>

          <!-- 联系方式 -->
          <div class="service-contact">
            <span class="contact-phone">{{ service.phone }}</span>
            <van-button
              size="mini"
              type="primary"
              @click.stop="callService(service.phone)"
            >
              拨打电话
            </van-button>
          </div>
        </div>
      </div>

      <van-empty v-if="services.length === 0 && !loading" description="暂无服务推荐" />
    </div>

    <!-- 加载状态 -->
    <van-loading v-if="loading" class="loading-center" type="spinner">
      加载中...
    </van-loading>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRecommendedServices, getAllServices, getServicesByType } from '@/api/nearby'

const route = useRoute()
const router = useRouter()

const stationId = ref(parseInt(route.query.stationId) || 1)
const waitTime = ref(parseInt(route.query.waitTime) || 0)

// 服务类型
const activeType = ref('recommend')

// 服务列表
const services = ref([])
const loading = ref(false)

// 加载服务列表
const loadServices = async () => {
  loading.value = true

  try {
    let res

    if (activeType.value === 'recommend') {
      // 推荐服务（根据等待时间）
      res = await getRecommendedServices(stationId.value, waitTime.value)
    } else {
      // 按类型筛选
      res = await getServicesByType(stationId.value, parseInt(activeType.value))
    }

    if (res.code === 200) {
      services.value = res.data || []
    }
  } catch (error) {
    console.error('加载服务列表失败:', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 切换服务类型
const onTypeChange = () => {
  loadServices()
}

// 获取服务类型文字
const getServiceTypeText = (type) => {
  const map = {
    1: '餐饮',
    2: '咖啡厅',
    3: '购物',
    4: '娱乐',
    5: '其他'
  }
  return map[type] || '未知'
}

// 获取服务类型颜色
const getServiceTypeColor = (type) => {
  const map = {
    1: 'danger',
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'default'
  }
  return map[type] || 'default'
}

// 拨打电话
const callService = (phone) => {
  window.location.href = `tel:${phone}`
}

// 查看服务详情
const goToServiceDetail = (service) => {
  // 可以跳转到地图或详情页
  showToast(`正在打开 ${service.serviceName}`)
  // TODO: 集成地图导航
}

onMounted(() => {
  loadServices()
})
</script>

<style scoped>
.nearby-service {
  padding-bottom: 20px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.wait-time-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.banner-content {
  flex: 1;
}

.wait-text {
  font-size: 12px;
  opacity: 0.9;
}

.wait-value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 4px;
}

.banner-tip {
  font-size: 12px;
  opacity: 0.9;
}

.service-list {
  padding: 12px;
}

.service-card {
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.service-image {
  position: relative;
}

.service-type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.service-info {
  padding: 12px;
}

.service-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 6px;
}

.service-desc {
  font-size: 13px;
  color: #646566;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.service-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #969799;
}

.recommend-reason {
  margin-bottom: 8px;
}

.service-contact {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #ebedf0;
}

.contact-phone {
  font-size: 14px;
  color: #1989fa;
}

.loading-center {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px;
}
</style>
