<template>
  <div class="statistics-page">
    <h2>统计分析</h2>

    <!-- 时间范围选择器 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="时间范围">
          <el-radio-group v-model="timeRange" @change="handleTimeRangeChange">
            <el-radio-button label="today">今日</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="timeRange === 'custom'" label="">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleCustomDateChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Refresh" @click="refreshData">刷新数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#409EFF">
              <Document />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ orderStats.totalCount || 0 }}</div>
              <div class="stat-label">总订单数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#67C23A">
              <CircleCheck />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ orderStats.completedCount || 0 }}</div>
              <div class="stat-label">完成订单</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#E6A23C">
              <Clock />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ orderStats.inProgressCount || 0 }}</div>
              <div class="stat-label">进行中订单</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#F56C6C">
              <CircleClose />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ orderStats.cancelledCount || 0 }}</div>
              <div class="stat-label">取消订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 - 第一行 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 收入统计图表 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>收入统计</span>
              <el-select v-model="revenuePeriod" size="small" style="width: 100px; margin-left: 10px" @change="fetchRevenueData">
                <el-option label="按日" value="day" />
                <el-option label="按周" value="week" />
                <el-option label="按月" value="month" />
              </el-select>
            </div>
          </template>
          <div ref="revenueChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>

      <!-- 充电量统计图表 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>充电量趋势</span>
              <el-tag size="small" style="margin-left: 10px">{{ chargeTrendDays }}天</el-tag>
            </div>
          </template>
          <div ref="chargeTrendChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 - 第二行 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 用户增长图表 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
            </div>
          </template>
          <div ref="userGrowthChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>

      <!-- 故障分布饼图 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>故障类型分布</span>
            </div>
          </template>
          <div ref="faultPieChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计摘要 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>统计摘要</span>
            </div>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="总充电量">
              <span class="highlight-value">{{ summaryStats.totalChargeAmount?.toFixed(2) || 0 }} kWh</span>
            </el-descriptions-item>
            <el-descriptions-item label="总收入">
              <span class="highlight-value revenue-color">¥{{ summaryStats.totalRevenue?.toFixed(2) || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="平均订单金额">
              <span class="highlight-value">¥{{ summaryStats.avgOrderAmount?.toFixed(2) || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="总用户数">
              <span class="highlight-value">{{ summaryStats.totalUsers || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="活跃用户数">
              <span class="highlight-value">{{ summaryStats.activeUsers || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="订单完成率">
              <span class="highlight-value">{{ summaryStats.completionRate?.toFixed(1) || 0 }}%</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Document, CircleCheck, Clock, CircleClose, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getOrderStatistics, getRevenueStatistics, getChargeTrend } from '@/api/statistics'
import { getFaultStatistics } from '@/api/fault'
import { ElMessage } from 'element-plus'

// 时间范围
const timeRange = ref('today')
const dateRange = ref([])
const startDate = ref('')
const endDate = ref('')

// 订单统计数据
const orderStats = ref({
  totalCount: 0,
  completedCount: 0,
  inProgressCount: 0,
  cancelledCount: 0
})

// 收入统计
const revenuePeriod = ref('day')
const revenueData = ref({
  dates: [],
  revenues: []
})

// 充电量趋势
const chargeTrendDays = ref(7)
const chargeTrendData = ref({
  dates: [],
  amounts: []
})

// 用户增长数据（模拟数据）
const userGrowthData = ref({
  dates: [],
  counts: []
})

// 故障统计
const faultStatsData = ref([])

// 统计摘要
const summaryStats = ref({
  totalChargeAmount: 0,
  totalRevenue: 0,
  avgOrderAmount: 0,
  totalUsers: 0,
  activeUsers: 0,
  completionRate: 0
})

// 图表引用
const revenueChartRef = ref(null)
const chargeTrendChartRef = ref(null)
const userGrowthChartRef = ref(null)
const faultPieChartRef = ref(null)

// 图表实例
let revenueChart = null
let chargeTrendChart = null
let userGrowthChart = null
let faultPieChart = null

// 计算日期范围
const calculateDateRange = () => {
  const today = new Date()
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  if (timeRange.value === 'today') {
    startDate.value = formatDate(today)
    endDate.value = formatDate(today)
  } else if (timeRange.value === 'week') {
    const weekAgo = new Date(today)
    weekAgo.setDate(today.getDate() - 6)
    startDate.value = formatDate(weekAgo)
    endDate.value = formatDate(today)
  } else if (timeRange.value === 'month') {
    const monthAgo = new Date(today)
    monthAgo.setDate(today.getDate() - 29)
    startDate.value = formatDate(monthAgo)
    endDate.value = formatDate(today)
  } else if (timeRange.value === 'custom' && dateRange.value && dateRange.value.length === 2) {
    startDate.value = dateRange.value[0]
    endDate.value = dateRange.value[1]
  }
}

// 时间范围变化
const handleTimeRangeChange = () => {
  if (timeRange.value !== 'custom') {
    calculateDateRange()
    fetchAllData()
  }
}

// 自定义日期变化
const handleCustomDateChange = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    calculateDateRange()
    fetchAllData()
  }
}

// 刷新数据
const refreshData = () => {
  ElMessage.success('正在刷新数据...')
  fetchAllData()
}

// 获取订单统计
const fetchOrderStatistics = async () => {
  try {
    const res = await getOrderStatistics(startDate.value, endDate.value)
    if (res.data) {
      orderStats.value = {
        totalCount: res.data.totalCount || 0,
        completedCount: res.data.completedCount || 0,
        inProgressCount: res.data.inProgressCount || 0,
        cancelledCount: res.data.cancelledCount || 0
      }

      // 更新统计摘要
      summaryStats.value.totalChargeAmount = res.data.totalChargeAmount || 0
      summaryStats.value.totalRevenue = res.data.totalRevenue || 0
      summaryStats.value.avgOrderAmount = res.data.totalCount > 0
        ? res.data.totalRevenue / res.data.totalCount
        : 0
      summaryStats.value.completionRate = res.data.totalCount > 0
        ? (res.data.completedCount / res.data.totalCount) * 100
        : 0
    }
  } catch (error) {
    console.error('获取订单统计失败:', error)
  }
}

// 获取收入统计
const fetchRevenueData = async () => {
  try {
    const res = await getRevenueStatistics(revenuePeriod.value)
    if (res.data) {
      revenueData.value = {
        dates: res.data.dates || [],
        revenues: res.data.revenues || []
      }
      initRevenueChart()
    }
  } catch (error) {
    console.error('获取收入统计失败:', error)
  }
}

// 获取充电量趋势
const fetchChargeTrendData = async () => {
  try {
    const days = timeRange.value === 'today' ? 7 : timeRange.value === 'week' ? 7 : 30
    chargeTrendDays.value = days
    const res = await getChargeTrend(days)
    if (res.data) {
      chargeTrendData.value = {
        dates: res.data.dates || [],
        amounts: res.data.chargeAmounts || []
      }
      initChargeTrendChart()
    }
  } catch (error) {
    console.error('获取充电量趋势失败:', error)
  }
}

// 获取故障统计
const fetchFaultStatistics = async () => {
  try {
    const res = await getFaultStatistics()
    if (res.data) {
      // 将故障统计数据转换为饼图格式
      const faultTypeMap = {
        1: '通信故障',
        2: '硬件故障',
        3: '软件故障',
        4: '其他故障'
      }

      faultStatsData.value = Object.entries(res.data.faultTypeDistribution || {}).map(([type, count]) => ({
        name: faultTypeMap[type] || `类型${type}`,
        value: count
      }))

      initFaultPieChart()
    }
  } catch (error) {
    console.error('获取故障统计失败:', error)
    // 使用模拟数据
    faultStatsData.value = [
      { name: '通信故障', value: 15 },
      { name: '硬件故障', value: 28 },
      { name: '软件故障', value: 12 },
      { name: '其他故障', value: 8 }
    ]
    initFaultPieChart()
  }
}

// 生成用户增长模拟数据
const generateUserGrowthData = () => {
  const dates = []
  const counts = []
  const baseCount = 1000
  const today = new Date()

  for (let i = 29; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    dates.push(`${date.getMonth() + 1}/${date.getDate()}`)
    counts.push(baseCount + Math.floor(Math.random() * 100) + i * 5)
  }

  userGrowthData.value = { dates, counts }
  summaryStats.value.totalUsers = counts[counts.length - 1]
  summaryStats.value.activeUsers = Math.floor(counts[counts.length - 1] * 0.65)
  initUserGrowthChart()
}

// 初始化收入图表
const initRevenueChart = () => {
  if (!revenueChartRef.value) return

  if (revenueChart) {
    revenueChart.dispose()
  }

  revenueChart = echarts.init(revenueChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>收入: ¥{c}'
    },
    xAxis: {
      type: 'category',
      data: revenueData.value.dates,
      axisLabel: {
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '收入 (元)',
      axisLabel: {
        formatter: '¥{value}'
      }
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: revenueData.value.revenues,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ])
        },
        barWidth: '60%',
        label: {
          show: true,
          position: 'top',
          formatter: '¥{c}'
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    }
  }

  revenueChart.setOption(option)
}

// 初始化充电量趋势图
const initChargeTrendChart = () => {
  if (!chargeTrendChartRef.value) return

  if (chargeTrendChart) {
    chargeTrendChart.dispose()
  }

  chargeTrendChart = echarts.init(chargeTrendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>充电量: {c} kWh'
    },
    xAxis: {
      type: 'category',
      data: chargeTrendData.value.dates,
      axisLabel: {
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '充电量 (kWh)',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '充电量',
        type: 'line',
        data: chargeTrendData.value.amounts,
        smooth: true,
        itemStyle: {
          color: '#67C23A'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.5)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
            ]
          }
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    }
  }

  chargeTrendChart.setOption(option)
}

// 初始化用户增长图表
const initUserGrowthChart = () => {
  if (!userGrowthChartRef.value) return

  if (userGrowthChart) {
    userGrowthChart.dispose()
  }

  userGrowthChart = echarts.init(userGrowthChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>用户数: {c}'
    },
    xAxis: {
      type: 'category',
      data: userGrowthData.value.dates,
      axisLabel: {
        rotate: 30,
        interval: 4
      }
    },
    yAxis: {
      type: 'value',
      name: '用户数',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '用户数',
        type: 'line',
        data: userGrowthData.value.counts,
        smooth: true,
        itemStyle: {
          color: '#E6A23C'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(230, 162, 60, 0.5)' },
              { offset: 1, color: 'rgba(230, 162, 60, 0.1)' }
            ]
          }
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    }
  }

  userGrowthChart.setOption(option)
}

// 初始化故障饼图
const initFaultPieChart = () => {
  if (!faultPieChartRef.value) return

  if (faultPieChart) {
    faultPieChart.dispose()
  }

  faultPieChart = echarts.init(faultPieChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '故障类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: faultStatsData.value,
        color: ['#F56C6C', '#E6A23C', '#409EFF', '#909399']
      }
    ]
  }

  faultPieChart.setOption(option)
}

// 获取所有数据
const fetchAllData = () => {
  calculateDateRange()
  fetchOrderStatistics()
  fetchRevenueData()
  fetchChargeTrendData()
  fetchFaultStatistics()
  generateUserGrowthData()
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  revenueChart?.resize()
  chargeTrendChart?.resize()
  userGrowthChart?.resize()
  faultPieChart?.resize()
}

// 生命周期
onMounted(() => {
  fetchAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  revenueChart?.dispose()
  chargeTrendChart?.dispose()
  userGrowthChart?.dispose()
  faultPieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.statistics-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  transition: transform 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.highlight-value {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.revenue-color {
  color: #67C23A;
}

:deep(.el-descriptions__label) {
  font-weight: bold;
}

:deep(.el-card__header) {
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}
</style>
