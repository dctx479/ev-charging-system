<template>
  <div class="charger-dashboard">
    <van-nav-bar
      title="代充工作台"
      left-arrow
      @click-left="onBack"
    />

    <div class="content">
      <!-- 师傅状态 -->
      <van-cell-group title="工作状态" inset>
        <van-cell title="当前状态">
          <template #value>
            <van-switch
              v-model="chargerStatus.isOnline"
              size="20px"
              active-color="#07c160"
              inactive-color="#dcdee0"
              @change="handleStatusChange"
            />
            <span :class="['status-text', chargerStatus.isOnline ? 'online' : 'offline']">
              {{ chargerStatus.isOnline ? '在线接单' : '离线休息' }}
            </span>
          </template>
        </van-cell>

        <van-cell title="今日接单数" :value="`${chargerStatus.todayOrders}单`" />
        <van-cell title="今日收入" :value="`¥${Number(chargerStatus.todayIncome).toFixed(2)}`" />
        <van-cell title="总评分" :value="`${chargerStatus.rating}分`">
          <template #value>
            <van-rate
              v-model="chargerStatus.rating"
              readonly
              size="16px"
              color="#ffd21e"
              void-icon="star"
              void-color="#eee"
            />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 当前订单 -->
      <van-cell-group v-if="currentOrder" title="当前订单" inset style="margin-top: 16px">
        <van-cell title="订单号" :value="currentOrder.orderNo" />
        <van-cell title="车牌号" :value="currentOrder.carPlate" />
        <van-cell title="充电桩" :value="currentOrder.pileNo" />
        <van-cell title="目标电量" :value="`${currentOrder.targetSoc}%`" />
        <van-cell title="预计费用" :value="`¥${currentOrder.estimatedCost}`" />

        <!-- 订单进度 -->
        <van-cell title="充电进度">
          <template #value>
            <van-progress
              :percentage="chargingProgress"
              :show-pivot="true"
              color="#07c160"
              stroke-width="8px"
            />
          </template>
        </van-cell>

        <!-- 完成订单按钮 -->
        <van-cell>
          <van-button
            type="success"
            size="small"
            block
            :loading="completing"
            @click="handleCompleteOrder"
          >
            完成订单
          </van-button>
        </van-cell>
      </van-cell-group>

      <!-- 可接订单列表 -->
      <van-cell-group title="可接订单" inset style="margin-top: 16px">
        <van-tabs v-model:active="activeTab" animated>
          <van-tab title="待接单" name="pending">
            <van-list
              v-model:loading="loading"
              :finished="finished"
              finished-text="没有更多了"
              @load="loadOrders"
            >
              <div
                v-for="order in pendingOrders"
                :key="order.id"
                class="order-card"
              >
                <div class="order-header">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <van-tag type="warning">待接单</van-tag>
                </div>

                <van-cell-group inset>
                  <van-cell title="车牌号" :value="order.carPlate" />
                  <van-cell title="车型" :value="order.vehicleModel" />
                  <van-cell title="充电桩" :value="order.pileNo" />
                  <van-cell title="目标电量" :value="`${order.currentSoc}% → ${order.targetSoc}%`" />
                  <van-cell title="期望时间" :value="formatDate(order.expectedTime)" />
                  <van-cell title="预计费用" :value="`¥${order.estimatedCost}`" />
                  <van-cell v-if="order.remarks" title="备注" :value="order.remarks" />
                </van-cell-group>

                <div class="order-actions">
                  <van-button
                    type="primary"
                    size="small"
                    :loading="accepting === order.id"
                    @click="handleAcceptOrder(order)"
                  >
                    接单
                  </van-button>
                </div>
              </div>

              <van-empty v-if="pendingOrders.length === 0 && !loading" description="暂无可接订单" />
            </van-list>
          </van-tab>

          <van-tab title="进行中" name="processing">
            <van-list>
              <div
                v-for="order in processingOrders"
                :key="order.id"
                class="order-card"
              >
                <div class="order-header">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <van-tag type="success">进行中</van-tag>
                </div>

                <van-cell-group inset>
                  <van-cell title="车牌号" :value="order.carPlate" />
                  <van-cell title="充电桩" :value="order.pileNo" />
                  <van-cell title="当前进度" :value="`${order.progress || 0}%`" />
                  <van-cell title="已用时" :value="formatDuration(order.startTime)" />
                </van-cell-group>
              </div>

              <van-empty v-if="processingOrders.length === 0" description="暂无进行中订单" />
            </van-list>
          </van-tab>

          <van-tab title="已完成" name="completed">
            <van-list>
              <div
                v-for="order in completedOrders"
                :key="order.id"
                class="order-card"
              >
                <div class="order-header">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <van-tag type="default">已完成</van-tag>
                </div>

                <van-cell-group inset>
                  <van-cell title="车牌号" :value="order.carPlate" />
                  <van-cell title="充电量" :value="`${order.actualChargeAmount} kWh`" />
                  <van-cell title="服务费" :value="`¥${order.serviceFee}`" />
                  <van-cell title="完成时间" :value="formatDate(order.completeTime)" />
                  <van-cell v-if="order.rating" title="用户评价">
                    <template #value>
                      <van-rate
                        :model-value="order.rating"
                        readonly
                        size="14px"
                        color="#ffd21e"
                      />
                    </template>
                  </van-cell>
                </van-cell-group>
              </div>

              <van-empty v-if="completedOrders.length === 0" description="暂无已完成订单" />
            </van-list>
          </van-tab>
        </van-tabs>
      </van-cell-group>
    </div>

    <!-- 完成订单对话框 -->
    <van-dialog
      v-model:show="showCompleteDialog"
      title="完成订单"
      show-cancel-button
      @confirm="confirmComplete"
    >
      <van-form style="padding: 16px">
        <van-field
          v-model="completeForm.actualChargeAmount"
          type="number"
          label="实际充电量(kWh)"
          placeholder="请输入实际充电量"
          required
        />
        <van-field
          v-model="completeForm.finalSoc"
          type="number"
          label="最终电量(%)"
          placeholder="请输入最终电量"
          required
        />
        <van-field
          v-model="completeForm.remarks"
          type="textarea"
          label="备注"
          placeholder="请输入备注信息"
          rows="2"
        />
      </van-form>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getChargerOrders, acceptOrder, completeOrder, updateChargerStatus } from '@/api/valet'
