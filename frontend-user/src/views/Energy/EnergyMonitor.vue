<template>
  <div class="energy-monitor">
    <!-- 实时能源数据 -->
    <div class="realtime-section">
      <h3 class="section-title">实时能源数据</h3>
      <div class="energy-cards">
        <div class="energy-card solar">
          <van-icon name="sunny" size="32" />
          <div class="label">光伏发电</div>
          <div class="value">{{ realtimeData.solarPower || 0 }} kW</div>
        </div>
        <div class="energy-card storage">
          <van-icon name="fire" size="32" />
          <div class="label">储能</div>
          <div class="value">{{ realtimeData.storagePower || 0 }} kW</div>
          <div class="sub-value">SOC: {{ realtimeData.storageSoc || 0 }}%</div>
        </div>
        <div class="energy-card charging">
          <van-icon name="fire" size="32" />
          <div class="label">充电负载</div>
          <div class="value">{{ realtimeData.chargingLoad || 0 }} kW</div>
        </div>
        <div class="energy-card grid">
          <van-icon name="apps-o" size="32" />
          <div class="label">电网</div>
          <div class="value">{{ realtimeData.gridPower || 0 }} kW</div>
        </div>
      </div>
    </div>

    <!-- 能源流向图 -->
    <van-cell-group class="flow-section" title="能源流向">
      <div class="flow-diagram">
        <div class="flow-container">
          <!-- 光伏 -->
          <div class="flow-node solar-node">
            <van-icon name="sunny" size="24" />
            <div>光伏</div>
            <div class="node-value">{{ realtimeData.solarPower || 0 }}kW</div>
          </div>

          <!-- 储能 -->
          <div class="flow-node storage-node">
            <van-icon name="fire" size="24" />
            <div>储能</div>
            <div class="node-value">{{ realtimeData.storageSoc || 0 }}%</div>
          </div>

          <!-- 充电桩 -->
          <div class="flow-node charging-node">
            <van-icon name="fire" size="24" />
            <div>充电桩</div>
            <div class="node-value">{{ realtimeData.chargingLoad || 0 }}kW</div>
          </div>

          <!-- 电网 -->
          <div class="flow-node grid-node">
            <van-icon name="apps-o" size="24" />
            <div>电网</div>
            <div class="node-value">{{ realtimeData.gridPower || 0 }}kW</div>
          </div>

          <!-- 能量流动箭头 (这里简化显示，实际可用SVG或Canvas绘制) -->
          <div class="flow-arrows">
            <div class="arrow solar-to-charging" v-if="realtimeData.solarPower > 0">
              →
            </div>
            <div class="arrow storage-to-charging" v-if="realtimeData.storagePower > 0">
              →
            </div>
            <div class="arrow grid-to-charging" v-if="realtimeData.gridPower > 0">
              →
            </div>
          </div>
        </div>
        <div class="flow-legend">
          <div class="legend-item">
            <span class="legend-color solar"></span>
            <span>光伏发电</span>
          </div>
          <div class="legend-item">
            <span class="legend-color storage"></span>
            <span>储能放电</span>
          </div>
          <div class="legend-item">
            <span class="legend-color grid"></span>
            <span>电网供电</span>
          </div>
        </div>
      </div>
    </van-cell-group>

    <!-- 统计数据 -->
    <van-cell-group class="stats-section" title="今日统计">
      <van-cell title="光伏发电量" :value="`${statistics.todaySolarGeneration || 0} kWh`" />
      <van-cell title="储能充电量" :value="`${statistics.todayStorageCharge || 0} kWh`" />
      <van-cell title="储能放电量" :value="`${statistics.todayStorageDischarge || 0} kWh`" />
      <van-cell title="电网用电量" :value="`${statistics.todayGridConsumption || 0} kWh`" />
      <van-cell title="充电桩总充电量" :value="`${statistics.todayChargingTotal || 0} kWh`" />
      <van-cell
        title="可再生能源占比"
        :value="`${statistics.renewableRatio || 0}%`"
        :label="`光伏+储能供电占比`"
      />
    </van-cell-group>

    <!-- 历史数据图表 -->
    <van-cell-group class="chart-section" title="历史趋势">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="今日" name="today"></van-tab>
        <van-tab title="本周" name="week"></van-tab>
        <van-tab title="本月" name="month"></van-tab>
      </van-tabs>
      <div class="chart-container">
        <div ref="chartRef" style="width: 100%; height: 300px;"></div>
      </div>
    </van-cell-group>

    <!-- 环保贡献 -->
    <van-cell-group class="eco-section" title="环保贡献">
      <div class="eco-content">
        <div class="eco-item">
          <van-icon name="flower-o" size="32" color="#07c160" />
          <div class="eco-label">减少碳排放</div>
          <div class="eco-value">{{ statistics.carbonReduction || 0 }} kg</div>
        </div>
        <div class="eco-item">
          <van-icon name="down" size="32" color="#1989fa" />
          <div class="eco-label">节约用煤</div>
          <div class="eco-value">{{ statistics.coalSaving || 0 }} kg</div>
        </div>
        <div class="eco-item">
          <van-icon name="like-o" size="32" color="#ff976a" />
          <div class="eco-label">相当于植树</div>
          <div class="eco-value">{{ statistics.treeEquivalent || 0 }} 棵</div>
        </div>
      </div>
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRealtimeEnergy, getEnergyHistory, getEnergyStatistics } from '@/api/energy'

