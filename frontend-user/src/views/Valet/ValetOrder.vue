<template>
  <div class="valet-order">
    <van-nav-bar
      title="代充服务"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 服务说明 -->
      <van-notice-bar
        left-icon="info-o"
        text="专业师傅为您移车充电，安全便捷，无需等待"
        background="#fff7cc"
        color="#ed6a0c"
      />

      <!-- 车辆信息 -->
      <van-cell-group title="车辆信息" inset style="margin-top: 16px">
        <van-form ref="formRef">
          <van-field
            v-model="formData.vehiclePlate"
            label="车牌号"
            placeholder="请输入车牌号"
            :rules="[{ required: true, message: '请输入车牌号' }]"
          />

          <van-field
            v-model="formData.vehicleModel"
            label="车辆型号"
            placeholder="请输入车辆型号"
            :rules="[{ required: true, message: '请输入车辆型号' }]"
          />

          <van-field
            v-model="formData.currentSoc"
            type="number"
            label="当前电量(%)"
            placeholder="请输入当前电量"
            :rules="[
              { required: true, message: '请输入当前电量' },
              { validator: validateSoc, message: '电量范围0-100' }
            ]"
          />

          <van-field
            v-model="formData.targetSoc"
            type="number"
            label="目标电量(%)"
            placeholder="请输入目标电量"
            :rules="[
              { required: true, message: '请输入目标电量' },
              { validator: validateTargetSoc, message: '目标电量必须大于当前电量' }
            ]"
          />

          <van-field
            v-model="formData.batteryCapacity"
            type="number"
            label="电池容量(kWh)"
            placeholder="请输入电池容量"
            :rules="[{ required: true, message: '请输入电池容量' }]"
          />
        </van-form>
      </van-cell-group>

      <!-- 充电桩选择 -->
      <van-cell-group title="充电桩选择" inset style="margin-top: 16px">
        <van-cell
          title="选择充电桩"
          :value="selectedPile ? selectedPile.pileNo : '请选择'"
          is-link
          @click="showPileSelector = true"
        />

        <van-cell v-if="selectedPile" title="充电站" :value="selectedStation?.name" />
        <van-cell v-if="selectedPile" title="充电功率" :value="`${selectedPile.power}kW`" />
        <van-cell v-if="selectedPile" title="服务费" :value="`¥${serviceFee}/次`" />
      </van-cell-group>

      <!-- 时间要求 -->
      <van-cell-group title="时间要求" inset style="margin-top: 16px">
        <van-cell
          title="期望完成时间"
          :value="expectedTimeText"
          is-link
          @click="showTimePicker = true"
        />

        <van-field
          v-model="formData.remarks"
          type="textarea"
          label="备注说明"
          placeholder="请输入特殊要求或备注"
          rows="2"
          maxlength="200"
          show-word-limit
        />
      </van-cell-group>

      <!-- 费用预估 -->
      <van-cell-group v-if="costEstimate" title="费用预估" inset style="margin-top: 16px">
        <van-cell title="预计充电量" :value="`${costEstimate.chargeAmount} kWh`" />
        <van-cell title="预计充电费" :value="`¥${costEstimate.chargeFee}`" />
        <van-cell title="服务费" :value="`¥${costEstimate.serviceFee}`" />
        <van-cell title="总计" :value="`¥${costEstimate.totalFee}`">
          <template #value>
            <span class="total-fee">¥{{ costEstimate.totalFee }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 操作按钮 -->
      <div class="button-group">
        <van-button
          type="primary"
          size="large"
          :loading="estimating"
          @click="calculateCost"
        >
          计算费用
        </van-button>

        <van-button
          type="success"
          size="large"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交订单
        </van-button>
      </div>
    </div>

    <!-- 充电桩选择弹窗 -->
    <van-popup
      v-model:show="showPileSelector"
      position="bottom"
      :style="{ height: '70%' }"
      round
    >
      <div class="pile-selector">
        <div class="selector-header">
          <span class="title">选择充电桩</span>
          <van-icon name="cross" @click="showPileSelector = false" />
        </div>

        <div class="search-bar">
          <van-search
            v-model="searchKeyword"
            placeholder="搜索充电站或充电桩"
            @search="searchPiles"
          />
        </div>

        <van-list
          v-model:loading="loadingPiles"
          :finished="pilesFinished"
          finished-text="没有更多了"
          @load="loadPiles"
        >
          <div
            v-for="pile in availablePiles"
            :key="pile.id"
            class="pile-item"
            :class="{ selected: selectedPile?.id === pile.id }"
            @click="selectPile(pile)"
          >
            <div class="pile-info">
              <div class="pile-no">{{ pile.pileNo }}</div>
              <div class="pile-station">{{ pile.stationName }}</div>
              <div class="pile-power">{{ pile.power }}kW</div>
            </div>
            <van-icon
              v-if="selectedPile?.id === pile.id"
              name="success"
              color="#07c160"
              size="24"
            />
          </div>

          <van-empty v-if="availablePiles.length === 0 && !loadingPiles" description="暂无可用充电桩" />
        </van-list>
      </div>
    </van-popup>

    <!-- 时间选择器 -->
    <van-popup v-model:show="showTimePicker" position="bottom" round>
      <van-picker-group
        title="选择期望完成时间"
        :tabs="['选择日期', '选择时间']"
        @confirm="onTimeConfirm"
        @cancel="showTimePicker = false"
      >
        <van-date-picker
          v-model="pickerDate"
          :min-date="minDate"
          :max-date="maxDate"
        />
        <van-time-picker v-model="pickerTime" />
      </van-picker-group>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { createValetOrder } from '@/api/valet'
import { getPileList } from '@/api/pile'
import { getStationDetail } from '@/api/station'

const router = useRouter()

// 表单数据
const formData = ref({
  vehiclePlate: '',
  vehicleModel: '',
  currentSoc: '',
  targetSoc: '',
  batteryCapacity: '',
  remarks: ''
})

// 充电桩相关
const selectedPile = ref(null)
const selectedStation = ref(null)
const showPileSelector = ref(false)
const availablePiles = ref([])
const loadingPiles = ref(false)
const pilesFinished = ref(false)
const searchKeyword = ref('')
const serviceFee = ref(20) // 服务费

// 时间选择
const showTimePicker = ref(false)
const defaultTime = new Date(Date.now() + 2 * 60 * 60 * 1000) // 默认2小时后
const pickerDate = ref([
  String(defaultTime.getFullYear()),
  String(defaultTime.getMonth() + 1).padStart(2, '0'),
  String(defaultTime.getDate()).padStart(2, '0')
])
const pickerTime = ref([
  String(defaultTime.getHours()).padStart(2, '0'),
  String(defaultTime.getMinutes()).padStart(2, '0')
])
const expectedTime = ref(defaultTime)
const minDate = ref(new Date())
const maxDate = ref(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)) // 7天内

