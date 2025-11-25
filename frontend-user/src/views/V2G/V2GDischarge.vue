<template>
  <div class="v2g-discharge">
    <van-nav-bar
      title="V2G双向充电"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 电池状态 -->
      <van-cell-group title="电池状态" inset>
        <van-cell title="当前电量" :value="`${batteryStatus.currentSoc}%`">
          <template #icon>
            <van-icon name="fire" color="#07c160" />
          </template>
        </van-cell>
        <van-cell title="电池容量" :value="`${batteryStatus.capacity} kWh`">
          <template #icon>
            <van-icon name="hot" color="#1989fa" />
          </template>
        </van-cell>
        <van-cell title="可放电量" :value="`${availableDischarge} kWh`">
          <template #icon>
            <van-icon name="flash" color="#ff976a" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 放电状态 -->
      <van-cell-group v-if="isDischarging" title="放电状态" inset style="margin-top: 16px">
        <van-cell title="放电功率" :value="`${dischargeStatus.power} kW`" />
        <van-cell title="已放电量" :value="`${dischargeStatus.discharged.toFixed(2)} kWh`" />
        <van-cell title="当前收益" :value="`¥${dischargeStatus.income.toFixed(2)}`" />
        <van-cell title="持续时长" :value="durationText" />

        <!-- 放电进度 -->
        <van-cell title="放电进度">
          <template #value>
            <van-progress
              :percentage="dischargeProgress"
              :show-pivot="true"
              color="#07c160"
              stroke-width="8px"
            />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 放电控制面板 -->
      <van-cell-group v-if="!isDischarging" title="放电策略配置" inset style="margin-top: 16px">
        <van-form @submit="onSubmit">
          <van-field
            v-model="formData.pileId"
            type="number"
            label="充电桩ID"
            placeholder="请输入充电桩ID"
            :rules="[{ required: true, message: '请输入充电桩ID' }]"
          />

          <van-field
            v-model="formData.currentSoc"
            type="number"
            label="当前电量(%)"
            placeholder="输入当前电池电量百分比"
            :rules="[
              { required: true, message: '请输入当前电量' },
              { validator: validateCurrentSoc, message: '当前电量必须在0-100之间' }
            ]"
          />

          <van-field
            v-model="formData.batteryHealthBefore"
            type="number"
            label="电池健康度(%)"
            placeholder="输入电池健康度"
            :rules="[
              { required: true, message: '请输入电池健康度' },
              { validator: validateBatteryHealth, message: '健康度必须在0-100之间' }
            ]"
          />

          <van-field name="minPrice" label="最低放电价格(元/kWh)">
            <template #input>
              <van-slider
                v-model="formData.minPrice"
                :min="0.5"
                :max="2.0"
                :step="0.1"
              />
              <span class="slider-value">{{ formData.minPrice }} 元/kWh</span>
            </template>
          </van-field>
        </van-form>
      </van-cell-group>

      <!-- 收益预估计算器 -->
      <van-cell-group v-if="!isDischarging" title="收益预估" inset style="margin-top: 16px">
        <van-cell>
          <van-button
            type="primary"
            size="small"
            :loading="estimating"
            @click="calculateProfit"
          >
            计算收益
          </van-button>
        </van-cell>

        <van-cell v-if="profitEstimate" title="预估放电量" :value="`${profitEstimate.energyAmount} kWh`" />
        <van-cell v-if="profitEstimate" title="预估收益" :value="`¥${profitEstimate.profit}`" />
      </van-cell-group>

      <!-- 历史放电记录 -->
      <van-cell-group title="历史记录" inset style="margin-top: 16px">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadRecords"
        >
          <van-cell
            v-for="record in records"
            :key="record.id"
            :title="`${record.energyAmount} kWh`"
            :label="formatDate(record.startTime)"
            is-link
            @click="viewRecordDetail(record)"
          >
            <template #value>
              <span class="profit-value">¥{{ record.profit }}</span>
            </template>
          </van-cell>

          <van-empty v-if="records.length === 0 && !loading" description="暂无放电记录" />
        </van-list>
      </van-cell-group>

      <!-- 操作按钮 -->
      <div class="button-group">
        <van-button
          v-if="!isDischarging"
          type="success"
          size="large"
          :loading="starting"
          :disabled="!canStart"
          @click="handleStartDischarge"
        >
          开始放电
        </van-button>

        <van-button
          v-else
          type="danger"
          size="large"
          :loading="stopping"
          @click="handleStopDischarge"
        >
          停止放电
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { startDischarge, stopDischarge, getV2GRecords, getProfitEstimate } from '@/api/v2g'
import { formatDate } from '@/utils/dateFormat'

