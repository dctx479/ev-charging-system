<template>
  <div class="order-list">
    <van-nav-bar title="我的订单" />

    <!-- 状态筛选 -->
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="全部" name="all" />
      <van-tab title="进行中" name="0" />
      <van-tab title="已完成" name="1" />
      <van-tab title="已取消" name="2" />
    </van-tabs>

    <!-- 订单列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-if="orderList.length === 0 && !loading" class="empty-state">
          <van-empty description="暂无订单" />
        </div>

        <van-card
          v-for="order in orderList"
          :key="order.id"
          class="order-card"
          @click="goToDetail(order.id)"
        >
          <template #title>
            <div class="order-header">
              <span class="station-name">{{ order.stationName }}</span>
              <van-tag :type="getStatusType(order.orderStatus)">
                {{ getStatusText(order.orderStatus) }}
              </van-tag>
            </div>
          </template>

          <template #desc>
            <div class="order-info">
              <div class="info-row">
                <span class="label">充电桩：</span>
                <span class="value">{{ order.pileNo }}</span>
              </div>
              <div class="info-row">
                <span class="label">充电时间：</span>
                <span class="value">{{ formatTime(order.startTime) }}</span>
              </div>
              <div class="info-row">
                <span class="label">充电量：</span>
                <span class="value">{{ order.chargeAmount || 0 }}kWh</span>
              </div>
              <div class="info-row">
                <span class="label">充电时长：</span>
                <span class="value">{{ order.chargeDuration || 0 }}分钟</span>
              </div>
            </div>
          </template>

          <template #footer>
            <div class="order-footer">
              <div class="fee">
                <span class="label">费用：</span>
                <span class="amount">¥{{ order.totalFee || 0 }}</span>
              </div>
              <div class="actions">
                <van-button
                  v-if="order.orderStatus === 1 && order.paymentStatus === 0"
                  size="small"
                  type="primary"
                  @click.stop="handlePay(order)"
                >
                  去支付
                </van-button>
                <van-button
                  v-if="order.orderStatus === 0"
                  size="small"
                  type="danger"
                  @click.stop="handleCancel(order.id)"
                >
                  取消订单
                </van-button>
              </div>
            </div>
          </template>
        </van-card>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getOrderList, cancelOrder } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()

const activeTab = ref('all')
const orderList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(0)
const size = ref(10)

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value
    }

    if (activeTab.value !== 'all') {
      params.orderStatus = parseInt(activeTab.value)
    }

    const res = await getOrderList(params)
    const newOrders = res.data.content || []

    if (page.value === 0) {
      orderList.value = newOrders
    } else {
      orderList.value.push(...newOrders)
    }

    // 判断是否还有更多数据
    finished.value = res.data.last || newOrders.length === 0
  } catch (error) {
    showToast('加载订单失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onLoad = () => {
  if (!finished.value) {
    page.value++
    loadOrders()
  }
}

const onRefresh = () => {
  page.value = 0
  finished.value = false
  loadOrders()
}

const onTabChange = () => {
  page.value = 0
  finished.value = false
  orderList.value = []
  loadOrders()
}

const goToDetail = (orderId) => {
  router.push({
    name: 'OrderDetail',
    query: { orderId }
  })
}

const handlePay = (order) => {
  router.push({
    name: 'OrderDetail',
    query: { orderId: order.id, action: 'pay' }
  })
}

const handleCancel = async (orderId) => {
  try {
    await showConfirmDialog({
      title: '确认取消',
      message: '确定要取消此订单吗？'
    })

    await cancelOrder(orderId)
    showToast('订单已取消')

    // 重新加载列表
    onRefresh()
  } catch (error) {
    if (error !== 'cancel') {
      showToast(error.message || '取消订单失败')
    }
  }
}

const getStatusType = (status) => {
  const types = {
    0: 'primary',
    1: 'success',
    2: 'default',
    3: 'danger'
  }
  return types[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    0: '进行中',
    1: '已完成',
    2: '已取消',
    3: '异常'
  }
  return texts[status] || '未知'
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.order-list {
  min-height: 100vh;
  background-color: #f7f8fa;

  .empty-state {
    padding: 80px 0;
  }

  .order-card {
    margin: 12px;
    border-radius: 8px;
    overflow: hidden;

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .station-name {
        font-size: 16px;
        font-weight: bold;
        color: #323233;
      }
    }

    .order-info {
      .info-row {
        display: flex;
        margin-bottom: 6px;
        font-size: 14px;

        .label {
          color: #969799;
          min-width: 70px;
        }

        .value {
          color: #323233;
          flex: 1;
        }
      }
    }

    .order-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;

      .fee {
        .label {
          font-size: 14px;
          color: #969799;
        }

        .amount {
          font-size: 18px;
          font-weight: bold;
          color: #ee0a24;
          margin-left: 4px;
        }
      }

      .actions {
        display: flex;
        gap: 8px;
      }
    }
  }
}
</style>