// 费用预估
const costEstimate = ref(null)
const estimating = ref(false)

// 提交状态
const submitting = ref(false)

// 计算属性
const expectedTimeText = computed(() => {
  return `${pickerDate.value.join('-')} ${pickerTime.value.join(':')}`
})

const canSubmit = computed(() => {
  return formData.value.vehiclePlate &&
         formData.value.vehicleModel &&
         formData.value.currentSoc &&
         formData.value.targetSoc &&
         formData.value.batteryCapacity &&
         selectedPile.value &&
         costEstimate.value
})

// 验证函数
const validateSoc = (val) => {
  const num = Number(val)
  return num >= 0 && num <= 100
}

const validateTargetSoc = (val) => {
  const num = Number(val)
  return num > Number(formData.value.currentSoc) && num <= 100
}

// 返回上一页
const onBack = () => {
  router.back()
}

// 加载充电桩列表
const loadPiles = async () => {
  if (pilesFinished.value) return

  loadingPiles.value = true
  try {
    const params = {
      status: 1, // 仅加载空闲充电桩（1=空闲）
      keyword: searchKeyword.value
    }

    const res = await getPileList(params)
    if (res.code === 200) {
      const piles = res.data || []
      availablePiles.value = piles
      pilesFinished.value = true
    }
  } catch (error) {
    console.error('Load piles error:', error)
    showToast('加载充电桩失败')
  } finally {
    loadingPiles.value = false
  }
}

// 搜索充电桩
const searchPiles = () => {
  availablePiles.value = []
  pilesFinished.value = false
  loadPiles()
}

