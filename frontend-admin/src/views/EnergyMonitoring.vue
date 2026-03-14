<template>
  <div class="energy-monitoring">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :span="6">
        <el-card class="stat-card solar">
          <el-statistic title="今日光伏发电" :value="statistics.todaySolarGeneration || 0" suffix="kWh">
            <template #prefix>
              <el-icon><Sunny /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card storage">
          <el-statistic title="储能系统SOC" :value="statistics.storageSoc || 0" suffix="%">
            <template #prefix>
              <el-icon><Connection /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card charging">
          <el-statistic title="充电桩总负载" :value="statistics.totalChargingLoad || 0" suffix="kW">
            <template #prefix>
              <el-icon><Lightning /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card renewable">
          <el-statistic title="可再生能源占比" :value="statistics.renewableRatio || 0" suffix="%">
            <template #prefix>
              <el-icon><Opportunity /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 能源流向图 -->
    <el-card class="flow-card">
      <template #header>
        <span>实时能源流向</span>
      </template>
      <div ref="flowChartRef" style="height: 400px;"></div>
    </el-card>

    <!-- 能源趋势图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>能源生产趋势</span>
              <el-radio-group v-model="productionPeriod" size="small" @change="fetchProductionData">
                <el-radio-button label="today">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="productionChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>能源消耗趋势</span>
              <el-radio-group v-model="consumptionPeriod" size="small" @change="fetchConsumptionData">
                <el-radio-button label="today">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="consumptionChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 站点能源详情 -->
    <el-card class="table-card">
      <template #header>
        <span>站点能源详情</span>
      </template>

      <el-table :data="stationData" border stripe v-loading="loading">
        <el-table-column prop="stationId" label="站点ID" width="100" />
        <el-table-column prop="stationName" label="站点名称" width="200" />
        <el-table-column label="光伏发电(kWh)" width="150">
          <template #default="{ row }">
            <span style="color: #ff9800;">{{ row.solarGeneration || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="储能SOC(%)" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.storageSoc || 0"
              :color="getSOCColor(row.storageSoc)"
            />
          </template>
        </el-table-column>
        <el-table-column label="充电负载(kW)" width="150">
          <template #default="{ row }">
            <span style="color: #2196f3;">{{ row.chargingLoad || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="电网功率(kW)" width="150">
          <template #default="{ row }">
            <span :style="{ color: row.gridPower > 0 ? '#f44336' : '#4caf50' }">
              {{ row.gridPower || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="可再生能源占比" width="150">
          <template #default="{ row }">
            <el-tag :type="getRenewableType(row.renewableRatio)">
              {{ row.renewableRatio || 0 }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewStation(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getEnergyData, getEnergyStatistics } from '@/api/energy'

// 统计数据
const statistics = ref({})

// 图表实例
const flowChartRef = ref(null)
const productionChartRef = ref(null)
const consumptionChartRef = ref(null)
let flowChart = null
let productionChart = null
let consumptionChart = null

// 周期选择
const productionPeriod = ref('today')
const consumptionPeriod = ref('today')

// 站点数据
const stationData = ref([])
const loading = ref(false)

// 定时器
let timer = null
let resizeTimeout = null

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getEnergyStatistics()
    if (res.code === 200) {
      statistics.value = res.data || {}
    }
  } catch (error) {
    if (import.meta.env.DEV) {
      console.error('获取统计数据失败:', error)
    }
    ElMessage.error('获取统计数据失败，请重试')
  }
}

// 初始化能源流向图
const initFlowChart = () => {
  if (!flowChartRef.value) return

  // 如果已有实例则先销毁
  if (flowChart) {
    flowChart.dispose()
  }

  flowChart = echarts.init(flowChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} kW'
    },
    series: [
      {
        type: 'sankey',
        layout: 'none',
        emphasis: {
          focus: 'adjacency'
        },
        data: [
          { name: '光伏发电' },
          { name: '储能系统' },
          { name: '电网供电' },
          { name: '充电桩' },
          { name: '建筑用电' }
        ],
        links: [
          { source: '光伏发电', target: '充电桩', value: 50 },
          { source: '光伏发电', target: '储能系统', value: 30 },
          { source: '储能系统', target: '充电桩', value: 20 },
          { source: '电网供电', target: '充电桩', value: 40 },
          { source: '电网供电', target: '建筑用电', value: 15 }
        ],
        itemStyle: {
          color: '#1890ff',
          borderColor: '#1890ff'
        },
        lineStyle: {
          color: 'gradient',
          curveness: 0.5
        }
      }
    ]
  }

  flowChart.setOption(option)
}

// 初始化生产趋势图
const initProductionChart = () => {
  if (!productionChartRef.value) return

  // 如果已有实例则先销毁
  if (productionChart) {
    productionChart.dispose()
  }

  productionChart = echarts.init(productionChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['光伏发电', '储能放电']
    },
    xAxis: {
      type: 'category',
      data: []
    },
    yAxis: {
      type: 'value',
      name: '功率(kW)'
    },
    series: [
      {
        name: '光伏发电',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#ff9800' }
      },
      {
        name: '储能放电',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#4caf50' }
      }
    ]
  }

  productionChart.setOption(option)
}

// 初始化消耗趋势图
const initConsumptionChart = () => {
  if (!consumptionChartRef.value) return

  // 如果已有实例则先销毁
  if (consumptionChart) {
    consumptionChart.dispose()
  }

  consumptionChart = echarts.init(consumptionChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['充电负载', '电网用电']
    },
    xAxis: {
      type: 'category',
      data: []
    },
    yAxis: {
      type: 'value',
      name: '功率(kW)'
    },
    series: [
      {
        name: '充电负载',
        type: 'line',
        data: [],
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#2196f3' }
      },
      {
        name: '电网用电',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#f44336' }
      }
    ]
  }

  consumptionChart.setOption(option)
}

// 获取生产数据
const fetchProductionData = async () => {
  try {
    const res = await getEnergyData({
      period: productionPeriod.value
    })

    if (res.code === 200 && productionChart) {
      const data = res.data || []
      productionChart.setOption({
        xAxis: { data: data.map(item => item.time) },
        series: [
          { data: data.map(item => item.solar || 0) },
          { data: data.map(item => item.storage || 0) }
        ]
      })
    }
  } catch (error) {
    console.error('获取生产数据失败:', error)
  }
}

// 获取消耗数据
const fetchConsumptionData = async () => {
  try {
    const res = await getEnergyData({
      period: consumptionPeriod.value
    })

    if (res.code === 200 && consumptionChart) {
      const data = res.data || []
      consumptionChart.setOption({
        xAxis: { data: data.map(item => item.time) },
        series: [
          { data: data.map(item => item.charging || 0) },
          { data: data.map(item => item.grid || 0) }
        ]
      })
    }
  } catch (error) {
    console.error('获取消耗数据失败:', error)
  }
}

// 获取站点数据
const fetchStationData = async () => {
  loading.value = true

  try {
    const res = await getEnergyData({ type: 'stations' })

    if (res.code === 200) {
      stationData.value = res.data || []
    }
  } catch (error) {
    console.error('获取站点数据失败:', error)
    ElMessage.error('获取站点数据失败')
  } finally {
    loading.value = false
  }
}

// 查看站点详情
const handleViewStation = (row) => {
  ElMessage.info(`查看站点 ${row.stationName} 的详情`)
}

// 获取SOC颜色
const getSOCColor = (soc) => {
  if (soc >= 80) return '#67c23a'
  if (soc >= 50) return '#e6a23c'
  return '#f56c6c'
}

// 获取可再生能源占比类型
const getRenewableType = (ratio) => {
  if (ratio >= 70) return 'success'
  if (ratio >= 40) return 'warning'
  return 'danger'
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  // 清除之前的超时
  if (resizeTimeout) {
    clearTimeout(resizeTimeout)
  }

  // 防抖处理
  resizeTimeout = setTimeout(() => {
    resizeTimeout = null
    if (flowChart && !flowChart.isDisposed()) {
      flowChart.resize()
    }
    if (productionChart && !productionChart.isDisposed()) {
      productionChart.resize()
    }
    if (consumptionChart && !consumptionChart.isDisposed()) {
      consumptionChart.resize()
    }
  }, 300)
}

// 启动自动刷新
const startAutoRefresh = () => {
  timer = setInterval(() => {
    fetchStatistics()
    fetchStationData()
  }, 10000) // 每10秒刷新一次
}

onMounted(async () => {
  await nextTick()
  initFlowChart()
  initProductionChart()
  initConsumptionChart()
  fetchStatistics()
  fetchProductionData()
  fetchConsumptionData()
  fetchStationData()
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 清除定时器
  if (timer) {
    clearInterval(timer)
    timer = null
  }

  // 清除 resize 防抖超时
  if (resizeTimeout) {
    clearTimeout(resizeTimeout)
    resizeTimeout = null
  }

  // 移除resize事件监听器
  window.removeEventListener('resize', handleResize)

  // 销毁 ECharts 实例
  if (flowChart && !flowChart.isDisposed()) {
    flowChart.dispose()
    flowChart = null
  }
  if (productionChart && !productionChart.isDisposed()) {
    productionChart.dispose()
    productionChart = null
  }
  if (consumptionChart && !consumptionChart.isDisposed()) {
    consumptionChart.dispose()
    consumptionChart = null
  }
})
</script>

<style scoped>
.energy-monitoring {
  padding: 20px;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  border-left: 4px solid;
}

.stat-card.solar {
  border-left-color: #ff9800;
}

.stat-card.storage {
  border-left-color: #4caf50;
}

.stat-card.charging {
  border-left-color: #2196f3;
}

.stat-card.renewable {
  border-left-color: #9c27b0;
}

.flow-card {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  margin-bottom: 20px;
}
</style>
