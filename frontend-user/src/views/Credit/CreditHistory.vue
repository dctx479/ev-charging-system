<template>
  <div class="credit-history">
    <!-- 头部 -->
    <van-nav-bar title="积分记录" left-arrow @click-left="onBack" fixed />

    <div class="content">
      <!-- 类型筛选 -->
      <div class="filter-bar">
        <van-button
          v-for="type in creditTypes"
          :key="type.value"
          :type="activeType === type.value ? 'primary' : 'default'"
          size="small"
          round
          @click="handleTypeChange(type.value)"
        >
          {{ type.label }}
        </van-button>
      </div>

      <!-- 积分记录列表 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-if="records.length > 0" class="records-list">
            <div
              v-for="record in records"
              :key="record.id"
              class="record-item"
            >
              <div class="record-header">
                <div class="record-type">
                  <van-tag :type="getTypeTag(record.creditType)">
                    {{ record.creditTypeText }}
                  </van-tag>
                </div>
                <div
                  class="record-amount"
                  :class="record.creditChange > 0 ? 'positive' : 'negative'"
                >
                  {{ record.creditChange > 0 ? '+' : '' }}{{ record.creditChange }}
                </div>
              </div>

              <div class="record-desc">{{ record.description }}</div>

              <div class="record-footer">
                <div class="record-time">{{ record.createTimeFormatted }}</div>
                <div class="record-balance">余额: {{ record.balanceAfter }}</div>
              </div>
            </div>
          </div>

          <van-empty v-else description="暂无积分记录" />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCreditHistory } from '@/api/credit'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 积分类型
const creditTypes = [
  { label: '全部', value: null },
  { label: '充电获得', value: 1 },
  { label: '每日签到', value: 2 },
  { label: '兑换消耗', value: 3 },
  { label: '活动奖励', value: 4 }
]

const activeType = ref(null)
const records = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

// 返回上一页
const onBack = () => {
  router.back()
}

// 加载积分记录
const loadRecords = async () => {
  try {
    const userId = userStore.userId || 1
    const params = {
      userId,
      creditType: activeType.value
    }

    const res = await getCreditHistory(params)
    records.value = res.data
    loading.value = false
    finished.value = true
  } catch (error) {
    console.error('加载积分记录失败:', error)
    loading.value = false
    showToast('加载失败')
  }
}

// 下拉刷新
const onRefresh = async () => {
  finished.value = false
  loading.value = true
  await loadRecords()
  refreshing.value = false
  showToast('刷新成功')
}

// 加载更多
const onLoad = async () => {
  await loadRecords()
}

// 切换类型筛选
const handleTypeChange = (type) => {
  activeType.value = type
  records.value = []
  finished.value = false
  loading.value = true
  loadRecords()
}

// 获取类型标签颜色
const getTypeTag = (creditType) => {
  switch (creditType) {
    case 1: // 充电获得
      return 'success'
    case 2: // 签到
      return 'primary'
    case 3: // 兑换消耗
      return 'warning'
    case 4: // 活动奖励
      return 'danger'
    default:
      return 'default'
  }
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.credit-history {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-top: 46px;
}

.content {
  padding: 0;
}

/* 筛选栏 */
.filter-bar {
  background: white;
  padding: 12px 16px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
}

.filter-bar::-webkit-scrollbar {
  display: none;
}

/* 积分记录列表 */
.records-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.record-amount {
  font-size: 20px;
  font-weight: bold;
}

.record-amount.positive {
  color: #07c160;
}

.record-amount.negative {
  color: #ee0a24;
}

.record-desc {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.5;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.record-balance {
  color: #667eea;
  font-weight: 500;
}
</style>
