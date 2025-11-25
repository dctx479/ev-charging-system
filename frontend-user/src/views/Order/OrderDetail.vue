<template>
  <div class="order-detail">
    <van-nav-bar
      title="订单详情"
      left-arrow
      @click-left="onBack"
    >
      <template #right>
        <van-icon name="share-o" size="18" @click="handleShare" />
      </template>
    </van-nav-bar>

    <!-- 加载中 -->
    <van-loading v-if="loading" class="loading-container" vertical>加载中...</van-loading>

    <!-- 加载失败 -->
    <van-empty v-else-if="loadError" description="加载失败" @click="loadOrderDetail">
      <van-button round type="primary">重新加载</van-button>
    </van-empty>

    <!-- 订单详情内容 -->
    <div v-else-if="orderInfo.id" class="content">
      <!-- 订单状态卡片 -->
      <div class="status-card">
        <van-icon
          :name="getStatusIcon(orderInfo.orderStatus)"
          :color="getStatusColor(orderInfo.orderStatus)"
          size="48"
        />
        <div class="status-text">{{ getStatusText(orderInfo.orderStatus) }}</div>
        <div class="sub-text">{{ getStatusDescription(orderInfo.orderStatus, orderInfo.paymentStatus) }}</div>
      </div>

      <!-- 充电站信息 -->
      <van-cell-group title="充电站信息" inset>
        <van-cell title="充电站" :value="orderInfo.stationName" icon="location-o" />
        <van-cell title="充电站地址" :value="orderInfo.stationAddress || '-'" :label="orderInfo.stationAddress" />
        <van-cell title="充电桩编号" :value="orderInfo.pileNo" icon="cluster-o" />
        <van-cell title="充电功率" :value="`${orderInfo.pilePower || '-'}kW`" icon="flash" />
      </van-cell-group>

      <!-- 充电详情 -->
      <van-cell-group title="充电详情" inset style="margin-top: 16px">
        <van-cell title="订单号" :value="orderInfo.orderNo" icon="records" />
        <van-cell title="开始时间" :value="formatTime(orderInfo.startTime)" icon="clock-o" />
        <van-cell title="结束时间" :value="orderInfo.orderStatus === 2 ? '充电中...' : formatTime(orderInfo.endTime)" icon="clock-o" />
        <van-cell title="充电时长" :value="orderInfo.orderStatus === 2 ? '充电中...' : `${orderInfo.chargeDuration || 0}分钟`" icon="hourglass-o" />
        <van-cell title="起始电量" :value="`${orderInfo.startSoc || 0}%`" icon="fire-o" />
        <van-cell title="结束电量" :value="orderInfo.orderStatus === 2 ? '充电中...' : `${orderInfo.endSoc || 0}%`" icon="fire" />
        <van-cell title="充电量" icon="certificate">
          <template #value>
            <span class="charge-amount">{{ orderInfo.orderStatus === 2 ? '充电中...' : `${(orderInfo.chargeAmount || 0).toFixed(2)}kWh` }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 费用明细 -->
      <van-cell-group title="费用明细" inset style="margin-top: 16px">
        <van-cell title="电费" :label="getPriceTypeLabel(orderInfo.priceType)" icon="gold-coin-o">
          <template #value>
            <span class="fee-value">¥{{ (orderInfo.electricityFee || 0).toFixed(2) }}</span>
          </template>
        </van-cell>
        <van-cell title="服务费" icon="gold-coin-o">
          <template #value>
            <span class="fee-value">¥{{ (orderInfo.serviceFee || 0).toFixed(2) }}</span>
          </template>
        </van-cell>
        <van-cell title="总费用" icon="balance-list-o">
          <template #value>
            <span class="total-fee">¥{{ (orderInfo.totalFee || 0).toFixed(2) }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 碳积分信息（已支付订单显示） -->
      <van-cell-group v-if="orderInfo.paymentStatus === 1 && orderInfo.orderStatus === 3" title="碳积分奖励" inset style="margin-top: 16px">
        <div class="carbon-reward-card">
          <van-icon name="certificate" size="40" color="#07c160" />
          <div class="reward-info">
            <div class="reward-title">本单获得碳积分</div>
            <div class="reward-value">+{{ orderInfo.carbonCredits || calculateCredits() }} 积分</div>
          </div>
        </div>
        <van-cell title="碳减排量" :value="`${((orderInfo.chargeAmount || 0) * 0.785).toFixed(2)}kg CO₂`" icon="flower-o" />
        <van-cell title="积分可用于" value="兑换优惠券、抵扣充电费用" icon="gift-o" />
      </van-cell-group>

      <!-- 支付信息 -->
      <van-cell-group v-if="orderInfo.paymentStatus !== 0" title="支付信息" inset style="margin-top: 16px">
        <van-cell title="支付状态" :value="getPaymentStatusText(orderInfo.paymentStatus)" icon="success">
          <template #value>
            <van-tag :type="getPaymentStatusTagType(orderInfo.paymentStatus)">
              {{ getPaymentStatusText(orderInfo.paymentStatus) }}
            </van-tag>
          </template>
        </van-cell>
        <van-cell title="支付方式" :value="getPaymentMethodText(orderInfo.paymentMethod)" icon="balance-pay" />
        <van-cell title="支付时间" :value="formatTime(orderInfo.paymentTime)" icon="clock-o" />
        <van-cell v-if="orderInfo.transactionNo" title="交易流水号" :value="orderInfo.transactionNo" icon="card" />
      </van-cell-group>

      <!-- 操作按钮 -->
      <div class="button-group">
        <!-- 已完成未支付订单 -->
        <van-button
          v-if="orderInfo.orderStatus === 3 && orderInfo.paymentStatus === 0"
          type="primary"
          size="large"
          block
          :loading="paying"
          @click="handlePay"
        >
          立即支付 ¥{{ (orderInfo.totalFee || 0).toFixed(2) }}
        </van-button>

        <!-- 进行中订单 -->
        <van-button
          v-if="orderInfo.orderStatus === 2"
          type="primary"
          size="large"
          block
          @click="goToProgress"
        >
          查看充电进度
        </van-button>

        <!-- 已完成已支付订单 -->
        <van-button
          v-if="orderInfo.orderStatus === 3 && orderInfo.paymentStatus === 1"
          type="success"
          size="large"
          block
          @click="handleRecharge"
        >
          再来一单
        </van-button>
      </div>
    </div>

    <!-- 支付方式选择 -->
    <van-action-sheet
      v-model:show="showPaymentSheet"
      title="选择支付方式"
      :actions="paymentActions"
      cancel-text="取消"
      close-on-click-action
      @select="onPaymentSelect"
    />

    <!-- 支付确认对话框 -->
    <van-dialog
      v-model:show="showPaymentConfirm"
      title="确认支付"
      show-cancel-button
      :before-close="beforePaymentConfirm"
    >
      <div class="payment-confirm-content">
        <div class="payment-method">
          <van-icon :name="selectedPaymentMethod.icon" size="32" />
          <span>{{ selectedPaymentMethod.name }}</span>
        </div>
        <div class="payment-amount">
          <span class="label">支付金额</span>
          <span class="amount">¥{{ (orderInfo.totalFee || 0).toFixed(2) }}</span>
        </div>
      </div>
    </van-dialog>

    <!-- 支付成功动画 -->
    <van-overlay :show="showSuccessOverlay" @click="showSuccessOverlay = false">
      <div class="success-wrapper">
        <div class="success-content">
          <van-icon name="checked" color="#07c160" size="64" />
          <div class="success-text">支付成功</div>
          <div class="success-tip">碳积分已到账</div>
        </div>
      </div>
    </van-overlay>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { getOrderDetail, payOrder } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const orderId = ref(route.query.orderId)
const orderInfo = ref({})
const loading = ref(false)
const loadError = ref(false)
const paying = ref(false)
const showPaymentSheet = ref(false)
const showPaymentConfirm = ref(false)
const showSuccessOverlay = ref(false)
const selectedPaymentMethod = ref({})

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
  loading.value = true
  loadError.value = false
  try {
    const res = await getOrderDetail(orderId.value)
    orderInfo.value = res.data
  } catch (error) {
    loadError.value = true
    showToast('加载订单详情失败')
  } finally {
    loading.value = false
  }
}

const handlePay = () => {
  showPaymentSheet.value = true
}

const onPaymentSelect = (action) => {
  selectedPaymentMethod.value = action
  showPaymentSheet.value = false
  showPaymentConfirm.value = true
}

const beforePaymentConfirm = (action) => {
  return new Promise((resolve) => {
    if (action === 'confirm') {
      handleConfirmPay()
      resolve(false) // 阻止对话框关闭
    } else {
      resolve(true) // 允许关闭
    }
  })
}

const handleConfirmPay = async () => {
  paying.value = true
  try {
    // 调用支付接口（不要提前关闭对话框）
    await payOrder(orderId.value, {
      paymentMethod: selectedPaymentMethod.value.value
    })

    // 支付成功后，先关闭确认对话框
    showPaymentConfirm.value = false

    // 重新加载订单详情
    await loadOrderDetail()

    // 显示支付成功动画
    showSuccessOverlay.value = true

    // 2秒后关闭动画
    setTimeout(() => {
      showSuccessOverlay.value = false
      showToast('支付成功')
    }, 2000)
  } catch (error) {
    // 支付失败时关闭对话框并显示错误
    showPaymentConfirm.value = false
    showToast(error.response?.data?.message || error.message || '支付失败')
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

const handleRecharge = () => {
  // 跳转到创建订单页面，传入充电桩ID
  router.push({
    name: 'CreateOrder',
    query: { pileId: orderInfo.value.pileId }
  })
}

const handleShare = () => {
  const shareText = `我在${orderInfo.value.stationName}充电啦！
充电桩：${orderInfo.value.pileNo}
充电量：${(orderInfo.value.chargeAmount || 0).toFixed(2)}kWh
获得碳积分：${orderInfo.value.carbonCredits || 0}分
订单号：${orderInfo.value.orderNo}`

  // 复制到剪贴板
  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareText).then(() => {
      showToast('订单信息已复制到剪贴板')
    }).catch(() => {
      showToast('复制失败，请手动复制')
    })
  } else {
    // 兼容方案
    const textarea = document.createElement('textarea')
    textarea.value = shareText
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      showToast('订单信息已复制到剪贴板')
    } catch (err) {
      showToast('复制失败，请手动复制')
    }
    document.body.removeChild(textarea)
  }
}