import { formatDate } from '@/utils/dateFormat'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

// 师傅状态
const chargerStatus = ref({
  id: null, // 从用户Store动态获取，onMounted时赋值
  isOnline: true,
  todayOrders: 0,
  todayIncome: 0,
  rating: 4.8
})

// 当前订单
const currentOrder = ref(null)
const chargingProgress = ref(0)

// 订单列表
const activeTab = ref('pending')
const pendingOrders = ref([])
const processingOrders = ref([])
const completedOrders = ref([])
const loading = ref(false)
const finished = ref(false)

// 接单状态
const accepting = ref(null)

// 完成订单
const completing = ref(false)
const showCompleteDialog = ref(false)
const completeForm = ref({
  actualChargeAmount: '',
  finalSoc: '',
  remarks: ''
})

// 定时器
let refreshTimer = null

// 返回上一页
const onBack = () => {
  router.back()
}

// 处理状态变更
const handleStatusChange = async (value) => {
  try {
    const res = await updateChargerStatus(chargerStatus.value.id, value ? 1 : 3)

    if (res.code === 200) {
      showToast(value ? '已上线，开始接单' : '已下线')
    } else {
      // 恢复状态
      chargerStatus.value.isOnline = !value
      showToast(res.message || '状态更新失败')
    }
  } catch (error) {
    console.error('Update status error:', error)
    chargerStatus.value.isOnline = !value
    showToast('网络错误')
  }
}

// 加载订单
const loadOrders = async () => {
  if (finished.value || !chargerStatus.value.isOnline) return

  loading.value = true
  try {
    const res = await getChargerOrders(chargerStatus.value.id)
    if (res.code === 200) {
      const orders = res.data || []

      // 分类订单（后端状态码：1=待接单 2=已接单 3=充电中 4=已完成 5=已取消）
      pendingOrders.value = orders.filter(o => o.orderStatus === 1)
      processingOrders.value = orders.filter(o => o.orderStatus === 2 || o.orderStatus === 3)
      completedOrders.value = orders.filter(o => o.orderStatus === 4)

      // 设置当前订单
      currentOrder.value = processingOrders.value[0] || null

      // 更新今日统计
      const today = new Date().toDateString()
      const todayCompleted = completedOrders.value.filter(o =>
        new Date(o.completeTime).toDateString() === today
      )
      chargerStatus.value.todayOrders = todayCompleted.length
      // 确保总收入是数字而非字符串拼接
      chargerStatus.value.todayIncome = todayCompleted.reduce((sum, o) => {
        return sum + (Number(o.serviceFee) || 0)
      }, 0)

      finished.value = true
    }
  } catch (error) {
    console.error('Load orders error:', error)
    showToast('加载订单失败')
  } finally {
    loading.value = false
  }
}

