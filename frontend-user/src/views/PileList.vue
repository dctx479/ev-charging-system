<template>
  <div class="pile-list">
    <van-nav-bar
      title="充电桩列表"
      left-arrow
      @click-left="onClickLeft"
    />

    <div v-if="loading" class="loading-container">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <div v-else class="content">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="全部" name="all" />
        <van-tab title="可用" name="available" />
      </van-tabs>

      <div class="pile-cards">
        <pile-card
          v-for="pile in pileList"
          :key="pile.id"
          :pile="pile"
          @click="onPileClick(pile)"
          @start-charging="onStartCharging(pile)"
        />
      </div>

      <van-empty
        v-if="pileList.length === 0"
        description="暂无充电桩"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getPilesByStation, getAvailablePiles } from '@/api/pile'
import PileCard from '@/components/PileCard.vue'

const router = useRouter()
const route = useRoute()

const activeTab = ref('all')
const pileList = ref([])
const loading = ref(true)

const onClickLeft = () => {
  router.back()
}

const onTabChange = (name) => {
  loadPiles(name)
}

const loadPiles = async (type = 'all') => {
  loading.value = true
  try {
    const stationId = route.params.stationId
    const res = type === 'available'
      ? await getAvailablePiles(stationId)
      : await getPilesByStation(stationId)

    pileList.value = res.data || []
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const onPileClick = (pile) => {
  // 如果充电桩空闲，则跳转到下单页面
  if (pile.status === 1) {
    router.push({
      name: 'CreateOrder',
      query: {
        pileId: pile.id,
        stationId: route.params.stationId
      }
    })
  } else {
    // 非空闲状态，提示用户
    const statusText = {
      2: '充电中',
      3: '预约中',
      4: '故障',
      5: '离线'
    }[pile.status] || '不可用'
    showToast(`该充电桩${statusText}，无法使用`)
  }
}

const onStartCharging = (pile) => {
  // 直接跳转到下单页面
  router.push({
    name: 'CreateOrder',
    query: {
      pileId: pile.id,
      stationId: route.params.stationId
    }
  })
}

onMounted(() => {
  loadPiles()
})
</script>

<style scoped>
.pile-list {
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
  padding-bottom: 20px;
}

.pile-cards {
  padding: 12px;
}
</style>