// 订单状态：1-待支付 2-充电中 3-已完成 4-已取消 5-异常 (与后端OrderConstants一致)
const getStatusIcon = (status) => {
  const icons = {
    1: 'balance-list-o', // 待支付
    2: 'clock-o',       // 充电中
    3: 'checked',       // 已完成
    4: 'close',         // 已取消
    5: 'warning-o'      // 异常
  }
  return icons[status] || 'question-o'
}

const getStatusColor = (status) => {
  const colors = {
    1: '#ff9c00',       // 待支付（橙色）
    2: '#1989fa',       // 充电中（蓝色）
    3: '#07c160',       // 已完成（绿色）
    4: '#969799',       // 已取消（灰色）
    5: '#ee0a24'        // 异常（红色）
  }
  return colors[status] || '#969799'
}

const getStatusText = (status) => {
  const texts = {
    1: '待支付',
    2: '充电进行中',
    3: '充电已完成',
    4: '订单已取消',
    5: '充电异常'
  }
  return texts[status] || '未知状态'
}

const getStatusDescription = (orderStatus, paymentStatus) => {
  if (orderStatus === 2) {
    return '正在为您充电，请耐心等待'
  } else if (orderStatus === 3 && paymentStatus === 0) {
    return '充电已完成，请尽快完成支付'
  } else if (orderStatus === 3 && paymentStatus === 1) {
    return '感谢使用，欢迎再次光临'
  } else if (orderStatus === 4) {
    return '订单已被取消'
  } else if (orderStatus === 5) {
    return '充电过程中发生异常，请联系客服'
  }
  return ''
}

