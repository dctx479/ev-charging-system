<template>
  <div class="charging-progress">
    <van-nav-bar
      title="充电中"
      left-arrow
      @click-left="onBack"
    />

    <!-- 无进行中订单提示 -->
    <van-empty
      v-if="!loading && !orderInfo.id"
      description="暂无进行中的充电订单"
    >
      <van-button type="primary" @click="router.push('/')">前往充电</van-button>
    </van-empty>

    <!-- 充电内容 -->
    <div v-else-if="orderInfo.id" class="content">
      <!-- 充电进度环形图 -->
      <div class="progress-section">
        <div class="progress-wrapper">
          <van-circle
            v-model:current-rate="progressRate"
            :rate="progressRate"
            :speed="100"
            :text="progressText"
            :stroke-width="120"
            size="220px"
            layer-color="#f0f0f0"
            color="#07c160"
          />
          <div class="progress-info">
            <div class="soc-info">
              <span class="label">当前电量</span>
              <span class="value">{{ currentSoc.toFixed(1) }}%</span>
            </div>
            <van-icon name="arrow" class="arrow-icon" />
            <div class="soc-info">
              <span class="label">目标电量</span>
              <span class="value">{{ orderInfo.targetSoc || 100 }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时数据卡片 -->
      <div class="data-cards">
        <div class="data-card">
          <div class="card-icon">
            <van-icon name="flash" color="#07c160" size="24" />
          </div>
          <div class="card-content">
            <div class="card-label">已充电量</div>
            <div class="card-value">{{ currentChargeAmount.toFixed(2) }} kWh</div>
          </div>
        </div>
        <div class="data-card">
          <div class="card-icon">
            <van-icon name="clock-o" color="#1989fa" size="24" />
          </div>
          <div class="card-content">
            <div class="card-label">充电时长</div>
            <div class="card-value">{{ formatDuration(chargeDuration) }}</div>
          </div>
        </div>
        <div class="data-card">
          <div class="card-icon">
            <van-icon name="clock-o" color="#ff976a" size="24" />
          </div>
          <div class="card-content">
            <div class="card-label">预计剩余</div>
            <div class="card-value">{{ estimatedRemaining }}</div>
          </div>
        </div>
        <div class="data-card">
          <div class="card-icon">
            <van-icon name="gold-coin-o" color="#ee0a24" size="24" />
          </div>
          <div class="card-content">
            <div class="card-label">当前费用</div>
            <div class="card-value">¥{{ currentTotalFee.toFixed(2) }}</div>
          </div>
        </div>
      </div>

      <!-- 费用详情 -->
      <van-cell-group title="费用详情" inset>
        <van-cell title="电费" :value="`¥${currentElectricityFee.toFixed(2)}`" />
        <van-cell title="服务费" :value="`¥${currentServiceFee.toFixed(2)}`" />
        <van-cell title="当前电价" :value="currentPriceText" />
        <van-cell title="总费用" :value="`¥${currentTotalFee.toFixed(2)}`" value-class="total-fee" />
      </van-cell-group>

      <!-- 充电桩信息 -->
      <van-cell-group title="充电桩信息" inset class="pile-info-group">
        <van-cell title="站点名称" :value="orderInfo.stationName" />
        <van-cell title="充电桩编号" :value="orderInfo.pileNo" />
        <van-cell title="充电功率" :value="`${orderInfo.pilePower}kW`" />
        <van-cell title="开始时间" :value="formatTime(orderInfo.startTime)" />
      </van-cell-group>

      <!-- 实时监控数据 -->
      <van-cell-group title="实时监控" inset class="monitoring-group">
        <van-cell title="充电电流" :value="`${chargeCurrent.toFixed(1)}A`" />
        <van-cell title="充电电压" :value="`${chargeVoltage.toFixed(1)}V`" />
        <van-cell title="电池温度" :value="`${batteryTemp.toFixed(1)}°C`" />
        <van-cell title="环境温度" :value="`${ambientTemp.toFixed(1)}°C`" />
      </van-cell-group>

      <!-- 结束充电按钮 -->
      <div class="button-group">
        <van-button
          type="danger"
          size="large"
          block
          :loading="ending"
          @click="handleEndCharging"
        >
          结束充电
        </van-button>
        <div class="button-tip">结束充电后将跳转至支付页面</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getOrderDetail, endCharging, getCurrentOrder } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const orderId = ref(route.query.orderId)
