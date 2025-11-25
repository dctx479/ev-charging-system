<template>
  <div class="create-order">
    <van-nav-bar
      title="开始充电"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 充电桩信息 -->
      <van-cell-group title="充电桩信息" inset>
        <van-cell title="充电桩编号" :value="pileInfo.pileNo || '-'" />
        <van-cell title="充电功率" :value="pileInfo.power ? `${pileInfo.power}kW` : '-'" />
        <van-cell title="充电站" :value="stationInfo.name || '-'" />
        <van-cell title="当前电价" :value="currentPrice" />
      </van-cell-group>

      <!-- 充电设置 -->
      <van-cell-group title="充电设置" inset style="margin-top: 16px">
        <van-field
          v-model="formData.startSoc"
          type="number"
          label="当前电量"
          placeholder="请输入当前电量"
          right-icon="percent"
          :rules="[{ required: true, message: '请输入当前电量' }]"
        />
        <van-field
          v-model="formData.batteryCapacity"
          type="number"
          label="电池容量"
          placeholder="请输入电池容量"
          right-icon="kWh"
          :rules="[{ required: true, message: '请输入电池容量' }]"
        />
        <van-cell title="充电模式" is-link @click="showModePicker = true">
          <template #value>
            <span>{{ chargeModeText }}</span>
          </template>
        </van-cell>
        <van-field
          v-if="formData.chargeMode !== 1"
          v-model="formData.targetValue"
          type="number"
          :label="targetLabel"
          :placeholder="`请输入${targetLabel}`"
        />
      </van-cell-group>

      <!-- AI预测结果 -->
      <van-cell-group v-if="prediction" title="AI智能预测" inset style="margin-top: 16px">
        <van-cell title="预计充电时长" :value="`${prediction.duration}分钟`" />
        <van-cell title="预计充电量" :value="`${prediction.charge_amount}kWh`" />
        <van-cell title="预计费用" :value="`¥${prediction.estimated_cost}`" />
      </van-cell-group>

      <!-- 按钮组 -->
      <div class="button-group">
        <van-button
          type="primary"
          size="large"
          :loading="predicting"
          @click="getPrediction"
        >
          AI预测充电信息
        </van-button>
        <van-button
          type="success"
          size="large"
          :loading="creating"
          :disabled="!canCreate"
          @click="handleCreateOrder"
        >
          开始充电
        </van-button>
      </div>
    </div>

    <!-- 充电模式选择器 -->
    <van-popup v-model:show="showModePicker" position="bottom" round>
      <van-picker
        :columns="chargeModeOptions"
        @confirm="onModeConfirm"
        @cancel="showModePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getPileDetail } from '@/api/pile'
import { createOrder } from '@/api/order'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

const pileId = ref(parseInt(route.query.pileId))
const stationId = ref(parseInt(route.query.stationId))
const pileInfo = ref({})
const stationInfo = ref({})

const formData = ref({
  pileId: pileId.value,
  chargeMode: 1, // 1-充满 2-按金额 3-按电量 4-按时间
  targetValue: null,
  startSoc: null,
  batteryCapacity: null,
  chargePower: null
})

const showModePicker = ref(false)
const prediction = ref(null)
const predicting = ref(false)
const creating = ref(false)

const chargeModeOptions = [
  { text: '充满', value: 1 },
  { text: '按金额充电', value: 2 },
  { text: '按电量充电', value: 3 },
  { text: '按时间充电', value: 4 }
]

const chargeModeText = computed(() => {
  const mode = chargeModeOptions.find(m => m.value === formData.value.chargeMode)
  return mode ? mode.text : '充满'
})

const targetLabel = computed(() => {
  switch (formData.value.chargeMode) {
    case 2: return '充电金额（元）'
    case 3: return '充电量（kWh）'
    case 4: return '充电时长（分钟）'
    default: return ''
  }
})

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

const canCreate = computed(() => {
  return formData.value.startSoc !== null && formData.value.startSoc !== '' &&
         formData.value.batteryCapacity &&
         (formData.value.chargeMode === 1 || formData.value.targetValue)
})

onMounted(async () => {
  // 验证参数
  if (!pileId.value || isNaN(pileId.value)) {
    showToast('充电桩参数错误')
    setTimeout(() => router.back(), 1500)
    return
  }

  await loadPileInfo()
})

const loadPileInfo = async () => {
  try {
    const res = await getPileDetail(pileId.value)
    console.log('Pile detail response:', res.data)
    pileInfo.value = res.data.pile || res.data
    stationInfo.value = res.data.station || {}

    // 获取充电功率，确保是数字
    const power = pileInfo.value.power || 120
    formData.value.chargePower = parseFloat(power)

    console.log('充电桩功率已设置:', formData.value.chargePower, 'kW')
  } catch (error) {
    console.error('加载充电桩信息失败:', error)
    showToast('加载充电桩信息失败')
  }
}