const getPriceTypeLabel = (priceType) => {
  const labels = {
    1: '谷时电价 0.4元/kWh',
    2: '平时电价 0.8元/kWh',
    3: '峰时电价 1.2元/kWh'
  }
  return labels[priceType] || ''
}

const getPaymentStatusText = (status) => {
  const texts = {
    0: '未支付',
    1: '已支付',
    2: '退款中',
    3: '已退款'
  }
  return texts[status] || '未知'
}

const getPaymentStatusTagType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'warning',
    3: 'info'
  }
  return types[status] || 'default'
}

const getPaymentMethodText = (method) => {
  const texts = {
    1: '微信支付',
    2: '支付宝',
    3: '余额支付'
  }
  return texts[method] || '-'
}

// 计算碳积分（备用方法，当后端未返回时使用）
const calculateCredits = () => {
  // 碳积分 = 充电量(kWh) × 碳排放因子(0.785kg/kWh) × 10
  return Math.round((orderInfo.value.chargeAmount || 0) * 0.785 * 10)
}

const formatTime = (time) => {
  if (!time) return '-'

  // 处理数组格式: [2025, 12, 14, 10, 30, 0] (Java LocalDateTime序列化)
  if (Array.isArray(time)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = time
    return dayjs(new Date(year, month - 1, day, hour, minute, second)).format('YYYY-MM-DD HH:mm:ss')
  }

  // 处理字符串格式
  const parsed = dayjs(time)
  if (parsed.isValid()) {
    return parsed.format('YYYY-MM-DD HH:mm:ss')
  }

  return '-'
}

