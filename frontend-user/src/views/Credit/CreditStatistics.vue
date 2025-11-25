<template>
  <div class="credit-statistics">
    <van-nav-bar
      title="碳积分统计"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 积分总览 -->
      <div class="overview-card">
        <div class="overview-header">
          <van-icon name="medal" color="#ffd21e" size="32" />
          <span class="title">我的积分</span>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <div class="value">{{ statistics.totalEarned || 0 }}</div>
            <div class="label">总积分</div>
          </div>
          <div class="stat-item">
            <div class="value">{{ statistics.currentBalance || 0 }}</div>
            <div class="label">可用积分</div>
          </div>
          <div class="stat-item">
            <div class="value">{{ statistics.totalSpent || 0 }}</div>
            <div class="label">已使用</div>
          </div>
          <div class="stat-item">
            <div class="value">{{ statistics.pendingCredits || 0 }}</div>
            <div class="label">待到账</div>
          </div>
        </div>
      </div>

      <!-- 积分来源统计 -->
      <van-cell-group title="积分来源" inset style="margin-top: 16px">
        <div class="chart-container">
          <canvas ref="sourceChartRef" class="chart"></canvas>
        </div>

        <van-cell
          v-for="source in sourceBreakdown"
          :key="source.type"
          :title="source.label"
          :value="`${source.credits}积分`"
        >
          <template #icon>
            <van-icon :name="source.icon" :color="source.color" size="20" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 积分使用统计 -->
      <van-cell-group title="积分使用" inset style="margin-top: 16px">
        <div class="chart-container">
          <canvas ref="usageChartRef" class="chart"></canvas>
        </div>

        <van-cell
          v-for="usage in usageBreakdown"
          :key="usage.type"
          :title="usage.label"
          :value="`${usage.credits}积分`"
        >
          <template #icon>
            <van-icon :name="usage.icon" :color="usage.color" size="20" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 月度趋势 -->
      <van-cell-group title="月度趋势" inset style="margin-top: 16px">
        <div class="chart-container" style="height: 200px">
          <canvas ref="trendChartRef" class="chart"></canvas>
        </div>
      </van-cell-group>

      <!-- 碳减排统计 -->
      <van-cell-group title="碳减排贡献" inset style="margin-top: 16px">
        <van-cell>
          <div class="carbon-stat">
            <van-icon name="fire" color="#52c41a" size="48" />
            <div class="carbon-info">
              <div class="carbon-value">{{ carbonReduction.toFixed(2) }} kg</div>
              <div class="carbon-label">累计减排CO₂</div>
            </div>
          </div>
        </van-cell>

        <van-cell title="等效植树" :value="`${treesEquivalent}棵`">
          <template #icon>
            <van-icon name="flower-o" color="#52c41a" size="20" />
          </template>
        </van-cell>

        <van-cell title="等效行驶里程" :value="`${mileageEquivalent} km`">
          <template #icon>
            <van-icon name="location-o" color="#1989fa" size="20" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 补偿记录 -->
      <van-cell-group title="碳积分补偿" inset style="margin-top: 16px">
        <van-cell
          title="补偿记录"
          is-link
          @click="goToCompensationRecords"
        >
          <template #value>
            <span class="compensation-value">{{ statistics.compensationCredits || 0 }}积分</span>
          </template>
        </van-cell>

        <van-cell title="补偿次数" :value="`${statistics.compensationCount || 0}次`" />
      </van-cell-group>

      <!-- 排行榜 -->
      <van-cell-group title="积分排行榜" inset style="margin-top: 16px">
        <van-cell title="我的排名" :value="`第${myRanking}名`">
          <template #icon>
            <van-icon name="award" color="#ffd21e" size="20" />
          </template>
        </van-cell>

        <van-cell
          v-for="(rank, index) in topRankings"
          :key="rank.userId"
          :title="`${index + 1}. ${rank.username}`"
          :value="`${rank.credits}积分`"
        >
          <template #icon>
            <van-icon
              :name="index < 3 ? 'medal' : 'user-o'"
              :color="getMedalColor(index)"
              size="20"
            />
          </template>
        </van-cell>
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCreditStatistics } from '@/api/credit'
import Chart from 'chart.js/auto'

const router = useRouter()

// 统计数据（字段名与后端 CreditStatisticsVO 对应）
const statistics = ref({
  totalEarned: 0,
  currentBalance: 0,
  totalSpent: 0,
  monthEarned: 0,
  monthSpent: 0,
  continuousCheckInDays: 0,
  hasCheckedInToday: false
})

// 来源统计
const sourceBreakdown = ref([
  { type: 'charging', label: '充电获得', credits: 0, icon: 'fire', color: '#1989fa' },
  { type: 'compensation', label: '碳积分补偿', credits: 0, icon: 'balance-o', color: '#52c41a' },
  { type: 'activity', label: '活动奖励', credits: 0, icon: 'gift', color: '#ff976a' },
  { type: 'referral', label: '推荐好友', credits: 0, icon: 'friends', color: '#ffd21e' }
])

// 使用统计
const usageBreakdown = ref([
  { type: 'exchange', label: '商品兑换', credits: 0, icon: 'gift-card', color: '#ee0a24' },
  { type: 'discount', label: '充电优惠', credits: 0, icon: 'coupon', color: '#ff976a' },
  { type: 'donation', label: '公益捐赠', credits: 0, icon: 'like', color: '#07c160' }
])