const onModeConfirm = ({ selectedOptions }) => {
  formData.value.chargeMode = selectedOptions[0].value
  formData.value.targetValue = null
  prediction.value = null
  showModePicker.value = false
}

// 计算目标电量百分比
const calculateTargetSoc = () => {
  const startSoc = parseInt(formData.value.startSoc) || 0
  const batteryCapacity = parseFloat(formData.value.batteryCapacity) || 0
  const targetValue = parseFloat(formData.value.targetValue) || 0
  const chargePower = parseFloat(formData.value.chargePower) || 120

  switch (formData.value.chargeMode) {
    case 1: // 充满
      return 100
    case 2: // 按金额 - 估算可充电量
      // 假设平均电价0.8元/kWh + 服务费0.5元/kWh = 1.3元/kWh
      const estimatedKwh = targetValue / 1.3
      const socIncrease = (estimatedKwh / batteryCapacity) * 100
      return Math.min(startSoc + socIncrease, 100)
    case 3: // 按电量(kWh)
      const socFromKwh = (targetValue / batteryCapacity) * 100
      return Math.min(startSoc + socFromKwh, 100)
    case 4: // 按时间(分钟)
      const kwhFromTime = (chargePower * targetValue) / 60
      const socFromTime = (kwhFromTime / batteryCapacity) * 100
      return Math.min(startSoc + socFromTime, 100)
    default:
      return Math.min(startSoc + 50, 100)
  }
}

const getPrediction = async () => {
  // 验证必填字段
  if (!formData.value.batteryCapacity || formData.value.batteryCapacity <= 0) {
    showToast('请先填写电池容量')
    return
  }

  if (!formData.value.startSoc || formData.value.startSoc < 0) {
    showToast('请先填写当前电量')
    return
  }

  // 确保充电功率已加载
  if (!formData.value.chargePower || formData.value.chargePower <= 0) {
    showToast('充电功率未加载，请稍后重试')
    return
  }

  // 非充满模式需要填写目标值
  if (formData.value.chargeMode !== 1 && (!formData.value.targetValue || formData.value.targetValue <= 0)) {
    showToast(`请先填写${targetLabel.value}`)
    return
  }

  predicting.value = true
  try {
    const targetSoc = calculateTargetSoc()

    // 准备请求参数
    const requestData = {
      battery_capacity: parseFloat(formData.value.batteryCapacity),
      current_soc: parseInt(formData.value.startSoc),
      target_soc: Math.round(targetSoc),
      charge_power: parseFloat(formData.value.chargePower),
      temperature: 25
    }

    console.log('AI预测请求参数:', requestData)

    // 通过后端API代理调用AI服务
    const response = await request({
      url: '/ai/predict/duration',
      method: 'post',
      data: requestData
    })

    console.log('AI prediction response:', response)

    // 后端返回格式: { code: 200, data: { duration, charge_amount, estimated_cost } }
    if (response.data) {
      prediction.value = {
        duration: response.data.duration || 0,
        charge_amount: response.data.charge_amount || response.data.chargeAmount || 0,
        estimated_cost: response.data.estimated_cost || response.data.estimatedCost || 0
      }
      showToast('预测成功')
    } else {
      throw new Error('预测数据为空')
    }
  } catch (error) {
    console.error('AI预测失败', error)
    const errorMsg = error.response?.data?.message || error.message || 'AI预测服务暂不可用'
    console.error('错误详情:', error.response?.data)
    showToast(errorMsg)
  } finally {
    predicting.value = false
  }
}

const handleCreateOrder = async () => {
  try {
    await showConfirmDialog({
      title: '确认开始充电',
      message: prediction.value ?
        `预计充电时长：${prediction.value.duration}分钟\n预计费用：¥${prediction.value.estimated_cost}` :
        '确定要开始充电吗？'
    })

    creating.value = true
    const res = await createOrder(formData.value)

    showToast('充电已开始')
    // 跳转到充电进度页面
    router.replace({
      name: 'ChargingProgress',
      query: { orderId: res.data }
    })
  } catch (error) {
    if (error !== 'cancel') {
      showToast(error.message || '创建订单失败')
    }
  } finally {
    creating.value = false
  }
}

const onBack = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.create-order {
  min-height: 100vh;
  background-color: #f7f8fa;

  .content {
    padding: 16px 0;
  }

  .button-group {
    padding: 16px;

    .van-button {
      margin-bottom: 12px;
    }
  }
}
</style>
