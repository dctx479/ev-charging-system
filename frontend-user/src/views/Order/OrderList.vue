<template>
  <div class="order-list">
    <van-nav-bar title="我的订单" />

    <!-- 搜索和筛选 -->
    <div class="search-filter">
      <van-search
        v-model="searchKeyword"
        placeholder="搜索订单号"
        @search="onSearch"
        @clear="onClearSearch"
      >
        <template #action>
          <div @click="showFilterPopup = true">筛选</div>
        </template>
      </van-search>
    </div>

    <!-- 状态筛选 -->
    <van-tabs v-model:active="activeTab" @change="onTabChange" swipeable>
      <van-tab title="全部" name="all" />
      <van-tab title="待支付" name="1" />
      <van-tab title="充电中" name="2" />
      <van-tab title="已完成" name="3" />
      <van-tab title="已取消" name="4" />
      <van-tab title="异常" name="5" />
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
          <van-empty
            :image="getEmptyImage()"
            :description="getEmptyDescription()"
          />
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
              <div class="status-tags">
                <van-tag :type="getStatusType(order.orderStatus)">
                  {{ getStatusText(order.orderStatus) }}
                </van-tag>
                <van-tag
                  v-if="order.paymentStatus !== undefined"
                  :type="getPaymentStatusType(order.paymentStatus)"
                  plain
                >
                  {{ getPaymentStatusText(order.paymentStatus) }}
                </van-tag>
              </div>
            </div>
          </template>

          <template #desc>
            <div class="order-info">
              <div class="info-row">
                <span class="label">订单号：</span>
                <span class="value order-no">{{ order.orderNo || order.id }}</span>
              </div>
              <div class="info-row">
                <span class="label">充电桩：</span>
                <span class="value">{{ order.pileNo }}</span>
              </div>
              <div class="info-row">
                <span class="label">充电时间：</span>
                <span class="value">{{ formatTimeAgo(order.startTime) }}</span>
              </div>
              <div class="info-row">
                <span class="label">充电模式：</span>
                <span class="value">
                  {{ getChargeModeText(order.chargeMode) }}
                </span>
              </div>
              <div class="info-row" v-if="order.priceType">
                <span class="label">电价类型：</span>
                <span class="value">
                  <van-tag :type="getPriceTypeColor(order.priceType)" plain size="small">
                    {{ getPriceTypeText(order.priceType) }}
                  </van-tag>
                </span>
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
                <span class="amount">¥{{ (order.totalFee || 0).toFixed(2) }}</span>
              </div>
              <div class="actions">
                <van-button
                  v-if="order.orderStatus === 2"
                  size="small"
                  type="primary"
                  plain
                  @click.stop="goToChargingProgress(order.id)"
                >
                  查看充电进度
                </van-button>
                <van-button
                  v-if="order.orderStatus === 3 && order.paymentStatus === 0"
                  size="small"
                  type="primary"
                  @click.stop="handlePay(order)"
                >
                  去支付
                </van-button>
                <van-button
                  v-if="order.orderStatus === 2"
                  size="small"
                  type="danger"
                  plain
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

    <!-- 筛选弹出层 -->
    <van-popup v-model:show="showFilterPopup" position="right" :style="{ width: '80%', height: '100%' }">
      <div class="filter-container">
        <div class="filter-header">
          <span>筛选</span>
          <van-icon name="cross" @click="showFilterPopup = false" />
        </div>

        <div class="filter-content">
          <div class="filter-section">
            <div class="section-title">时间范围</div>
            <van-radio-group v-model="timeRange">
              <van-cell-group inset>
                <van-cell title="全部" clickable @click="timeRange = 'all'">
                  <template #right-icon>
                    <van-radio name="all" />
                  </template>
                </van-cell>
                <van-cell title="今天" clickable @click="timeRange = 'today'">
                  <template #right-icon>
                    <van-radio name="today" />
                  </template>
                </van-cell>
                <van-cell title="本周" clickable @click="timeRange = 'week'">
                  <template #right-icon>
                    <van-radio name="week" />
                  </template>
                </van-cell>
                <van-cell title="本月" clickable @click="timeRange = 'month'">
                  <template #right-icon>
                    <van-radio name="month" />
                  </template>
                </van-cell>
              </van-cell-group>
            </van-radio-group>
          </div>
        </div>

        <div class="filter-footer">
          <van-button block plain @click="handleResetFilter">重置</van-button>
          <van-button block type="primary" @click="handleApplyFilter">确定</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 底部导航栏 -->
    <van-tabbar v-model="activeTabBar" route>
      <van-tabbar-item to="/" icon="shop-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getOrderList, cancelOrder } from '@/api/order'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

// 配置dayjs
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

// 列表状态
const activeTab = ref('all')
const activeTabBar = ref(1) // 订单页在导航栏是第2个（索引1）
const orderList = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(0)
const size = ref(10)