const onBack = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.order-detail {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 20px;

  .loading-container {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
  }

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
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);

    .van-icon {
      background: rgba(255, 255, 255, 0.2);
      padding: 16px;
      border-radius: 50%;
    }

    .status-text {
      margin-top: 16px;
      font-size: 22px;
      font-weight: bold;
      color: #fff;
    }

    .sub-text {
      margin-top: 8px;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
    }
  }

  .charge-amount {
    font-size: 16px;
    font-weight: bold;
    color: #07c160;
  }

  .fee-value {
    font-size: 15px;
    color: #323233;
  }

  .total-fee {
    font-size: 20px;
    font-weight: bold;
    color: #ee0a24;
  }

  .carbon-credits {
    font-size: 16px;
    font-weight: bold;
    color: #07c160;
  }

  .carbon-reward-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 16px;
    background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
    margin: 8px;
    border-radius: 8px;

    .reward-info {
      flex: 1;

      .reward-title {
        font-size: 14px;
        color: #666;
        margin-bottom: 4px;
      }

      .reward-value {
        font-size: 24px;
        font-weight: bold;
        color: #07c160;
      }
    }
  }

  .button-group {
    padding: 16px;
    margin-top: 16px;

    .van-button {
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

// 支付确认对话框样式
.payment-confirm-content {
  padding: 24px 16px;

  .payment-method {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebedf0;

    span {
      font-size: 16px;
      font-weight: bold;
      color: #323233;
    }
  }

  .payment-amount {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;

    .label {
      font-size: 15px;
      color: #646566;
    }

    .amount {
      font-size: 24px;
      font-weight: bold;
      color: #ee0a24;
    }
  }
}

// 支付成功动画样式
.success-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;

  .success-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 280px;
    padding: 40px 20px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);

    .van-icon {
      animation: scaleIn 0.3s ease-out;
    }

    .success-text {
      margin-top: 20px;
      font-size: 20px;
      font-weight: bold;
      color: #323233;
    }

    .success-tip {
      margin-top: 8px;
      font-size: 14px;
      color: #969799;
    }
  }
}

@keyframes scaleIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

// Vant组件样式覆盖
:deep(.van-cell__title) {
  font-size: 15px;
}

:deep(.van-cell__value) {
  font-size: 14px;
}

:deep(.van-cell__label) {
  margin-top: 4px;
  font-size: 12px;
}

:deep(.van-cell-group__title) {
  padding-left: 16px;
  font-weight: 600;
  color: #323233;
}
</style>
