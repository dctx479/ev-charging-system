<template>
  <div class="exchange-records">
    <van-nav-bar
      title="兑换记录"
      left-arrow
      @click-left="onClickLeft"
    />

    <van-tabs v-model:active="activeTab" @change="handleTabChange">
      <van-tab title="全部" name="all" />
      <van-tab title="待发货" name="pending" />
      <van-tab title="已发货" name="shipped" />
      <van-tab title="已完成" name="completed" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div
          v-for="record in records"
          :key="record.id"
          class="record-item"
        >
          <div class="record-header">
            <span class="record-time">{{ formatTime(record.createTime) }}</span>
            <van-tag :type="getStatusType(record.status)">
              {{ getStatusText(record.status) }}
            </van-tag>
          </div>
          <div class="record-content">
            <van-image
              :src="record.productImage || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
              fit="cover"
              class="record-image"
            />
            <div class="record-info">
              <div class="record-name">{{ record.productName }}</div>
              <div class="record-credit">
                <van-icon name="medal-o" size="14" color="#667eea" />
                <span>{{ record.credits }} 积分</span>
              </div>
              <div class="record-quantity">数量：{{ record.quantity }}</div>
            </div>
          </div>
          <div v-if="record.trackingNumber" class="record-tracking">
            <van-icon name="logistics" />
            <span>物流单号：{{ record.trackingNumber }}</span>
          </div>
        </div>

        <van-empty
          v-if="!loading && records.length === 0"
          description="暂无兑换记录"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getExchangeRecords } from '@/api/credit'
import { formatTime } from '@/utils/dateFormat'

const router = useRouter()

const activeTab = ref('all')
const records = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const pageSize = 20

const onClickLeft = () => {
  router.back()
}

// 加载兑换记录
const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getExchangeRecords({
      page: page.value,
      size: pageSize,
      status: activeTab.value === 'all' ? null : activeTab.value
    })

    const newRecords = res.data.records || []

    if (page.value === 1) {
      records.value = newRecords
    } else {
      records.value.push(...newRecords)
    }

    loading.value = false

    if (newRecords.length < pageSize) {
      finished.value = true
    }
  } catch (error) {
    console.error('加载兑换记录失败:', error)
    loading.value = false
    finished.value = true
  }
}

// 下拉刷新
const onRefresh = async () => {
  page.value = 1
  finished.value = false
  try {
    await loadRecords()
  } finally {
    refreshing.value = false
  }
}

// 加载更多
const onLoad = () => {
  if (!finished.value) {
    page.value++
    loadRecords()
  }
}

// 切换标签
const handleTabChange = () => {
  page.value = 1
  finished.value = false
  records.value = []
  loadRecords()
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    pending: 'warning',
    shipped: 'primary',
    completed: 'success'
  }
  return typeMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    pending: '待发货',
    shipped: '已发货',
    completed: '已完成'
  }
  return textMap[status] || '未知'
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.exchange-records {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.record-item {
  background: white;
  margin: 12px;
  padding: 16px;
  border-radius: 12px;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-time {
  font-size: 13px;
  color: #969799;
}

.record-content {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.record-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  flex-shrink: 0;
}

.record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.record-name {
  font-size: 15px;
  font-weight: bold;
  color: #323233;
}

.record-credit {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-size: 14px;
}

.record-quantity {
  font-size: 13px;
  color: #969799;
}

.record-tracking {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1989fa;
  padding-top: 12px;
  border-top: 1px solid #ebedf0;
}
</style>