const router = useRouter()

// 电池状态
const batteryStatus = ref({
  currentSoc: 80,
  capacity: 75,
  minSoc: 20
})

// 放电状态
const isDischarging = ref(false)
const dischargeStatus = ref({
  recordId: null,
  power: 0,
  discharged: 0,
  income: 0,
  startTime: null,
  targetSoc: 0
})

// 表单数据 - 与StartDischargeDTO匹配
const formData = ref({
  pileId: '',
  currentSoc: 80,
  batteryHealthBefore: 95,
  minPrice: 1.0
})

// 收益预估
const profitEstimate = ref(null)
const estimating = ref(false)

// 历史记录
const records = ref([])
const loading = ref(false)
const finished = ref(false)
const page = ref(1)

// 按钮状态
const starting = ref(false)
const stopping = ref(false)

// 定时器
let statusTimer = null

// 计算属性
const availableDischarge = computed(() => {
  const available = (batteryStatus.value.currentSoc - batteryStatus.value.minSoc) / 100 * batteryStatus.value.capacity
  return Math.max(0, available).toFixed(2)
})

const dischargeProgress = computed(() => {
  if (!dischargeStatus.value.targetSoc) return 0
  const total = batteryStatus.value.currentSoc - dischargeStatus.value.targetSoc
  const current = dischargeStatus.value.discharged / batteryStatus.value.capacity * 100
  return Math.min(100, (current / total * 100).toFixed(1))
})

const durationText = computed(() => {
  if (!dischargeStatus.value.startTime) return '0分钟'
  const start = new Date(dischargeStatus.value.startTime)
  const now = new Date()
  const minutes = Math.floor((now - start) / 60000)
  return `${minutes}分钟`
})

const canStart = computed(() => {
  return formData.value.pileId &&
         formData.value.currentSoc !== null &&
         formData.value.batteryHealthBefore !== null &&
         formData.value.minPrice
})

// 验证函数 - 匹配StartDischargeDTO验证
const validateCurrentSoc = (val) => {
  const num = Number(val)
  return num >= 0 && num <= 100
}

const validateBatteryHealth = (val) => {
  const num = Number(val)
  return num >= 0 && num <= 100
}

// 返回上一页
const onBack = () => {
  router.back()
}

// 计算收益
const calculateProfit = async () => {
  if (!formData.value.pileId || formData.value.currentSoc === null) {
    showToast('请先完整填写放电配置')
    return
  }

  estimating.value = true
  try {
    const params = {
      currentSoc: formData.value.currentSoc,
      targetSoc: Math.max(0, formData.value.currentSoc - 30), // 默认放电到当前电量-30%
      batteryCapacity: batteryStatus.value.capacity,
      maxPower: 10, // 使用V2G的默认功率
      duration: 60  // 默认1小时
    }

    const res = await getProfitEstimate(params)
    if (res.code === 200) {
      profitEstimate.value = res.data
    } else {
      showToast(res.message || '获取收益预估失败')
    }
  } catch (error) {
    console.error('Calculate profit error:', error)
    showToast('网络错误')
  } finally {
    estimating.value = false
  }
}

