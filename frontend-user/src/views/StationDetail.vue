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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getStationDetail } from '@/api/station'
import { getStationQueueInfo, joinQueue } from '@/api/queue'

const router = useRouter()
const route = useRoute()

const station = ref(null)
const loading = ref(true)
const queueInfo = ref(null)
const joiningQueue = ref(false)

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