// 月度趋势数据
const monthlyTrend = ref([])

// 排行榜
const myRanking = ref(0)
const topRankings = ref([])

// 图表引用
const sourceChartRef = ref(null)
const usageChartRef = ref(null)
const trendChartRef = ref(null)

// 图表实例
let sourceChart = null
let usageChart = null
let trendChart = null

// 计算属性
const carbonReduction = computed(() => {
  // 假设每积分代表0.785kg CO2减排
  return (statistics.value.totalEarned || 0) * 0.0785
})

const treesEquivalent = computed(() => {
  // 假设1棵树年吸收18.3kg CO2
  return Math.floor(carbonReduction.value / 18.3)
})

const mileageEquivalent = computed(() => {
  // 假设燃油车每km排放0.12kg CO2
  return Math.floor(carbonReduction.value / 0.12)
})

// 返回上一页
const onBack = () => {
  router.back()
}

// 获取奖牌颜色
const getMedalColor = (index) => {
  const colors = ['#ffd700', '#c0c0c0', '#cd7f32', '#969799']
  return colors[index] || colors[3]
}

// 跳转到补偿记录
const goToCompensationRecords = () => {
  router.push('/credit/exchange/records')
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getCreditStatistics()
    if (res.code === 200) {
      statistics.value = res.data

      // 更新来源统计
      if (res.data.sourceBreakdown) {
        sourceBreakdown.value.forEach(item => {
          item.credits = res.data.sourceBreakdown[item.type] || 0
        })
      }

      // 更新使用统计
      if (res.data.usageBreakdown) {
        usageBreakdown.value.forEach(item => {
          item.credits = res.data.usageBreakdown[item.type] || 0
        })
      }

      // 更新月度趋势
      if (res.data.monthlyTrend) {
        monthlyTrend.value = res.data.monthlyTrend
      }

      // 更新排行榜
      if (res.data.myRanking) {
        myRanking.value = res.data.myRanking
      }
      if (res.data.topRankings) {
        topRankings.value = res.data.topRankings
      }

      // 渲染图表
      renderCharts()
    }
  } catch (error) {
    console.error('Load statistics error:', error)
    showToast('加载统计数据失败')
  }
}

const renderCharts = () => {
  renderSourceChart()
  renderUsageChart()
  renderTrendChart()
}

// 渲染来源饼图
const renderSourceChart = () => {
  if (!sourceChartRef.value) return

  if (sourceChart) {
    sourceChart.destroy()
  }

  const ctx = sourceChartRef.value.getContext('2d')
  sourceChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: sourceBreakdown.value.map(item => item.label),
      datasets: [{
        data: sourceBreakdown.value.map(item => item.credits),
        backgroundColor: sourceBreakdown.value.map(item => item.color)
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    }
  })
}

// 渲染使用饼图
const renderUsageChart = () => {
  if (!usageChartRef.value) return

  if (usageChart) {
    usageChart.destroy()
  }

  const ctx = usageChartRef.value.getContext('2d')
  usageChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: usageBreakdown.value.map(item => item.label),
      datasets: [{
        data: usageBreakdown.value.map(item => item.credits),
        backgroundColor: usageBreakdown.value.map(item => item.color)
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    }
  })
}

// 渲染趋势折线图
const renderTrendChart = () => {
  if (!trendChartRef.value || monthlyTrend.value.length === 0) return

  if (trendChart) {
    trendChart.destroy()
  }

  const ctx = trendChartRef.value.getContext('2d')
  trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: monthlyTrend.value.map(item => item.month),
      datasets: [{
        label: '获得积分',
        data: monthlyTrend.value.map(item => item.earned),
        borderColor: '#1989fa',
        backgroundColor: 'rgba(25, 137, 250, 0.1)',
        tension: 0.4
      }, {
        label: '使用积分',
        data: monthlyTrend.value.map(item => item.used),
        borderColor: '#ee0a24',
        backgroundColor: 'rgba(238, 10, 36, 0.1)',
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  })
}

// 生命周期
onMounted(() => {
  loadStatistics()
})

onBeforeUnmount(() => {
  if (sourceChart) { sourceChart.destroy(); sourceChart = null }
  if (usageChart) { usageChart.destroy(); usageChart = null }
  if (trendChart) { trendChart.destroy(); trendChart = null }
})
</script>

<style scoped lang="scss">
.credit-statistics {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 20px;

  .content {
    padding: 16px;
  }

  .overview-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 24px;
    color: #fff;

    .overview-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 24px;

      .title {
        font-size: 18px;
        font-weight: 600;
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 20px;

      .stat-item {
        text-align: center;

        .value {
          font-size: 28px;
          font-weight: 700;
          margin-bottom: 4px;
        }

        .label {
          font-size: 14px;
          opacity: 0.9;
        }
      }
    }
  }

  .chart-container {
    padding: 16px;
    height: 250px;

    .chart {
      width: 100%;
      height: 100%;
    }
  }

  .carbon-stat {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 0;

    .carbon-info {
      flex: 1;

      .carbon-value {
        font-size: 24px;
        font-weight: 600;
        color: #52c41a;
        margin-bottom: 4px;
      }

      .carbon-label {
        font-size: 14px;
        color: #646566;
      }
    }
  }

  .compensation-value {
    color: #52c41a;
    font-weight: 600;
  }

  :deep(.van-cell__value) {
    font-weight: 500;
  }

  :deep(.van-icon) {
    margin-right: 8px;
  }
}
</style>