// 搜索和筛选
const searchKeyword = ref('')
const showFilterPopup = ref(false)
const timeRange = ref('all')

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

    // 订单状态筛选
    if (activeTab.value !== 'all') {
      params.orderStatus = parseInt(activeTab.value)
    }

    // 订单号搜索
    if (searchKeyword.value.trim()) {
      params.orderNo = searchKeyword.value.trim()
    }

    // 时间范围筛选
    if (timeRange.value !== 'all') {
      const now = dayjs()
      if (timeRange.value === 'today') {
        params.startTime = now.startOf('day').format('YYYY-MM-DD HH:mm:ss')
        params.endTime = now.endOf('day').format('YYYY-MM-DD HH:mm:ss')
      } else if (timeRange.value === 'week') {
        params.startTime = now.startOf('week').format('YYYY-MM-DD HH:mm:ss')
        params.endTime = now.endOf('week').format('YYYY-MM-DD HH:mm:ss')
      } else if (timeRange.value === 'month') {
        params.startTime = now.startOf('month').format('YYYY-MM-DD HH:mm:ss')
        params.endTime = now.endOf('month').format('YYYY-MM-DD HH:mm:ss')
      }
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

const onSearch = () => {
  page.value = 0
  finished.value = false
  orderList.value = []
  loadOrders()
}

const onClearSearch = () => {
  searchKeyword.value = ''
  onSearch()
}

const handleApplyFilter = () => {
  showFilterPopup.value = false
  page.value = 0
  finished.value = false
  orderList.value = []
  loadOrders()
}

const handleResetFilter = () => {
  timeRange.value = 'all'
  handleApplyFilter()
}

const goToDetail = (orderId) => {
  router.push({
    name: 'OrderDetail',
    query: { orderId }
  })
}

const goToChargingProgress = (orderId) => {
  router.push({
    name: 'ChargingProgress',
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

// 订单状态：1-待支付 2-充电中 3-已完成 4-已取消 5-异常 (与后端OrderConstants一致)
const getStatusType = (status) => {
  const types = {
    1: 'warning',   // 待支付
    2: 'primary',   // 充电中
    3: 'success',   // 已完成
    4: 'default',   // 已取消
    5: 'danger'     // 异常
  }
  return types[status] || 'default'
}

const getStatusText = (status) => {
  const texts = {
    1: '待支付',
    2: '充电中',
    3: '已完成',
    4: '已取消',
    5: '异常'
  }
  return texts[status] || '未知'
}

// 支付状态相关
const getPaymentStatusType = (status) => {
  const types = {
    0: 'warning',  // 未支付
    1: 'success',  // 已支付
    2: 'primary',  // 退款中
    3: 'info'      // 已退款
  }
  return types[status] || 'default'
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

// 充电模式
const getChargeModeText = (mode) => {
  const modes = {
    1: '充满自停',
    2: '按金额充电',
    3: '按电量充电',
    4: '按时间充电'
  }
  return modes[mode] || '未知'
}

// 电价类型
const getPriceTypeText = (type) => {
  const types = {
    1: '谷时',
    2: '平时',
    3: '峰时'
  }
  return types[type] || '未知'
}

const getPriceTypeColor = (type) => {
  const colors = {
    1: 'success',
    2: 'primary',
    3: 'danger'
  }
  return colors[type] || 'default'
}

// 时间格式化（相对时间）
const formatTimeAgo = (time) => {
  if (!time) return '-'

  // 处理数组格式: [2025, 12, 14, 10, 30, 0] (Java LocalDateTime序列化)
  let dayjsTime
  if (Array.isArray(time)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = time
    dayjsTime = dayjs(new Date(year, month - 1, day, hour, minute, second))
  } else {
    dayjsTime = dayjs(time)
  }

  if (!dayjsTime.isValid()) return '-'

  const diffHours = dayjs().diff(dayjsTime, 'hour')

  // 24小时内显示相对时间
  if (diffHours < 24) {
    return dayjsTime.fromNow()
  }

  // 超过24小时显示完整时间
  return dayjsTime.format('YYYY-MM-DD HH:mm')
}

// 空状态相关
const getEmptyImage = () => {
  if (activeTab.value === '2') {
    return 'search'
  }
  return 'default'
}

const getEmptyDescription = () => {
  const descriptions = {
    'all': '暂无订单',
    '2': '暂无充电中的订单',
    '3': '暂无已完成的订单',
    '4': '暂无已取消的订单',
    '5': '暂无异常订单'
  }
  return descriptions[activeTab.value] || '暂无订单'
}
</script>

<style scoped lang="scss">
.order-list {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;

  .search-filter {
    background-color: #fff;
    padding: 8px 0;

    :deep(.van-search__action) {
      color: #1989fa;
      font-size: 14px;
      padding: 0 12px;
      cursor: pointer;
    }
  }

  .empty-state {
    padding: 80px 0;
  }

  .order-card {
    margin: 12px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    cursor: pointer;
    transition: all 0.3s ease;

    &:active {
      transform: scale(0.98);
    }

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .station-name {
        font-size: 16px;
        font-weight: bold;
        color: #323233;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-right: 12px;
      }

      .status-tags {
        display: flex;
        gap: 6px;
        align-items: center;
        flex-shrink: 0;
      }
    }

    .order-info {
      .info-row {
        display: flex;
        margin-bottom: 6px;
        font-size: 14px;
        align-items: center;

        .label {
          color: #969799;
          min-width: 80px;
          flex-shrink: 0;
        }

        .value {
          color: #323233;
          flex: 1;
          word-break: break-all;

          &.order-no {
            font-family: 'Courier New', monospace;
            font-size: 13px;
          }
        }
      }
    }

    .order-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #ebedf0;

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
        flex-shrink: 0;
      }
    }
  }

  // 筛选弹出层样式
  .filter-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    background-color: #f7f8fa;

    .filter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background-color: #fff;
      border-bottom: 1px solid #ebedf0;

      span {
        font-size: 16px;
        font-weight: bold;
      }

      .van-icon {
        font-size: 20px;
        color: #969799;
        cursor: pointer;
      }
    }

    .filter-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px 0;

      .filter-section {
        margin-bottom: 16px;

        .section-title {
          font-size: 14px;
          font-weight: bold;
          color: #323233;
          padding: 8px 16px;
          margin-bottom: 8px;
        }

        :deep(.van-cell-group) {
          margin: 0 12px;
        }
      }
    }

    .filter-footer {
      padding: 12px 16px;
      background-color: #fff;
      border-top: 1px solid #ebedf0;
      display: flex;
      gap: 12px;

      .van-button {
        flex: 1;
      }
    }
  }
}
</style>