// 选择充电桩
const selectPile = async (pile) => {
  selectedPile.value = pile

  // 获取充电站信息
  try {
    const res = await getStationDetail(pile.stationId)
    if (res.code === 200) {
      selectedStation.value = res.data
    }
  } catch (error) {
    console.error('Get station error:', error)
  }

  showPileSelector.value = false
}

// 确认时间
const onTimeConfirm = () => {
  const [year, month, day] = pickerDate.value
  const [hour, minute] = pickerTime.value
  expectedTime.value = new Date(year, month - 1, day, hour, minute)
  showTimePicker.value = false
}

// 计算费用
const calculateCost = async () => {
  if (!formData.value.currentSoc || !formData.value.targetSoc || !formData.value.batteryCapacity) {
    showToast('请完整填写车辆信息')
    return
  }

  if (!selectedPile.value) {
    showToast('请选择充电桩')
    return
  }

  estimating.value = true
  try {
    // 计算充电量
    const chargeAmount = ((Number(formData.value.targetSoc) - Number(formData.value.currentSoc)) / 100 * Number(formData.value.batteryCapacity)).toFixed(2)

    // 模拟计算充电费（实际应从后端获取）
    const avgPrice = 0.8 // 平均电价
    const chargeFee = (chargeAmount * avgPrice).toFixed(2)
    const totalFee = (Number(chargeFee) + serviceFee.value).toFixed(2)

    costEstimate.value = {
      chargeAmount,
      chargeFee,
      serviceFee: serviceFee.value,
      totalFee
    }

    showToast('费用计算完成')
  } catch (error) {
    console.error('Calculate cost error:', error)
    showToast('计算费用失败')
  } finally {
    estimating.value = false
  }
}

// 提交订单
const handleSubmit = async () => {
  if (!canSubmit.value) {
    showToast('请完整填写信息并计算费用')
    return
  }

  // Vant 4 showConfirmDialog: 确认时resolve，取消时reject
  try {
    await showConfirmDialog({
      title: '确认提交',
      message: `总费用：¥${costEstimate.value.totalFee}，确认提交代充订单吗？`,
    })
  } catch {
    return // 用户点击取消
  }

  submitting.value = true
  try {
    const data = {
      stationId: selectedPile.value.stationId,
      pileId: selectedPile.value.id,
      carPlate: formData.value.vehiclePlate,
      parkingLocation: formData.value.remarks || '待确认',
      targetSoc: Number(formData.value.targetSoc),
      pickupTime: `${pickerDate.value.join('-')} ${pickerTime.value.join(':')}:00`
    }

    const res = await createValetOrder(data)
    if (res.code === 200) {
      showToast('订单提交成功')

      // 延迟跳转到订单列表
      setTimeout(() => {
        router.push('/valet/orders')
      }, 1500)
    } else {
      showToast(res.message || '提交订单失败')
    }
  } catch (error) {
    console.error('Submit order error:', error)
    showToast('网络错误')
  } finally {
    submitting.value = false
  }
}

// 生命周期
onMounted(() => {
  loadPiles()
})
</script>

<style scoped lang="scss">
.valet-order {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 120px;

  .content {
    padding: 16px;
  }

  .total-fee {
    color: #ee0a24;
    font-size: 18px;
    font-weight: 600;
  }

  .button-group {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: #fff;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
    display: flex;
    gap: 12px;

    .van-button {
      flex: 1;
    }
  }

  .pile-selector {
    height: 100%;
    display: flex;
    flex-direction: column;

    .selector-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 1px solid #ebedf0;

      .title {
        font-size: 16px;
        font-weight: 600;
      }

      .van-icon {
        font-size: 20px;
        cursor: pointer;
      }
    }

    .search-bar {
      padding: 12px 16px;
      background: #fff;
    }

    .van-list {
      flex: 1;
      overflow-y: auto;
      padding: 0 16px;
    }

    .pile-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      margin-bottom: 12px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid #ebedf0;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: #1989fa;
      }

      &.selected {
        border-color: #07c160;
        background: #f0f9ff;
      }

      .pile-info {
        flex: 1;

        .pile-no {
          font-size: 16px;
          font-weight: 600;
          margin-bottom: 4px;
        }

        .pile-station {
          font-size: 14px;
          color: #646566;
          margin-bottom: 4px;
        }

        .pile-power {
          font-size: 12px;
          color: #969799;
        }
      }
    }
  }
}
</style>
