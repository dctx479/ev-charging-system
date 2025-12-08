<template>
  <div class="credit-center">
    <!-- 头部 -->
    <van-nav-bar title="积分中心" left-arrow @click-left="onBack" fixed />

    <div class="content">
      <!-- 积分余额卡片 -->
      <div class="balance-card">
        <div class="balance-title">我的碳积分</div>
        <div class="balance-amount">{{ statistics.currentBalance || 0 }}</div>
        <div class="balance-desc">每次充电都能获得碳积分哦~</div>

        <!-- 签到按钮 -->
        <van-button
          type="primary"
          round
          block
          class="checkin-btn"
          :disabled="statistics.hasCheckedInToday"
          @click="handleCheckIn"
        >
          {{ statistics.hasCheckedInToday ? '今日已签到' : '每日签到' }}
        </van-button>

        <div v-if="statistics.continuousCheckInDays > 0" class="checkin-info">
          已连续签到 <span class="highlight">{{ statistics.continuousCheckInDays }}</span> 天
        </div>
      </div>

      <!-- 积分统计 -->
      <div class="statistics-card">
        <div class="statistics-title">积分统计</div>
        <div class="statistics-grid">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalEarned || 0 }}</div>
            <div class="stat-label">累计获得</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalSpent || 0 }}</div>
            <div class="stat-label">累计消耗</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.monthEarned || 0 }}</div>
            <div class="stat-label">本月获得</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.monthSpent || 0 }}</div>
            <div class="stat-label">本月消耗</div>
          </div>
        </div>
      </div>

      <!-- 积分记录 -->
      <div class="records-section">
        <div class="section-header">
          <span class="section-title">积分记录</span>
          <van-button type="link" size="small" @click="goToHistory">
            查看全部 <van-icon name="arrow" />
          </van-button>
        </div>

        <div v-if="records.length > 0" class="records-list">
          <div
            v-for="record in records"
            :key="record.id"
            class="record-item"
          >
            <div class="record-left">
              <div class="record-type">{{ record.creditTypeText }}</div>
              <div class="record-time">{{ formatTime(record.createTime) }}</div>
            </div>
            <div class="record-right">
              <div
                class="record-amount"
                :class="record.creditChange > 0 ? 'positive' : 'negative'"
              >
                {{ record.creditChange > 0 ? '+' : '' }}{{ record.creditChange }}
              </div>
            </div>
          </div>
        </div>

        <van-empty v-else description="暂无积分记录" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showFailToast } from 'vant'
import { getCreditStatistics, getCreditHistory, dailyCheckIn } from '@/api/credit'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const statistics = ref({
  currentBalance: 0,
  totalEarned: 0,
  totalSpent: 0,
  monthEarned: 0,
  monthSpent: 0,
  continuousCheckInDays: 0,
  hasCheckedInToday: false
})

const records = ref([])

// 返回上一页
const onBack = () => {
  router.back()
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const userId = userStore.userId || 1 // 从store获取用户ID
    const res = await getCreditStatistics(userId)
    statistics.value = res.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载积分记录（最近10条）
const loadRecords = async () => {
  try {
    const userId = userStore.userId || 1
    const res = await getCreditHistory({ userId })
    records.value = res.data.slice(0, 10) // 只显示最近10条
  } catch (error) {
    console.error('加载积分记录失败:', error)
  }
}

// 每日签到
const handleCheckIn = async () => {
  try {
    const userId = userStore.userId || 1
    const res = await dailyCheckIn(userId)
    showSuccessToast(res.message || '签到成功')

    // 刷新数据
    await loadStatistics()
    await loadRecords()
  } catch (error) {
    showFailToast(error.message || '签到失败')
  }
}

// 跳转到积分记录页面
const goToHistory = () => {
  router.push('/credit/history')
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)

  if (date >= today) {
    return '今天 ' + date.toTimeString().slice(0, 5)
  } else if (date >= yesterday) {
    return '昨天 ' + date.toTimeString().slice(0, 5)
  } else {
    return date.toLocaleDateString() + ' ' + date.toTimeString().slice(0, 5)
  }
}

onMounted(() => {
  loadStatistics()
  loadRecords()
})
</script>

<style scoped>
.credit-center {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-top: 46px;
}

.content {
  padding: 16px;
}

/* 积分余额卡片 */
.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  color: white;
  text-align: center;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.balance-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.balance-amount {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 8px;
}

.balance-desc {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 20px;
}

.checkin-btn {
  margin-bottom: 12px;
  background-color: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.checkin-btn:disabled {
  background-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
}

.checkin-info {
  font-size: 13px;
  opacity: 0.9;
}

.checkin-info .highlight {
  color: #ffd700;
  font-weight: bold;
  font-size: 16px;
  margin: 0 4px;
}

/* 积分统计卡片 */
.statistics-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.statistics-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 16px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

/* 积分记录 */
.records-section {
  background: white;
  border-radius: 12px;
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.record-left {
  flex: 1;
}

.record-type {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.record-time {
  font-size: 12px;
  color: #999;
}

.record-amount {
  font-size: 18px;
  font-weight: bold;
}

.record-amount.positive {
  color: #07c160;
}

.record-amount.negative {
  color: #ee0a24;
}
</style>
