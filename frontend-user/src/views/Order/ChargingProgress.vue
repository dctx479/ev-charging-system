<template>
  <div class="charging-progress">
    <van-nav-bar
      title="充电中"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 充电动画 -->
      <div class="charging-animation">
        <div class="battery-icon">
          <div class="battery-fill" :style="{ height: `${currentProgress}%` }">
            <div class="wave"></div>
          </div>
          <div class="battery-percentage">{{ currentProgress }}%</div>
        </div>
      </div>

      <!-- 充电信息 -->
      <van-cell-group title="充电信息" inset>
        <van-cell title="充电桩编号" :value="orderInfo.pileNo" />
        <van-cell title="开始时间" :value="formatTime(orderInfo.startTime)" />
        <van-cell title="充电时长" :value="`${chargeDuration}分钟`" />
        <van-cell title="已充电量" :value="`${currentChargeAmount.toFixed(2)}kWh`" />
        <van-cell title="当前费用" :value="`¥${currentFee.toFixed(2)}`" />
      </van-cell-group>

      <!-- 实时数据 -->
      <van-cell-group title="实时数据" inset style="margin-top: 16px">
        <van-cell title="充电功率" :value="`${orderInfo.pilePower}kW`" />
        <van-cell title="当前电价" :value="currentPrice" />
        <van-cell title="电池温度" :value="`${batteryTemp}°C`" />
        <van-cell title="充电电流" :value="`${chargeCurrent}A`" />
      </van-cell-group>

      <!-- 结束充电按钮 -->
      <div class="button-group">
        <van-button
          type="danger"
          size="large"
          :loading="ending"
          @click="handleEndCharging"
        >
          结束充电
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getOrderDetail, endCharging } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const orderId = ref(route.query.orderId)
const orderInfo = ref({})
const currentProgress = ref(20) // 当前电量百分比
const currentChargeAmount = ref(0) // 当前充电量
const currentFee = ref(0) // 当前费用
const chargeDuration = ref(0) // 充电时长
const batteryTemp = ref(25) // 电池温度
const chargeCurrent = ref(150) // 充电电流
const ending = ref(false)

let progressTimer = null
let durationTimer = null

const currentPrice = computed(() => {
  const hour = new Date().getHours()
  let price = 0.8
  if (hour >= 23 || hour < 7) {
    price = 0.4
  } else if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
    price = 1.2
  }
  return `¥${price}/kWh`
})

onMounted(async () => {
  await loadOrderInfo()
  startSimulation()
})

onUnmounted(() => {
  stopSimulation()
})

const loadOrderInfo = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    orderInfo.value = res.data
    currentProgress.value = orderInfo.value.startSoc || 20
  } catch (error) {
    showToast('加载订单信息失败')
  }
}

const startSimulation = () => {
  // 模拟充电进度（每秒增加0.5%）
  progressTimer = setInterval(() => {
    if (currentProgress.value < 100) {
      currentProgress.value = Math.min(currentProgress.value + 0.5, 100)

      // 模拟充电量增加（基于功率）
      const powerKw = orderInfo.value.pilePower || 120
      currentChargeAmount.value += powerKw / 3600 // 每秒充电量

      // 计算费用
      const hour = new Date().getHours()
      let price = 0.8
      if (hour >= 23 || hour < 7) {
        price = 0.4
      } else if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
        price = 1.2
      }
      const electricityFee = currentChargeAmount.value * price
      const serviceFee = currentChargeAmount.value * 0.5
      currentFee.value = electricityFee + serviceFee

      // 模拟电池温度波动
      batteryTemp.value = 25 + Math.random() * 5

      // 模拟充电电流波动
      chargeCurrent.value = 150 + Math.random() * 20 - 10
    }
  }, 1000)

  // 更新充电时长
  durationTimer = setInterval(() => {
    chargeDuration.value++
  }, 60000) // 每分钟更新
}

const stopSimulation = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
  }
  if (durationTimer) {
    clearInterval(durationTimer)
  }
}

const handleEndCharging = async () => {
  try {
    await showConfirmDialog({
      title: '确认结束充电',
      message: `当前已充电${currentChargeAmount.value.toFixed(2)}kWh\n当前费用：¥${currentFee.value.toFixed(2)}\n确定要结束充电吗？`
    })

    ending.value = true
    stopSimulation()

    // 结束充电
    await endCharging(orderId.value, {
      endSoc: Math.round(currentProgress.value),
      actualChargeAmount: currentChargeAmount.value.toFixed(2)
    })

    showToast('充电已结束')

    // 跳转到订单详情
    router.replace({
      name: 'OrderDetail',
      query: { orderId: orderId.value }
    })
  } catch (error) {
    if (error !== 'cancel') {
      showToast(error.message || '结束充电失败')
      ending.value = false
      startSimulation() // 重新开始模拟
    }
  }
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const onBack = () => {
  showConfirmDialog({
    title: '提示',
    message: '充电正在进行中，确定要退出吗？'
  }).then(() => {
    router.back()
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.charging-progress {
  min-height: 100vh;
  background-color: #f7f8fa;

  .content {
    padding: 16px 0;
  }

  .charging-animation {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

    .battery-icon {
      position: relative;
      width: 150px;
      height: 250px;
      border: 4px solid #fff;
      border-radius: 12px;
      background-color: rgba(255, 255, 255, 0.2);
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: -20px;
        left: 50%;
        transform: translateX(-50%);
        width: 50px;
        height: 20px;
        background-color: #fff;
        border-radius: 0 0 8px 8px;
      }

      .battery-fill {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background: linear-gradient(to top, #4ade80, #22c55e);
        transition: height 0.5s ease;

        .wave {
          position: absolute;
          top: -10px;
          left: 0;
          right: 0;
          height: 20px;
          background-color: rgba(255, 255, 255, 0.3);
          animation: wave 2s infinite linear;
        }
      }

      .battery-percentage {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        font-size: 32px;
        font-weight: bold;
        color: #fff;
        z-index: 10;
      }
    }
  }

  .button-group {
    padding: 16px;

    .van-button {
      margin-top: 12px;
    }
  }
}

@keyframes wave {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
