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
        <van-cell title="充电桩编号" :value="pileInfo.pileNo" />
        <van-cell title="充电功率" :value="`${pileInfo.power}kW`" />
        <van-cell title="充电站" :value="stationInfo.stationName" />
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
    <van-popup v-model:show="showModePicker" position="bottom">
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
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const pileId = ref(route.query.pileId)
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
  return formData.value.startSoc && formData.value.batteryCapacity &&
         (formData.value.chargeMode === 1 || formData.value.targetValue)
})

onMounted(async () => {
  await loadPileInfo()
})

const loadPileInfo = async () => {
  try {
    const res = await getPileDetail(pileId.value)
    pileInfo.value = res.data.pile
    stationInfo.value = res.data.station
    formData.value.chargePower = pileInfo.value.power
  } catch (error) {
    showToast('加载充电桩信息失败')
  }
}

const onModeConfirm = ({ selectedOptions }) => {
  formData.value.chargeMode = selectedOptions[0].value
  formData.value.targetValue = null
  prediction.value = null
  showModePicker.value = false
}

const getPrediction = async () => {
  if (!formData.value.startSoc || !formData.value.batteryCapacity) {
    showToast('请先填写当前电量和电池容量')
    return
  }

  predicting.value = true
  try {
    // 调用AI预测服务（假设充满到80%）
    const targetSoc = formData.value.chargeMode === 1 ? 80 :
                      Math.min(parseInt(formData.value.startSoc) + 50, 100)

    const response = await axios.post('http://localhost:5000/api/ai/predict/duration', {
      battery_capacity: parseFloat(formData.value.batteryCapacity),
      current_soc: parseInt(formData.value.startSoc),
      target_soc: targetSoc,
      charge_power: parseFloat(formData.value.chargePower),
      temperature: 25
    })

    prediction.value = response.data
    showToast('预测成功')
  } catch (error) {
    console.error('AI预测失败', error)
    showToast('AI预测服务暂不可用')
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