// 接单
const handleAcceptOrder = async (order) => {
  try {
    await showConfirmDialog({
      title: '确认接单',
      message: `确定接受订单 ${order.orderNo} 吗？`,
    })
  } catch {
    return // 用户点击取消
  }

  accepting.value = order.id
  try {
    const res = await acceptOrder(order.orderNo)

    if (res.code === 200) {
      showToast('接单成功')

      // 刷新订单列表
      pendingOrders.value = []
      finished.value = false
      loadOrders()
    } else {
      showToast(res.message || '接单失败')
    }
  } catch (error) {
    console.error('Accept order error:', error)
    showToast('网络错误')
  } finally {
    accepting.value = null
  }
}

// 完成订单
const handleCompleteOrder = () => {
  if (!currentOrder.value) {
    showToast('没有进行中的订单')
    return
  }

  // 预填充数据
  completeForm.value.actualChargeAmount = ''
  completeForm.value.finalSoc = currentOrder.value.targetSoc
  completeForm.value.remarks = ''

  showCompleteDialog.value = true
}

// 确认完成
const confirmComplete = async () => {
  if (!completeForm.value.actualChargeAmount || !completeForm.value.finalSoc) {
    showToast('请填写完整信息')
    return
  }

  completing.value = true
  try {
    const data = {
      pileId: currentOrder.value.pileId || null
    }

    const res = await completeOrder(currentOrder.value.orderNo, data)
    if (res.code === 200) {
      showToast('订单已完成')
      showCompleteDialog.value = false

      // 刷新订单列表
      pendingOrders.value = []
      processingOrders.value = []
      completedOrders.value = []
      finished.value = false
      loadOrders()
    } else {
      showToast(res.message || '完成订单失败')
    }
  } catch (error) {
    console.error('Complete order error:', error)
    showToast('网络错误')
  } finally {
    completing.value = false
  }
}

// 格式化持续时长
const formatDuration = (startTime) => {
  if (!startTime) return '0分钟'
  const start = new Date(startTime)
  const now = new Date()
  const minutes = Math.floor((now - start) / 60000)
  return `${minutes}分钟`
}

// 开始定时刷新
const startRefresh = () => {
  refreshTimer = setInterval(() => {
    if (chargerStatus.value.isOnline) {
      loadOrders()

      // 更新充电进度（模拟）
      if (currentOrder.value) {
        chargingProgress.value = Math.min(100, chargingProgress.value + 5)
      }
    }
  }, 10000) // 每10秒刷新
}

// 停止定时刷新
const stopRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 生命周期
onMounted(async () => {
  // 通过 /valet/charger/my 获取当前用户的代充师傅ID（而非使用userId作为chargerId）
  try {
    const res = await request({ url: '/valet/charger/my', method: 'get' })
    if (res.code === 200 && res.data) {
      chargerStatus.value.id = res.data.id
      chargerStatus.value.rating = res.data.rating || 4.8
      loadOrders()
      startRefresh()
    } else {
      showToast('您尚未注册为代充师傅')
    }
  } catch (error) {
    console.error('Get charger info error:', error)
    showToast('获取代充师傅信息失败')
  }
})

onBeforeUnmount(() => {
  stopRefresh()
})
</script>

<style scoped lang="scss">
.charger-dashboard {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 20px;

  .content {
    padding: 16px;
  }

  .status-text {
    margin-left: 8px;
    font-size: 14px;
    font-weight: 500;

    &.online {
      color: #07c160;
    }

    &.offline {
      color: #969799;
    }
  }

  .order-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 12px;

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .order-no {
        font-size: 14px;
        font-weight: 600;
        color: #323233;
      }
    }

    .order-actions {
      margin-top: 12px;
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  }

  :deep(.van-tabs) {
    margin-top: 0;
  }

  :deep(.van-tab__panel) {
    padding: 16px 0;
  }

  :deep(.van-cell-group) {
    margin-bottom: 0;
  }
}
</style>
