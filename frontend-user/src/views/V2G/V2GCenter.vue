<template>
  <div class="v2g-center">
    <!-- 顶部统计 -->
    <van-cell-group class="stats-group">
      <van-cell title="累计放电量" :value="`${Number(statistics.totalDischargeAmount || 0).toFixed(2)} kWh`" />
      <van-cell title="累计收益" :value="`¥${Number(statistics.totalProfit || 0).toFixed(2)}`" />
      <van-cell title="平均电价" :value="`${Number(statistics.averagePrice || 0).toFixed(2)} 元/kWh`" />
    </van-cell-group>

    <!-- 电价预测 -->
    <van-cell-group class="price-forecast" title="电价预测">
      <div class="forecast-chart">
        <div v-for="item in priceForecast" :key="item.time" class="forecast-item">
          <div class="time">{{ item.time }}</div>
          <div class="price" :class="getPriceClass(item.priceLevel)">
            {{ item.price }}元/kWh
          </div>
          <div class="level">{{ getPriceLevelText(item.priceLevel) }}</div>
        </div>
      </div>
    </van-cell-group>

    <!-- 当前状态 -->
    <van-cell-group class="status-group" title="当前状态">
      <van-cell v-if="!currentDischarge">
        <template #title>
          <div class="status-idle">
            <van-icon name="info-o" />
            <span>未在放电</span>
          </div>
        </template>
      </van-cell>
      <template v-else>
        <van-cell title="已放电量" :value="`${Number(currentDischarge.energyAmount || 0).toFixed(2)} kWh`" />
        <van-cell title="当前收益" :value="`¥${Number(currentDischarge.profit || 0).toFixed(2)}`" />
        <van-cell title="电价" :value="`${Number(currentDischarge.electricityPrice || 0).toFixed(2)} 元/kWh`" />
        <van-cell title="开始时间" :value="currentDischarge.startTime" />
      </template>
    </van-cell-group>

    <!-- 放电策略设置 -->
    <van-cell-group class="strategy-group" title="放电策略">
      <van-form @submit="onStrategySubmit">
        <van-field
          v-model="strategyForm.minBatteryHealth"
          name="minBatteryHealth"
          label="最低电量(%)"
          type="number"
          placeholder="请输入最低电量百分比"
          :rules="[{ required: true, message: '请输入最低电量' }]"
        />
        <van-field
          v-model="strategyForm.maxDischargeAmount"
          name="maxDischargeAmount"
          label="最大放电量(kWh)"
          type="number"
          placeholder="请输入最大放电量"
          :rules="[{ required: true, message: '请输入最大放电量' }]"
        />
        <van-field name="minPrice" label="价格阈值">
          <template #input>
            <van-slider v-model="strategyForm.minPrice" :min="0.5" :max="2.0" :step="0.1" />
            <span class="slider-value">{{ strategyForm.minPrice }} 元/kWh</span>
          </template>
        </van-field>
        <div class="strategy-button-wrapper">
          <van-button round block type="primary" native-type="submit">
            保存策略
          </van-button>
        </div>
      </van-form>
    </van-cell-group>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <van-button
        v-if="!currentDischarge"
        type="success"
        size="large"
        round
        block
        @click="showStartDialog = true"
      >
        开始放电
      </van-button>
      <van-button
        v-else
        type="danger"
        size="large"
        round
        block
        @click="handleStopDischarge"
      >
        停止放电
      </van-button>
    </div>

    <!-- 放电记录 -->
    <van-cell-group class="history-group" title="放电记录">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadRecords"
      >
        <van-cell
          v-for="record in records"
          :key="record.id"
          :title="`${Number(record.energyAmount || 0).toFixed(2)} kWh`"
          :label="record.startTime"
          :value="`¥${Number(record.profit || 0).toFixed(2)}`"
          is-link
          @click="goToRecordDetail(record)"
        />
      </van-list>
    </van-cell-group>

    <!-- 开始放电对话框 -->
    <van-dialog
      v-model:show="showStartDialog"
      title="开始V2G放电"
      show-cancel-button
      @confirm="handleStartDischarge"
    >
      <van-form>
        <van-field
          v-model="startForm.pileId"
          name="pileId"
          label="充电桩ID"
          type="number"
          placeholder="请输入充电桩ID"
          required
        />
        <van-field
          v-model="startForm.currentSoc"
          name="currentSoc"
          label="当前电量(%)"
          type="number"
          placeholder="请输入当前电池电量"
          required
        />
        <van-field
          v-model="startForm.batteryHealthBefore"
          name="batteryHealthBefore"
          label="电池健康度(%)"
          type="number"
          placeholder="请输入电池健康度"
        />
        <van-field
          v-model="startForm.minPrice"
          name="minPrice"
          label="最低触发电价"
          type="number"
          placeholder="请输入最低触发电价(元/kWh)"
        />
      </van-form>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog, showLoadingToast, closeToast } from 'vant'
import {
  startDischarge,
  stopDischarge,
  getV2GRecords,
  getV2GStatistics,
  getPriceForecast,
  setDischargeStrategy
} from '@/api/v2g'

const router = useRouter()

// 统计数据
const statistics = ref({})

// 电价预测
const priceForecast = ref([])

// 当前放电信息
const currentDischarge = ref(null)

// 放电记录
const records = ref([])
const loading = ref(false)
const finished = ref(false)
const page = ref(1)

// 表单 - 匹配StartDischargeDTO
const showStartDialog = ref(false)
const startForm = ref({
  pileId: '',
  currentSoc: '',
  batteryHealthBefore: '',
  minPrice: ''
})

