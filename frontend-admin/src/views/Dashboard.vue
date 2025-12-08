<template>
  <div class="dashboard">
    <h2>数据概览</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#67C23A">
              <Lightning />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ dashboardStats.todayChargeAmount || 0 }}</div>
              <div class="stat-label">今日充电量 (kWh)</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#409EFF">
              <Money />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">¥{{ dashboardStats.todayRevenue || 0 }}</div>
              <div class="stat-label">今日收入 (元)</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#E6A23C">
              <Operation />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ dashboardStats.onlinePileCount || 0 }}</div>
              <div class="stat-label">在线充电桩</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#F56C6C">
              <Warning />
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ dashboardStats.faultPileCount || 0 }}</div>
              <div class="stat-label">故障充电桩</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 7天充电量趋势图 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>充电量趋势（最近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>

      <!-- 充电桩状态分布饼图 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>充电桩状态分布</span>
            </div>
          </template>
          <div ref="pieChartRef" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 站点收入排行榜 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>站点收入排行榜（Top 5）</span>
            </div>
          </template>
          <el-table :data="stationRanking" stripe>
            <el-table-column type="index" label="排名" width="80" />
            <el-table-column prop="stationName" label="站点名称" />
            <el-table-column prop="revenue" label="收入 (元)" sortable>
              <template #default="{ row }">
                <span class="revenue-value">¥{{ row.revenue?.toFixed(2) || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="chargeAmount" label="充电量 (kWh)" sortable>
              <template #default="{ row }">
                {{ row.chargeAmount?.toFixed(2) || 0 }}
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" sortable />
            <el-table-column label="运营状态">
              <template #default="{ row }">
                <el-tag :type="row.revenue > 5000 ? 'success' : row.revenue > 2000 ? '' : 'warning'">
                  {{ row.revenue > 5000 ? '优秀' : row.revenue > 2000 ? '良好' : '一般' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Lightning, Money, Operation, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats, getChargeTrend, getPileStatusDistribution, getStationRanking } from '@/api/statistics'
import { ElMessage } from 'element-plus'
import WebSocketClient from '@/utils/websocket'

// 数据定义
const dashboardStats = ref({
  todayChargeAmount: 0,
  todayRevenue: 0,
  onlinePileCount: 0,
  faultPileCount: 0
})

const trendData = ref({
  dates: [],
  chargeAmounts: []
})

const pileStatusData = ref([])
const stationRanking = ref([])

// 图表实例
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

// WebSocket 实例
let wsClient = null

// 获取仪表板数据
const fetchDashboardStats = async () => {
  try {
    const res = await getDashboardStats()
    dashboardStats.value = res.data || {
      todayChargeAmount: 0,
      todayRevenue: 0,
      onlinePileCount: 0,
      faultPileCount: 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取充电趋势数据
const fetchChargeTrend = async () => {
  try {
    const res = await getChargeTrend(7)
    trendData.value = res.data || { dates: [], chargeAmounts: [] }
    initTrendChart()
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}

// 获取充电桩状态分布
const fetchPileStatusDistribution = async () => {
  try {
    const res = await getPileStatusDistribution()
    pileStatusData.value = res.data || []
    initPieChart()
  } catch (error) {
    console.error('获取状态分布失败:', error)
  }
}

// 获取站点排行
const fetchStationRanking = async () => {
  try {
    const res = await getStationRanking(5)
    stationRanking.value = res.data || []
  } catch (error) {
    console.error('获取站点排行失败:', error)
  }
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>充电量: {c} kWh'
    },
    xAxis: {
      type: 'category',
      data: trendData.value.dates,
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
        data: trendData.value.chargeAmounts,
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
      bottom: '3%',
      containLabel: true
    }
  }

  trendChart.setOption(option)
}

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return

  if (pieChart) {
    pieChart.dispose()
  }

  pieChart = echarts.init(pieChartRef.value)

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
        name: '充电桩状态',
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
        data: pileStatusData.value,
        color: ['#67C23A', '#E6A23C', '#409EFF', '#F56C6C', '#909399']
      }
    ]
  }

  pieChart.setOption(option)
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

// 初始化 WebSocket 连接
const initWebSocket = () => {
  wsClient = new WebSocketClient('ws://localhost:8080/api/ws/pile-status')
  wsClient.connect()

  // 监听 WebSocket 消息
  wsClient.onMessage((message) => {
    console.log('收到 WebSocket 消息:', message)

    // 处理充电桩状态更新消息
    if (message.type === 'pile_status_update') {
      // 刷新统计数据（充电桩数量可能变化）
      fetchDashboardStats()
      // 刷新充电桩状态分布图
      fetchPileStatusDistribution()
    }

    // 处理故障告警消息
    if (message.type === 'fault_alert') {
      // 刷新统计数据（故障充电桩数量变化）
      fetchDashboardStats()
      // 刷新充电桩状态分布图
      fetchPileStatusDistribution()
    }
  })
}

// 生命周期
onMounted(() => {
  fetchDashboardStats()
  fetchChargeTrend()
  fetchPileStatusDistribution()
  fetchStationRanking()

  window.addEventListener('resize', handleResize)

  // 连接 WebSocket
  initWebSocket()
})

onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)

  // 断开 WebSocket 连接
  if (wsClient) {
    wsClient.close()
  }
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  transition: transform 0.3s;
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
  font-weight: bold;
  font-size: 16px;
}

.revenue-value {
  font-weight: bold;
  color: #67C23A;
  font-size: 15px;
}
</style>