// 开始放电 - 发送StartDischargeDTO
const handleStartDischarge = async () => {
  if (!canStart.value) {
    showToast('请完整填写放电配置')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认开始放电',
      message: `将使用充电桩${formData.value.pileId}开始V2G放电，最低电价${formData.value.minPrice}元/kWh`,
    })
  } catch {
    return
  }

  starting.value = true
  try {
    const data = {
      pileId: Number(formData.value.pileId),
      currentSoc: Number(formData.value.currentSoc),
      batteryHealthBefore: Number(formData.value.batteryHealthBefore),
      minPrice: Number(formData.value.minPrice)
    }

    const res = await startDischarge(data)
    if (res.code === 200) {
      showToast('放电已启动')
      isDischarging.value = true
      dischargeStatus.value = {
        recordId: res.data.id,
        power: 10, // V2G默认功率
        discharged: 0,
        income: 0,
        startTime: new Date().toISOString(),
        targetSoc: Math.max(0, formData.value.currentSoc - 30)
      }

      // 开始轮询状态
      startStatusPolling()
    } else {
      showToast(res.message || '启动放电失败')
    }
  } catch (error) {
    console.error('Start discharge error:', error)
    showToast('网络错误')
  } finally {
    starting.value = false
  }
}

// 停止放电
const handleStopDischarge = async () => {
  try {
    await showConfirmDialog({
      title: '确认停止放电',
      message: '确定要停止当前放电吗？',
    })
  } catch {
    return
  }

  stopping.value = true
  try {
    const data = {
      recordId: dischargeStatus.value.recordId,
      energyAmount: Number(dischargeStatus.value.discharged.toFixed(3))
    }

    const res = await stopDischarge(data)
    if (res.code === 200) {
      showToast('已停止放电')
      isDischarging.value = false
      stopStatusPolling()

      // 刷新记录列表
      records.value = []
      page.value = 1
      finished.value = false
      loadRecords()
    } else {
      showToast(res.message || '停止放电失败')
    }
  } catch (error) {
    console.error('Stop discharge error:', error)
    showToast('网络错误')
  } finally {
    stopping.value = false
  }
}

// 加载历史记录
const loadRecords = async () => {
  if (finished.value) return

  loading.value = true
  try {
    const params = {
      page: page.value,
      size: 10
    }

    const res = await getV2GRecords(params)
    if (res.code === 200) {
      // 后端直接返回数组，不是分页对象
      const newRecords = res.data || []
      records.value = [...records.value, ...newRecords]
      finished.value = true  // 后端不分页，一次返回全部
    }
  } catch (error) {
    console.error('Load records error:', error)
  } finally {
    loading.value = false
  }
}

// 查看记录详情
const viewRecordDetail = (record) => {
  showToast('记录详情功能待开发')
  // TODO: 导航到详情页
}

// 开始状态轮询
const startStatusPolling = () => {
  statusTimer = setInterval(() => {
    // 模拟状态更新（实际应从后端获取）
    if (isDischarging.value) {
      dischargeStatus.value.discharged += 0.5
      dischargeStatus.value.income += 0.5
      batteryStatus.value.currentSoc = Math.max(
        dischargeStatus.value.targetSoc,
        batteryStatus.value.currentSoc - 0.1
      )
    }
  }, 3000)
}

// 停止状态轮询
const stopStatusPolling = () => {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
}

// 生命周期
onMounted(() => {
  loadRecords()
})

onBeforeUnmount(() => {
  stopStatusPolling()
})
</script>

<style scoped lang="scss">
.v2g-discharge {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 80px;

  .content {
    padding: 16px;
  }

  .slider-value {
    margin-left: 12px;
    color: #1989fa;
    font-weight: 500;
  }

  .profit-value {
    color: #07c160;
    font-weight: 600;
    font-size: 16px;
  }

  .button-group {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: #fff;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);

    .van-button {
      width: 100%;
    }
  }

  :deep(.van-cell-group) {
    margin-bottom: 16px;
  }

  :deep(.van-cell__title) {
    font-weight: 500;
  }

  :deep(.van-progress) {
    width: 100%;
  }
}
</style>