const strategyForm = ref({
  minBatteryHealth: 20,
  maxDischargeAmount: 30,
  minPrice: 1.0
})

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getV2GStatistics()
    if (res.code === 200) {
      statistics.value = res.data || { totalDischargeAmount: 0, totalProfit: 0, averagePrice: 0 }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    showToast('获取统计数据失败')
  }
}

// 获取电价预测
const fetchPriceForecast = async () => {
  try {
    const res = await getPriceForecast()
    if (res.code === 200) {
      priceForecast.value = res.data || []
    }
  } catch (error) {
    console.error('获取电价预测失败:', error)
    showToast('获取电价预测失败')
  }
}

// 加载放电记录（后端不支持分页，一次性加载全部）
const loadRecords = async () => {
  try {
    const res = await getV2GRecords()
    if (res.code === 200) {
      records.value = res.data || []
      loading.value = false
      finished.value = true
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    showToast('加载记录失败')
    loading.value = false
    finished.value = true
  }
}

// 开始放电 - 发送StartDischargeDTO
const handleStartDischarge = async () => {
  if (!startForm.value.pileId) {
    showToast('请填写充电桩ID')
    return
  }

  if (!startForm.value.currentSoc) {
    showToast('请填写当前电量')
    return
  }

  const toast = showLoadingToast({
    message: '正在启动放电...',
    forbidClick: true,
    duration: 0
  })

  try {
    const data = {
      pileId: Number(startForm.value.pileId),
      currentSoc: Number(startForm.value.currentSoc),
      batteryHealthBefore: startForm.value.batteryHealthBefore ? Number(startForm.value.batteryHealthBefore) : 95,
      minPrice: startForm.value.minPrice ? Number(startForm.value.minPrice) : 1.0
    }

    const res = await startDischarge(data)
    closeToast()

    if (res.code === 200) {
      showToast('放电已启动')
      showStartDialog.value = false
      currentDischarge.value = res.data || { pileId: startForm.value.pileId, energyAmount: 0, profit: 0 }
      startForm.value = { pileId: '', currentSoc: '', batteryHealthBefore: '', minPrice: '' }
      fetchStatistics()
    } else {
      showToast(res.message || '启动失败')
    }
  } catch (error) {
    closeToast()
    showToast('启动失败')
  }
}

// 停止放电
const handleStopDischarge = async () => {
  try {
    await showDialog({
      title: '确认',
      message: '确定要停止放电吗？'
    })
  } catch {
    return  // 用户点击取消
  }

  const toast = showLoadingToast({
    message: '正在停止放电...',
    forbidClick: true,
    duration: 0
  })

  try {
    // 估算放电量：基于已放电时间（模拟 7kW 放电功率）
    const startTime = currentDischarge.value.startTime
      ? new Date(currentDischarge.value.startTime).getTime()
      : Date.now() - 600000 // 默认10分钟前
    const elapsedHours = (Date.now() - startTime) / 3600000
    const estimatedEnergy = Math.max(0.1, Number((7 * elapsedHours).toFixed(2)))

    const res = await stopDischarge({
      recordId: currentDischarge.value.id,
      energyAmount: estimatedEnergy
    })
    closeToast()

    if (res.code === 200) {
      showToast('放电已停止')
      currentDischarge.value = null
      fetchStatistics()
      // 刷新记录
      records.value = []
      page.value = 1
      finished.value = false
      loadRecords()
    } else {
      showToast(res.message || '停止失败')
    }
  } catch (error) {
    closeToast()
    showToast('停止失败')
  }
}

// 保存策略
const onStrategySubmit = async () => {
  const toast = showLoadingToast({
    message: '保存中...',
    forbidClick: true,
    duration: 0
  })

  try {
    const res = await setDischargeStrategy(strategyForm.value)
    closeToast()

    if (res.code === 200) {
      showToast('策略已保存')
    } else {
      showToast(res.message || '保存失败')
    }
  } catch (error) {
    closeToast()
    showToast('保存失败')
  }
}

// 获取价格等级样式
const getPriceClass = (level) => {
  return {
    'price-low': level === '谷',
    'price-normal': level === '平',
    'price-high': level === '峰'
  }
}

// 获取价格等级文字
const getPriceLevelText = (level) => {
  const map = { '谷': '谷时', '平': '平时', '峰': '峰时' }
  return map[level] || ''
}

// 查看记录详情
const goToRecordDetail = (record) => {
  showToast('记录详情功能开发中，敬请期待')
}

onMounted(() => {
  fetchStatistics()
  fetchPriceForecast()
})
</script>

<style scoped>
.v2g-center {
  padding-bottom: 80px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.stats-group {
  margin-bottom: 12px;
}

.price-forecast {
  margin-bottom: 12px;
}

.forecast-chart {
  display: flex;
  overflow-x: auto;
  padding: 16px;
  gap: 12px;
}

.forecast-item {
  flex-shrink: 0;
  text-align: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  min-width: 100px;
}

.forecast-item .time {
  font-size: 12px;
  color: #969799;
  margin-bottom: 8px;
}

.forecast-item .price {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.price-low {
  color: #07c160;
}

.price-normal {
  color: #1989fa;
}

.price-high {
  color: #ee0a24;
}

.forecast-item .level {
  font-size: 12px;
  color: #969799;
}

.status-group {
  margin-bottom: 12px;
}

.status-idle {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #969799;
}

.strategy-group {
  margin-bottom: 12px;
}

.slider-value {
  margin-left: 12px;
  color: #323233;
}

.action-buttons {
  padding: 16px;
}

.history-group {
  margin-top: 12px;
}

.strategy-button-wrapper {
  margin: 16px;
}
</style>