// 实时数据
const realtimeData = ref({})

// 统计数据
const statistics = ref({})

// 图表相关
const activeTab = ref('today')
const chartRef = ref(null)
let chartInstance = null

// 定时器
let timer = null

// 获取实时数据
const fetchRealtimeData = async () => {
  try {
    const res = await getRealtimeEnergy({ stationId: 1 })
    if (res.code === 200) {
      realtimeData.value = res.data || {}
    }
  } catch (error) {
    console.error('获取实时数据失败:', error)
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const today = new Date()
    const dateStr = today.toISOString().split('T')[0]
    const res = await getEnergyStatistics({
      stationId: 1,
      date: dateStr
    })
    if (res.code === 200) {
      statistics.value = res.data || {}
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取历史数据
const fetchHistoryData = async () => {
  try {
    const now = new Date()
    let startTime, endTime

    if (activeTab.value === 'today') {
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      endTime = now
    } else if (activeTab.value === 'week') {
      startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      endTime = now
    } else {
      startTime = new Date(now.getFullYear(), now.getMonth(), 1)
      endTime = now
    }

    const formatDateTime = (date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    }

    const res = await getEnergyHistory({
      stationId: 1,
      startTime: formatDateTime(startTime),
      endTime: formatDateTime(endTime)
    })
    if (res.code === 200) {
      updateChart(res.data || [])
    }
  } catch (error) {
    console.error('获取历史数据失败:', error)
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['光伏', '储能', '电网', '充电负载']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: []
    },
    yAxis: {
      type: 'value',
      name: '功率(kW)'
    },
    series: [
      {
        name: '光伏',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#ff976a' }
      },
      {
        name: '储能',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#07c160' }
      },
      {
        name: '电网',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#1989fa' }
      },
      {
        name: '充电负载',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#ee0a24' }
      }
    ]
  }

  chartInstance.setOption(option)
}

// 更新图表数据
const updateChart = (data) => {
  if (!chartInstance) return

  const times = data.map(item => item.time)
  const solarData = data.map(item => item.solarPower)
  const storageData = data.map(item => item.storagePower)
  const gridData = data.map(item => item.gridPower)
  const chargingData = data.map(item => item.chargingLoad)

  chartInstance.setOption({
    xAxis: {
      data: times
    },
    series: [
      { data: solarData },
      { data: storageData },
      { data: gridData },
      { data: chargingData }
    ]
  })
}

// 切换Tab
const onTabChange = () => {
  fetchStatistics()
  fetchHistoryData()
}

// 启动定时刷新
const startAutoRefresh = () => {
  timer = setInterval(() => {
    fetchRealtimeData()
  }, 5000) // 每5秒刷新一次
}

onMounted(async () => {
  fetchRealtimeData()
  fetchStatistics()
  await nextTick()
  initChart()
  fetchHistoryData()
  startAutoRefresh()
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.energy-monitor {
  padding-bottom: 20px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  padding: 16px;
  margin: 0;
  background: #fff;
}

.realtime-section {
  margin-bottom: 12px;
  background: #fff;
}

.energy-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}

.energy-card {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.energy-card.solar {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
}

.energy-card.storage {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
}

.energy-card.charging {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #fff;
}

.energy-card.grid {
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
  color: #333;
}

.energy-card .label {
  margin: 8px 0;
  font-size: 14px;
}

.energy-card .value {
  font-size: 20px;
  font-weight: bold;
}

.energy-card .sub-value {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.8;
}

.flow-section {
  margin-bottom: 12px;
}

.flow-diagram {
  padding: 16px;
}

.flow-container {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 16px;
}

.flow-node {
  text-align: center;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  font-size: 12px;
}

.node-value {
  margin-top: 4px;
  font-weight: bold;
  color: #323233;
}

.flow-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.legend-color {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.solar {
  background: #ff976a;
}

.legend-color.storage {
  background: #07c160;
}

.legend-color.grid {
  background: #1989fa;
}

.stats-section {
  margin-bottom: 12px;
}

.chart-section {
  margin-bottom: 12px;
}

.chart-container {
  padding: 16px;
  background: #fff;
}

.eco-section {
  margin-bottom: 12px;
}

.eco-content {
  display: flex;
  justify-content: space-around;
  padding: 24px 16px;
  background: #fff;
}

.eco-item {
  text-align: center;
}

.eco-label {
  margin: 8px 0;
  font-size: 12px;
  color: #969799;
}

.eco-value {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}
</style>
