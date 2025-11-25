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

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <van-button
          type="primary"
          size="large"
          round
          @click="goToPileList"
        >
          查看充电桩
        </van-button>
      </div>
    </div>

    <van-empty v-else description="充电站不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getStationDetail } from '@/api/station'

const router = useRouter()
const route = useRoute()

const station = ref(null)
const loading = ref(true)

const onClickLeft = () => {
  router.back()
}

const goToPileList = () => {
  router.push(`/piles/${station.value.id}`)
}

const loadStationDetail = async () => {
  loading.value = true
  try {
    const res = await getStationDetail(route.params.id)
    station.value = res.data
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStationDetail()
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
