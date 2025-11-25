<template>
  <div class="valet-order-list">
    <van-nav-bar
      title="代充订单"
      left-arrow
      @click-left="$router.back()"
    />

    <!-- 订单筛选 -->
    <van-tabs v-model:active="activeStatus" @change="onStatusChange">
      <van-tab title="全部" name="all" />
      <van-tab title="待接单" name="1" />
      <van-tab title="进行中" name="2" />
      <van-tab title="已完成" name="4" />
      <van-tab title="已取消" name="5" />
    </van-tabs>

    <!-- 订单列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadOrders"
      >
        <div
          v-for="order in orders"
          :key="order.orderNo"
          class="order-card"
          @click="goToOrderDetail(order)"
        >
          <!-- 订单头部 -->
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <van-tag :type="getStatusType(order.status)">
              {{ getStatusText(order.status) }}
            </van-tag>
          </div>

          <!-- 师傅信息 -->
          <div class="charger-info" v-if="order.chargerName">
            <van-image
              round
              width="40"
              height="40"
              :src="order.chargerAvatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            />
            <div class="charger-details">
              <div class="charger-name">{{ order.chargerName }}</div>
              <div class="charger-phone">{{ order.chargerPhone }}</div>
            </div>
          </div>

          <!-- 车辆信息 -->
          <van-cell-group class="order-info">
            <van-cell title="车牌号" :value="order.carPlate" />
            <van-cell title="车辆位置" :value="order.carLocation" />
            <van-cell title="目标电量" :value="`${order.targetSoc}%`" />
            <van-cell title="充电站" :value="order.stationName" />
          </van-cell-group>

          <!-- 费用信息 -->
          <div class="order-fee">
            <span>服务费：¥{{ order.servicePrice }}</span>
            <span v-if="order.status === 2">电费：¥{{ order.chargeFee }}</span>
            <span class="total-fee">合计：¥{{ order.totalFee }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="order-actions">
            <van-button
              size="small"
              v-if="order.status === 1"
              @click.stop="cancelOrder(order.orderNo)"
            >
              取消订单
            </van-button>
            <van-button
              size="small"
              type="primary"
              v-if="order.status === 2 || order.status === 3"
              @click.stop="callCharger(order.chargerPhone)"
            >
              联系师傅
            </van-button>
            <van-button
              size="small"
              type="success"
              v-if="order.status === 4 && !order.evaluated"
              @click.stop="evaluateOrder(order.orderNo)"
            >
              评价
            </van-button>
          </div>

          <!-- 订单时间 -->
          <div class="order-time">
            下单时间：{{ order.createTime }}
          </div>
        </div>

        <van-empty v-if="orders.length === 0 && !loading" description="暂无订单" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { getMyValetOrders } from '@/api/valet'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 订单状态
const activeStatus = ref('all')

// 订单列表
const orders = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(0)

// 加载订单
const loadOrders = async () => {
  try {
    const params = {
      userId: userStore.userId,
      page: page.value,
      size: 10
    }

    if (activeStatus.value !== 'all') {
      params.status = parseInt(activeStatus.value)
    }

    const res = await getMyValetOrders(params)

    if (res.code === 200) {
      const newOrders = res.data || []

      if (refreshing.value) {
        orders.value = newOrders
        refreshing.value = false
      } else {
        orders.value = [...orders.value, ...newOrders]
      }

      loading.value = false

      if (newOrders.length < 10) {
        finished.value = true
      } else {
        page.value++
      }
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    loading.value = false
    refreshing.value = false
    finished.value = true
  }
}

// 刷新订单
const onRefresh = () => {
  orders.value = []
  page.value = 0
  finished.value = false
  loadOrders()
}

// 切换状态
const onStatusChange = () => {
  orders.value = []
  page.value = 0
  finished.value = false
  loadOrders()
}

// 获取状态文字（后端状态码：1=待接单 2=已接单 3=充电中 4=已完成 5=已取消）
const getStatusText = (status) => {
  const map = {
    1: '待接单',
    2: '已接单',
    3: '充电中',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    1: 'warning',
    2: 'primary',
    3: 'primary',
    4: 'success',
    5: 'default'
  }
  return map[status] || 'default'
}

// 取消订单
const cancelOrder = async (orderNo) => {
  try {
    await showDialog({
      title: '确认',
      message: '确定要取消订单吗？'
    })
    showToast('取消功能暂未开放，请联系客服处理')
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

// 联系师傅
const callCharger = (phone) => {
  window.location.href = `tel:${phone}`
}

// 评价订单
const evaluateOrder = (orderNo) => {
  showToast('评价功能开发中，敬请期待')
}

// 查看订单详情
const goToOrderDetail = (order) => {
  showToast('订单详情功能开发中，敬请期待')
}

onMounted(() => {
  // 初始加载会由van-list自动触发
})
</script>

<style scoped>
.valet-order-list {
  padding-bottom: 20px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.order-card {
  margin: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebedf0;
}

.order-no {
  font-size: 14px;
  color: #646566;
}

.charger-info {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
}

.charger-details {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.charger-name {
  font-size: 14px;
  font-weight: bold;
  color: #323233;
}

.charger-phone {
  font-size: 12px;
  color: #969799;
}

.order-info {
  margin-bottom: 12px;
}

.order-fee {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #646566;
}

.total-fee {
  font-weight: bold;
  color: #ee0a24;
}

.order-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.order-time {
  font-size: 12px;
  color: #969799;
  text-align: right;
}
</style>
