<template>
  <div class="valet-service">
    <van-nav-bar
      title="代充服务"
      left-arrow
      @click-left="$router.back()"
    />

    <!-- 服务介绍 -->
    <van-notice-bar
      left-icon="volume-o"
      text="专业师傅帮您移车充电，省时省心！"
      background="#fff7e8"
      color="#ed6a0c"
    />

    <!-- 选择师傅 -->
    <van-cell-group class="charger-section" title="选择代充师傅">
      <div class="charger-list">
        <div
          v-for="charger in chargers"
          :key="charger.id"
          :class="['charger-card', selectedCharger?.id === charger.id ? 'selected' : '']"
          @click="selectCharger(charger)"
        >
          <div class="charger-info">
            <van-image
              round
              width="60"
              height="60"
              :src="charger.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            />
            <div class="charger-details">
              <div class="charger-name">{{ charger.name }}</div>
              <div class="charger-rating">
                <van-rate v-model="charger.rating" size="14" readonly />
                <span class="rating-text">({{ charger.orderCount }}单)</span>
              </div>
              <div class="charger-status">
                <van-tag :type="charger.status === 1 ? 'success' : 'default'">
                  {{ charger.status === 1 ? '空闲' : '忙碌' }}
                </van-tag>
              </div>
            </div>
          </div>
          <div class="charger-price">¥{{ charger.servicePrice }}/次</div>
        </div>
      </div>
      <van-empty v-if="chargers.length === 0" description="暂无可用师傅" />
    </van-cell-group>

    <!-- 订单信息 -->
    <van-cell-group class="order-section" title="填写信息">
      <van-form @submit="createOrder">
        <van-field
          v-model="orderForm.stationId"
          name="stationId"
          label="充电站ID"
          type="number"
          placeholder="请输入充电站ID"
          required
          :rules="[{ required: true, message: '请输入充电站ID' }]"
        />
        <van-field
          v-model="orderForm.pileId"
          name="pileId"
          label="充电桩ID"
          type="number"
          placeholder="请输入充电桩ID"
          required
          :rules="[{ required: true, message: '请输入充电桩ID' }]"
        />
        <van-field
          v-model="orderForm.carPlate"
          name="carPlate"
          label="车牌号"
          placeholder="请输入车牌号"
          required
          :rules="[{ required: true, message: '请输入车牌号' }]"
        />
        <van-field
          v-model="orderForm.carLocation"
          name="carLocation"
          label="车辆位置"
          placeholder="如：A区2号停车位"
          required
          :rules="[{ required: true, message: '请输入车辆位置' }]"
        />
        <van-field
          v-model="orderForm.targetSoc"
          name="targetSoc"
          label="目标电量(%)"
          type="number"
          placeholder="请输入目标电量"
          required
          :rules="[{ required: true, message: '请输入目标电量' }]"
        />
        <van-field
          v-model="orderForm.phone"
          name="phone"
          label="联系电话"
          type="tel"
          placeholder="请输入联系电话"
          required
          :rules="[{ required: true, message: '请输入联系电话' }]"
        />
        <van-field
          v-model="orderForm.remark"
          name="remark"
          label="备注"
          type="textarea"
          placeholder="如有特殊要求请填写"
          rows="2"
        />

        <!-- 费用预估 -->
        <div class="fee-estimate">
          <van-cell title="服务费" :value="`¥${selectedCharger?.servicePrice || 0}`" />
          <van-cell title="预估电费" :value="`¥${estimatedChargeFee}`" />
          <van-cell title="合计" :value="`¥${totalFee}`" value-class="total-fee" />
        </div>

        <div class="submit-button-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :disabled="!selectedCharger"
          >
            立即下单
          </van-button>
        </div>
      </van-form>
    </van-cell-group>

    <!-- 我的代充订单 -->
    <van-cell-group class="my-orders-section" title="我的代充订单">
      <van-cell
        title="查看我的订单"
        is-link
        @click="goToMyOrders"
      />
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showDialog } from 'vant'
import { getChargerList, createValetOrder } from '@/api/valet'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 师傅列表
const chargers = ref([])
const selectedCharger = ref(null)

// 订单表单
const orderForm = ref({
  stationId: '',
  pileId: '',
  carPlate: '',
  carLocation: '',
  targetSoc: 80,
  phone: '',
  remark: ''
})

// 预估电费（简化计算）
const estimatedChargeFee = computed(() => {
  const targetSoc = parseInt(orderForm.value.targetSoc) || 0
  // 假设电池容量60kWh，当前电量20%
  const chargeAmount = 60 * (targetSoc - 20) / 100
  const unitPrice = 1.2 // 峰时电价
  return (chargeAmount * unitPrice).toFixed(2)
})

// 总费用
const totalFee = computed(() => {
  const serviceFee = selectedCharger.value?.servicePrice || 0
  return (parseFloat(serviceFee) + parseFloat(estimatedChargeFee.value)).toFixed(2)
})

// 加载师傅列表
const loadChargers = async () => {
  try {
    const res = await getChargerList({ status: 1 }) // 只获取空闲师傅
    if (res.code === 200) {
      chargers.value = res.data || []
    }
  } catch (error) {
    console.error('加载师傅列表失败:', error)
  }
}

// 选择师傅
const selectCharger = (charger) => {
  if (charger.status !== 1) {
    showToast('该师傅暂不可用')
    return
  }
  selectedCharger.value = charger
}

// 创建订单
const createOrder = async () => {
  if (!selectedCharger.value) {
    showToast('请选择代充师傅')
    return
  }

  const toast = showLoadingToast({
    message: '正在下单...',
    forbidClick: true,
    duration: 0
  })

  try {
    const res = await createValetOrder({
      userId: userStore.userId,
      stationId: parseInt(orderForm.value.stationId),
      pileId: parseInt(orderForm.value.pileId) || null,
      carPlate: orderForm.value.carPlate,
      parkingLocation: orderForm.value.carLocation,
      targetSoc: parseInt(orderForm.value.targetSoc),
      chargerId: selectedCharger.value.id
    })

    closeToast()

    if (res.code === 200) {
      await showDialog({
        title: '下单成功',
        message: `订单号：${res.data}\n师傅将尽快接单，请保持电话畅通`
      })
      router.push({ name: 'ValetOrderList' })
    } else {
      showToast(res.message || '下单失败')
    }
  } catch (error) {
    closeToast()
    showToast('下单失败')
  }
}

// 查看我的订单
const goToMyOrders = () => {
  router.push({ name: 'ValetOrderList' })
}

onMounted(() => {
  loadChargers()
})
</script>

<style scoped>
.valet-service {
  padding-bottom: 20px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.charger-section {
  margin-top: 12px;
  margin-bottom: 12px;
}

.charger-list {
  padding: 12px;
}

.charger-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s;
}

.charger-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.charger-card.selected {
  border-color: #1989fa;
  background: #f0f9ff;
}

.charger-info {
  display: flex;
  gap: 12px;
}

.charger-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.charger-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.charger-rating {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating-text {
  font-size: 12px;
  color: #969799;
}

.charger-price {
  font-size: 18px;
  font-weight: bold;
  color: #ee0a24;
}

.order-section {
  margin-bottom: 12px;
}

.fee-estimate {
  margin: 16px 0;
  background: #fff;
  border-radius: 8px;
}

.total-fee {
  font-size: 18px;
  font-weight: bold;
  color: #ee0a24;
}

.my-orders-section {
  margin-bottom: 12px;
}

.submit-button-wrapper {
  margin: 16px;
}
</style>
