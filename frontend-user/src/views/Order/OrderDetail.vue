<template>
  <div class="order-detail">
    <van-nav-bar
      title="订单详情"
      left-arrow
      @click-left="onBack"
    />

    <div v-if="orderInfo.id" class="content">
      <!-- 订单状态 -->
      <div class="status-card">
        <van-icon
          :name="getStatusIcon(orderInfo.orderStatus)"
          :color="getStatusColor(orderInfo.orderStatus)"
          size="48"
        />
        <div class="status-text">{{ getStatusText(orderInfo.orderStatus) }}</div>
        <div v-if="orderInfo.orderStatus === 1 && orderInfo.paymentStatus === 0" class="sub-text">
          请尽快完成支付
        </div>
      </div>

      <!-- 充电站信息 -->
      <van-cell-group title="充电站信息" inset>
        <van-cell title="充电站" :value="orderInfo.stationName" />
        <van-cell title="充电桩编号" :value="orderInfo.pileNo" />
        <van-cell title="充电功率" :value="`${orderInfo.pilePower}kW`" />
      </van-cell-group>

      <!-- 充电详情 -->
      <van-cell-group title="充电详情" inset style="margin-top: 16px">
        <van-cell title="订单号" :value="orderInfo.orderNo" />
        <van-cell title="开始时间" :value="formatTime(orderInfo.startTime)" />
        <van-cell title="结束时间" :value="formatTime(orderInfo.endTime)" />
        <van-cell title="充电时长" :value="`${orderInfo.chargeDuration || 0}分钟`" />
        <van-cell title="起始电量" :value="`${orderInfo.startSoc}%`" />
        <van-cell title="结束电量" :value="`${orderInfo.endSoc || '-'}%`" />
        <van-cell title="充电量" :value="`${orderInfo.chargeAmount || 0}kWh`" />
      </van-cell-group>

      <!-- 费用明细 -->
      <van-cell-group title="费用明细" inset style="margin-top: 16px">
        <van-cell title="电费" :value="`¥${orderInfo.electricityFee || 0}`" />
        <van-cell title="服务费" :value="`¥${orderInfo.serviceFee || 0}`" />
        <van-cell title="总费用" :value="`¥${orderInfo.totalFee || 0}`">
          <template #value>
            <span class="total-fee">¥{{ orderInfo.totalFee || 0 }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 支付信息 -->
      <van-cell-group v-if="orderInfo.paymentStatus !== 0" title="支付信息" inset style="margin-top: 16px">
        <van-cell title="支付状态" :value="getPaymentStatusText(orderInfo.paymentStatus)" />
        <van-cell title="支付方式" :value="getPaymentMethodText(orderInfo.paymentMethod)" />
        <van-cell title="支付时间" :value="formatTime(orderInfo.paymentTime)" />
      </van-cell-group>

      <!-- 操作按钮 -->
      <div class="button-group">
        <van-button
          v-if="orderInfo.orderStatus === 1 && orderInfo.paymentStatus === 0"
          type="primary"
          size="large"
          :loading="paying"
          @click="handlePay"
        >
          立即支付
        </van-button>
        <van-button
          v-if="orderInfo.orderStatus === 0"
          type="danger"
          size="large"
          @click="goToProgress"
        >
          查看充电进度
        </van-button>
      </div>
    </div>

    <!-- 支付方式选择 -->
    <van-action-sheet
      v-model:show="showPaymentSheet"
      title="选择支付方式"
      :actions="paymentActions"
      @select="onPaymentSelect"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getOrderDetail, payOrder } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const orderId = ref(route.query.orderId)
const orderInfo = ref({})
const paying = ref(false)
const showPaymentSheet = ref(false)

const paymentActions = [
  { name: '微信支付', value: 1, icon: 'wechat' },
  { name: '支付宝', value: 2, icon: 'alipay' },
  { name: '余额支付', value: 3, icon: 'balance-o' }
]

onMounted(async () => {
  await loadOrderDetail()

  // 如果是从列表点击支付进来的，自动弹出支付选择
  if (route.query.action === 'pay') {
    showPaymentSheet.value = true
  }
})

const loadOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    orderInfo.value = res.data
  } catch (error) {
    showToast('加载订单详情失败')
  }
}

const handlePay = () => {
  showPaymentSheet.value = true
}

const onPaymentSelect = async (action) => {
  paying.value = true
  try {
    await payOrder(orderId.value, {
      paymentMethod: action.value
    })

    showToast('支付成功')
    showPaymentSheet.value = false

    // 重新加载订单详情
    await loadOrderDetail()
  } catch (error) {
    showToast(error.message || '支付失败')
  } finally {
    paying.value = false
  }
}

const goToProgress = () => {
  router.push({
    name: 'ChargingProgress',
    query: { orderId: orderId.value }
  })
}

const getStatusIcon = (status) => {
  const icons = {
    0: 'clock-o',
    1: 'checked',
    2: 'close',
    3: 'warning-o'
  }
  return icons[status] || 'question-o'
}

const getStatusColor = (status) => {
  const colors = {
    0: '#1989fa',
    1: '#07c160',
    2: '#969799',
    3: '#ee0a24'
  }
  return colors[status] || '#969799'
}

const getStatusText = (status) => {
  const texts = {
    0: '充电进行中',
    1: '充电已完成',
    2: '订单已取消',
    3: '充电异常'
  }
  return texts[status] || '未知状态'
}

const getPaymentStatusText = (status) => {
  const texts = {
    0: '未支付',
    1: '已支付',
    2: '已退款'
  }
  return texts[status] || '未知'
}

const getPaymentMethodText = (method) => {
  const texts = {
    1: '微信支付',
    2: '支付宝',
    3: '余额支付'
  }
  return texts[method] || '-'
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const onBack = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.order-detail {
  min-height: 100vh;
  background-color: #f7f8fa;

  .content {
    padding: 16px 0;
  }

  .status-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    margin: 16px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

    .status-text {
      margin-top: 16px;
      font-size: 20px;
      font-weight: bold;
      color: #323233;
    }

    .sub-text {
      margin-top: 8px;
      font-size: 14px;
      color: #969799;
    }
  }

  .total-fee {
    font-size: 18px;
    font-weight: bold;
    color: #ee0a24;
  }

  .button-group {
    padding: 16px;

    .van-button {
      margin-bottom: 12px;
    }
  }
}
</style>