const orderInfo = ref({})
const currentSoc = ref(20) // 当前电量百分比
const currentChargeAmount = ref(0) // 当前充电量(kWh)
const currentElectricityFee = ref(0) // 电费
const currentServiceFee = ref(0) // 服务费
const chargeDuration = ref(0) // 充电时长(秒)
const batteryTemp = ref(25) // 电池温度
const ambientTemp = ref(20) // 环境温度
const chargeCurrent = ref(0) // 充电电流
const chargeVoltage = ref(400) // 充电电压
const ending = ref(false)

let refreshTimer = null
let simulationTimer = null

// 充电进度百分比(相对于目标SOC)
const progressRate = computed(() => {
  if (!orderInfo.value.startSoc || !orderInfo.value.targetSoc) return 0
  const total = orderInfo.value.targetSoc - orderInfo.value.startSoc
  const current = currentSoc.value - orderInfo.value.startSoc
  return Math.min((current / total) * 100, 100)
})

// 环形进度条显示文本
const progressText = computed(() => {
  return `${progressRate.value.toFixed(1)}%`
})

// 总费用
const currentTotalFee = computed(() => {
  return currentElectricityFee.value + currentServiceFee.value
})

// 当前电价文本
const currentPriceText = computed(() => {
  const price = getCurrentPrice()
  const type = getPriceType()
  return `¥${price}/kWh (${type})`
})

// 预计剩余时间
const estimatedRemaining = computed(() => {
  if (!orderInfo.value.pilePower || currentSoc.value >= orderInfo.value.targetSoc) {
    return '即将完成'
  }

  const remainingSoc = orderInfo.value.targetSoc - currentSoc.value
  const batteryCapacity = orderInfo.value.batteryCapacity || 60 // 假设60kWh
  const remainingKwh = batteryCapacity * remainingSoc / 100
  const chargePower = orderInfo.value.pilePower || 120

  // 充电效率约90%
  const remainingMinutes = (remainingKwh / chargePower) * 60 / 0.9

  return formatDuration(Math.round(remainingMinutes * 60))
})

onMounted(async () => {
  await loadOrderInfo()
  if (orderId.value) {
    startRefresh()
    startSimulation()
  }
  loading.value = false
})

onBeforeUnmount(() => {
  // 清理所有定时器
  stopRefresh()
  stopSimulation()
})

// 加载订单信息
const loadOrderInfo = async () => {
  try {
    let res
    if (orderId.value) {
      // 如果有orderId参数，获取指定订单
      res = await getOrderDetail(orderId.value)
    } else {
      // 否则获取当前进行中的订单
      res = await getCurrentOrder()
    }

    if (res.data) {
      orderInfo.value = res.data
      orderId.value = res.data.id
      currentSoc.value = res.data.startSoc || 20

      // 计算目标SOC（如果没有则根据充电模式计算）
      if (!orderInfo.value.targetSoc) {
        if (res.data.chargeMode === 1) {
          orderInfo.value.targetSoc = 100 // 充满模式
        } else {
          orderInfo.value.targetSoc = Math.min((res.data.startSoc || 20) + 50, 100)
        }
      }

      // 如果订单不是充电中状态，提示并返回
      if (res.data.orderStatus !== 2) { // 2表示充电中
        showToast('该订单不在充电中')
        setTimeout(() => {
          router.replace('/order/list')
        }, 1500)
        return
      }

      // 初始化充电时长(秒) - 从实际开始时间计算
      if (res.data.startTime) {
        let startTime;

        // 处理LocalDateTime数组格式 [year, month, day, hour, minute, second, nano]
        // 后端返回的是服务器本地时间(Asia/Shanghai UTC+8)
        if (Array.isArray(res.data.startTime)) {
          const [year, month, day, hour = 0, minute = 0, second = 0] = res.data.startTime;
          // 创建ISO格式字符串，指定为中国时区 (UTC+8)
          // 格式: "2025-12-15T10:30:00+08:00"
          const isoString = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}+08:00`;
          startTime = new Date(isoString);
        } else if (typeof res.data.startTime === 'string') {
          // 处理字符串格式 "2025-12-15 10:30:00"
          // 如果没有时区信息，假设是中国时区
          let timeStr = res.data.startTime;
          if (!timeStr.includes('T') && !timeStr.includes('+') && !timeStr.includes('Z')) {
            // 将 "2025-12-15 10:30:00" 转换为 "2025-12-15T10:30:00+08:00"
            timeStr = timeStr.replace(' ', 'T') + '+08:00';
          }
          startTime = new Date(timeStr);
        } else {
          // 其他情况直接转换
          startTime = new Date(res.data.startTime);
        }

        const now = new Date();
        const durationSeconds = Math.floor((now - startTime) / 1000);
        // 确保时长合理（不为负数，且不超过24小时）
        chargeDuration.value = Math.max(0, Math.min(durationSeconds, 86400));
      } else {
        chargeDuration.value = 0
      }

      // 初始化充电电流
      const powerKw = orderInfo.value.pilePower || 120
      chargeCurrent.value = (powerKw * 1000) / 400 // 假设400V电压
    }
  } catch (error) {
    if (import.meta.env.DEV) {
      console.error('加载订单失败:', error)
    }
    showToast('加载订单信息失败')
  }
}

// 定时刷新订单状态
const startRefresh = () => {
  stopRefresh()
  refreshTimer = setInterval(async () => {
    try {
      const res = await getOrderDetail(orderId.value)
      if (res.data) {
        // 更新订单信息，但不影响前端模拟的实时数据
        orderInfo.value = {
          ...orderInfo.value,
          ...res.data
        }

        // 如果订单状态改变，停止模拟
        if (res.data.orderStatus !== 2) {
          stopSimulation()
          stopRefresh()
        }
      }
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('刷新订单失败:', error)
      }
    }
  }, 5000) // 每5秒刷新
}

const stopRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 模拟充电数据变化
const startSimulation = () => {
  stopSimulation()
  simulationTimer = setInterval(() => {
    if (currentSoc.value >= (orderInfo.value.targetSoc || 100)) {
      // 达到目标电量，停止模拟
      stopSimulation()
      showToast('已达到目标电量')
      return
    }

    // 更新充电时长
    chargeDuration.value++

    // 模拟SOC增长(根据功率计算)
    const powerKw = orderInfo.value.pilePower || 120
    const batteryCapacity = orderInfo.value.batteryCapacity || 60
    // 每秒SOC增长量 = 功率(kW) / 电池容量(kWh) / 3600(秒) * 100(百分比) * 效率(90%)
    const socIncrement = (powerKw / batteryCapacity / 3600) * 100 * 0.9
    currentSoc.value = Math.min(currentSoc.value + socIncrement, orderInfo.value.targetSoc || 100)

    // 模拟充电量增加
    currentChargeAmount.value += powerKw / 3600 // kWh

    // 计算费用
    const price = getCurrentPrice()
    currentElectricityFee.value = currentChargeAmount.value * price
    currentServiceFee.value = currentChargeAmount.value * 0.5 // 服务费0.5元/kWh

    // 模拟电池温度变化(充电过程中会升温)
    batteryTemp.value = 25 + Math.min(chargeDuration.value / 600, 15) + Math.random() * 2 - 1

    // 模拟环境温度微小波动
    ambientTemp.value = 20 + Math.random() * 2 - 1

    // 模拟充电电流波动(±5%)
    const baseCurrent = (powerKw * 1000) / chargeVoltage.value
    chargeCurrent.value = baseCurrent * (0.95 + Math.random() * 0.1)

    // 模拟充电电压微小波动
    chargeVoltage.value = 400 + Math.random() * 10 - 5
  }, 1000) // 每秒更新一次
}

const stopSimulation = () => {
  if (simulationTimer) {
    clearInterval(simulationTimer)
    simulationTimer = null
  }
}

// 获取当前电价
const getCurrentPrice = () => {
  const hour = new Date().getHours()
  if (hour >= 23 || hour < 7) {
    return 0.4 // 谷时
  } else if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
    return 1.2 // 峰时
  } else {
    return 0.8 // 平时
  }
}

// 获取电价类型
const getPriceType = () => {
  const hour = new Date().getHours()
  if (hour >= 23 || hour < 7) {
    return '谷时'
  } else if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
    return '峰时'
  } else {
    return '平时'
  }
}

// 结束充电
const handleEndCharging = async () => {
  try {
    await showConfirmDialog({
      title: '确认结束充电',
      message: `当前已充${currentChargeAmount.value.toFixed(2)}kWh
当前SOC: ${currentSoc.value.toFixed(1)}%
总费用: ¥${currentTotalFee.value.toFixed(2)}

确定要结束充电吗？`,
      confirmButtonText: '确认结束',
      cancelButtonText: '继续充电'
    })

    ending.value = true
    stopSimulation()
    stopRefresh()

    // 调用结束充电API
    await endCharging(orderId.value, {
      endSoc: Math.round(currentSoc.value),
      actualChargeAmount: parseFloat(currentChargeAmount.value.toFixed(2))
    })

    showToast('充电已结束')

    // 跳转到订单详情页面(支付)
    setTimeout(() => {
      router.replace({
        name: 'OrderDetail',
        query: { orderId: orderId.value }
      })
    }, 1000)
  } catch (error) {
    if (error !== 'cancel') {
      if (import.meta.env.DEV) {
        console.error('结束充电失败:', error)
      }
      showToast(error.message || '结束充电失败')
      ending.value = false
      // 重新开始模拟和刷新
      startSimulation()
      startRefresh()
    }
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  try {
    return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
  } catch {
    return '-'
  }
}

// 格式化时长(秒转为时分秒)
const formatDuration = (seconds) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}时${minutes}分${secs}秒`
  } else if (minutes > 0) {
    return `${minutes}分${secs}秒`
  } else {
    return `${secs}秒`
  }
}

// 返回按钮处理
const onBack = () => {
  showConfirmDialog({
    title: '提示',
    message: '充电正在进行中，确定要退出吗？\n充电将继续进行，您可以随时返回查看。'
  }).then(() => {
    router.back()
  }).catch(() => {
    // 用户取消
  })
}
</script>

<style scoped lang="scss">
.charging-progress {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 20px;

  .content {
    padding-bottom: 20px;
  }

  .progress-section {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 30px 16px 40px;

    .progress-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;

      :deep(.van-circle) {
        margin-bottom: 20px;
      }

      :deep(.van-circle__text) {
        font-size: 36px;
        font-weight: bold;
      }

      .progress-info {
        display: flex;
        align-items: center;
        gap: 16px;

        .soc-info {
          display: flex;
          flex-direction: column;
          align-items: center;
          color: #fff;

          .label {
            font-size: 12px;
            opacity: 0.8;
            margin-bottom: 4px;
          }

          .value {
            font-size: 20px;
            font-weight: bold;
          }
        }

        .arrow-icon {
          color: #fff;
          font-size: 20px;
          opacity: 0.6;
        }
      }
    }
  }

  .data-cards {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 16px;
    margin-top: -20px;

    .data-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

      .card-icon {
        flex-shrink: 0;
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: #f7f8fa;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .card-content {
        flex: 1;
        min-width: 0;

        .card-label {
          font-size: 12px;
          color: #969799;
          margin-bottom: 4px;
        }

        .card-value {
          font-size: 16px;
          font-weight: bold;
          color: #323233;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }

  :deep(.van-cell-group) {
    margin: 16px;

    .van-cell__title {
      color: #646566;
    }

    .van-cell__value {
      color: #323233;
      font-weight: 500;
    }

    .total-fee {
      color: #ee0a24;
      font-size: 16px;
      font-weight: bold;
    }
  }

  .button-group {
    padding: 16px;
    margin-top: 16px;

    .van-button {
      height: 48px;
      font-size: 16px;
      font-weight: bold;
    }

    .button-tip {
      text-align: center;
      font-size: 12px;
      color: #969799;
      margin-top: 8px;
    }
  }

  .pile-info-group {
    margin-top: 16px;
  }

  .monitoring-group {
    margin-top: 16px;
  }
}
</style>
