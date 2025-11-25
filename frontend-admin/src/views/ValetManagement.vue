<template>
  <div class="valet-management">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :span="6">
        <el-card>
          <el-statistic title="代充师傅总数" :value="statistics.chargerCount || 0" suffix="人" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="今日订单" :value="statistics.todayOrders || 0" suffix="单" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="今日收入" :value="statistics.todayIncome || 0" prefix="¥" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="服务满意度" :value="statistics.satisfaction || 0" suffix="%" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 师傅管理 -->
      <el-tab-pane label="师傅管理" name="chargers">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>代充师傅列表</span>
            </div>
          </template>

          <!-- 筛选器 -->
          <el-form :inline="true" :model="chargerQuery" class="query-form">
            <el-form-item label="姓名">
              <el-input v-model="chargerQuery.name" placeholder="请输入姓名" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="chargerQuery.status" placeholder="请选择状态" clearable>
                <el-option label="全部" :value="null" />
                <el-option label="空闲" :value="1" />
                <el-option label="忙碌" :value="2" />
                <el-option label="离线" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="审核状态">
              <el-select v-model="chargerQuery.auditStatus" placeholder="请选择" clearable>
                <el-option label="全部" :value="null" />
                <el-option label="待审核" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已拒绝" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="() => { fetchChargers(); applyChargerFilter() }">查询</el-button>
              <el-button @click="resetChargerQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 师傅表格 -->
          <el-table :data="chargers" border stripe v-loading="chargerLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="idCard" label="身份证号" width="180" />
            <el-table-column label="评分" width="120">
              <template #default="{ row }">
                <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="完成订单" width="100" />
            <el-table-column label="服务费" width="100">
              <template #default="{ row }">
                ¥{{ row.servicePrice }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getChargerStatusType(row.status)">
                  {{ getChargerStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAuditStatusType(row.isCertified)">
                  {{ getAuditStatusText(row.isCertified) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="success"
                  size="small"
                  v-if="row.isCertified === 0"
                  @click="handleAudit(row, 1)"
                >
                  通过
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  v-if="row.isCertified === 0"
                  @click="handleAudit(row, 2)"
                >
                  拒绝
                </el-button>
                <el-button type="primary" size="small" @click="handleViewCharger(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="chargerPagination.page"
            v-model:page-size="chargerPagination.size"
            :total="filteredChargers.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="() => { chargerPagination.page = 1 }"
            @current-change="() => {}"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-card>
      </el-tab-pane>

      <!-- 订单管理 -->
      <el-tab-pane label="订单管理" name="orders">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>代充订单列表</span>
              <el-button type="primary" @click="handleExportOrders">导出数据</el-button>
            </div>
          </template>

          <!-- 筛选器 -->
          <el-form :inline="true" :model="orderQuery" class="query-form">
            <el-form-item label="订单号">
              <el-input v-model="orderQuery.orderNo" placeholder="请输入订单号" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="orderQuery.status" placeholder="请选择状态" clearable>
                <el-option label="全部" :value="null" />
                <el-option label="待接单" :value="1" />
                <el-option label="已接单" :value="2" />
                <el-option label="充电中" :value="3" />
                <el-option label="已完成" :value="4" />
                <el-option label="已取消" :value="5" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="orderQuery.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="() => { fetchOrders(); applyOrderFilter() }">查询</el-button>
              <el-button @click="resetOrderQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 订单表格 -->
          <el-table :data="orders" border stripe v-loading="orderLoading">
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="userName" label="用户" width="120" />
            <el-table-column prop="chargerName" label="师傅" width="120" />
            <el-table-column prop="carPlate" label="车牌号" width="120" />
            <el-table-column prop="stationName" label="充电站" width="150" />
            <el-table-column prop="pileNo" label="充电桩" width="120" />
            <el-table-column label="目标电量" width="100">
              <template #default="{ row }">
                {{ row.targetSoc }}%
              </template>
            </el-table-column>
            <el-table-column label="费用" width="120">
              <template #default="{ row }">
                <div>服务费: ¥{{ row.servicePrice }}</div>
                <div v-if="row.status === 2">电费: ¥{{ row.chargeFee }}</div>
                <div style="color: #f56c6c; font-weight: bold;">
                  合计: ¥{{ row.totalFee }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusType(row.status)">
                  {{ getOrderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleViewOrder(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="orderPagination.page"
            v-model:page-size="orderPagination.size"
            :total="filteredOrders.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="() => { orderPagination.page = 1 }"
            @current-change="() => {}"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 师傅详情对话框 -->
    <el-dialog v-model="chargerDetailVisible" title="师傅详情" width="600px">
      <el-descriptions :column="2" border v-if="currentCharger">
        <el-descriptions-item label="姓名">{{ currentCharger.name }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ currentCharger.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentCharger.idCard }}</el-descriptions-item>
        <el-descriptions-item label="评分">
          <el-rate v-model="currentCharger.rating" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="服务费">¥{{ currentCharger.servicePrice }}</el-descriptions-item>
        <el-descriptions-item label="完成订单">{{ currentCharger.orderCount }}单</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getChargerStatusType(currentCharger.status)">
            {{ getChargerStatusText(currentCharger.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="getAuditStatusType(currentCharger.isCertified)">
            {{ getAuditStatusText(currentCharger.isCertified) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">
          {{ currentCharger.createTime }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="orderDetailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentOrder.userName }}</el-descriptions-item>
        <el-descriptions-item label="师傅">{{ currentOrder.chargerName }}</el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ currentOrder.carPlate }}</el-descriptions-item>
        <el-descriptions-item label="车辆位置">{{ currentOrder.carLocation }}</el-descriptions-item>
        <el-descriptions-item label="充电站">{{ currentOrder.stationName }}</el-descriptions-item>
        <el-descriptions-item label="充电桩">{{ currentOrder.pileNo }}</el-descriptions-item>
        <el-descriptions-item label="目标电量">{{ currentOrder.targetSoc }}%</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentOrder.phone }}</el-descriptions-item>
        <el-descriptions-item label="服务费">¥{{ currentOrder.servicePrice }}</el-descriptions-item>
        <el-descriptions-item label="电费" v-if="currentOrder.status === 2">
          ¥{{ currentOrder.chargeFee }}
        </el-descriptions-item>
        <el-descriptions-item label="总费用">
          <span style="color: #f56c6c; font-weight: bold;">¥{{ currentOrder.totalFee }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getOrderStatusType(currentOrder.status)">
            {{ getOrderStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">
          {{ currentOrder.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2" v-if="currentOrder.remark">
          {{ currentOrder.remark }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChargerList,
  auditCharger,
  getValetOrderList,
  getValetStatistics
} from '@/api/valet'

// Tab切换
const activeTab = ref('chargers')

// 统计数据
const statistics = ref({})

// 师傅管理
const chargerQuery = ref({
  name: '',
  status: null,
  auditStatus: null
})
// 全量原始数据（从后端一次性拉取）
const allChargers = ref([])
const chargerLoading = ref(false)
const chargerPagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 本地过滤：先按 name/status/auditStatus(isCertified) 过滤
const filteredChargers = computed(() => {
  let list = allChargers.value
  const { name, status, auditStatus } = chargerQuery.value
  if (name) {
    list = list.filter(item => item.name && item.name.includes(name))
  }
  if (status !== null && status !== undefined) {
    list = list.filter(item => item.status === status)
  }
  if (auditStatus !== null && auditStatus !== undefined) {
    list = list.filter(item => item.isCertified === auditStatus)
  }
  return list
})

// 本地分页：对过滤结果切片
const chargers = computed(() => {
  const start = (chargerPagination.value.page - 1) * chargerPagination.value.size
  const end = start + chargerPagination.value.size
  return filteredChargers.value.slice(start, end)
})

// 订单管理
const orderQuery = ref({
  orderNo: '',
  status: null,
  dateRange: []
})
// 全量原始数据（从后端一次性拉取）
const allOrders = ref([])
const orderLoading = ref(false)
const orderPagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 本地过滤：按 orderNo/status 过滤
const filteredOrders = computed(() => {
  let list = allOrders.value
  const { orderNo, status } = orderQuery.value
  if (orderNo) {
    list = list.filter(item => item.orderNo && item.orderNo.includes(orderNo))
  }
  if (status !== null && status !== undefined) {
    list = list.filter(item => item.status === status)
  }
  return list
})

// 本地分页：对过滤结果切片
const orders = computed(() => {
  const start = (orderPagination.value.page - 1) * orderPagination.value.size
  const end = start + orderPagination.value.size
  return filteredOrders.value.slice(start, end)
})

// 详情对话框
const chargerDetailVisible = ref(false)
const currentCharger = ref(null)
const orderDetailVisible = ref(false)
const currentOrder = ref(null)

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getValetStatistics()
    if (res.code === 200) {
      statistics.value = res.data || {}
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取师傅列表（拉取全量，本地过滤+分页）
const fetchChargers = async () => {
  chargerLoading.value = true

  try {
    const params = {
      serviceArea: undefined,
      status: chargerQuery.value.status !== null ? chargerQuery.value.status : undefined
    }

    const res = await getChargerList(params)

    if (res.code === 200) {
      allChargers.value = res.data || []
    }
  } catch (error) {
    console.error('获取师傅列表失败:', error)
    ElMessage.error('获取师傅列表失败')
  } finally {
    chargerLoading.value = false
  }
}

// 应用查询筛选（重置到第1页后由 computed 自动刷新视图）
const applyChargerFilter = () => {
  chargerPagination.value.page = 1
}

// 重置师傅查询
const resetChargerQuery = () => {
  chargerQuery.value = {
    name: '',
    status: null,
    auditStatus: null
  }
  chargerPagination.value.page = 1
  fetchChargers()
}

// 审核师傅
const handleAudit = async (row, auditStatus) => {
  try {
    const action = auditStatus === 1 ? '通过' : '拒绝'
    await ElMessageBox.confirm(`确定要${action}该师傅的申请吗？`, '确认', {
      type: 'warning'
    })

    const res = await auditCharger(row.id, auditStatus)

    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchChargers()
      fetchStatistics()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${auditStatus === 1 ? '通过' : '拒绝'}失败`)
    }
  }
}

// 查看师傅详情
const handleViewCharger = (row) => {
  currentCharger.value = row
  chargerDetailVisible.value = true
}

// 获取订单列表（拉取全量，本地过滤+分页）
const fetchOrders = async () => {
  orderLoading.value = true

  try {
    const params = {}

    const res = await getValetOrderList(params)

    if (res.code === 200) {
      allOrders.value = res.data || []
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    orderLoading.value = false
  }
}

// 应用订单筛选（重置到第1页后由 computed 自动刷新视图）
const applyOrderFilter = () => {
  orderPagination.value.page = 1
}

// 重置订单查询
const resetOrderQuery = () => {
  orderQuery.value = {
    orderNo: '',
    status: null,
    dateRange: []
  }
  orderPagination.value.page = 1
  fetchOrders()
}

// 查看订单详情
const handleViewOrder = (row) => {
  currentOrder.value = row
  orderDetailVisible.value = true
}

// 导出订单
const handleExportOrders = () => {
  ElMessage.info('导出功能开发中...')
}

// Tab切换
const handleTabChange = (tabName) => {
  if (tabName === 'chargers') {
    fetchChargers()
  } else if (tabName === 'orders') {
    fetchOrders()
  }
}

// 获取师傅状态文字
const getChargerStatusText = (status) => {
  const map = { 1: '空闲', 2: '忙碌', 3: '离线' }
  return map[status] || '未知'
}

// 获取师傅状态类型
const getChargerStatusType = (status) => {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[status] || 'info'
}

// 获取审核状态文字
const getAuditStatusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

// 获取审核状态类型
const getAuditStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

// 获取订单状态文字
const getOrderStatusText = (status) => {
  const map = { 1: '待接单', 2: '已接单', 3: '充电中', 4: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  const map = { 1: 'warning', 2: 'primary', 3: 'primary', 4: 'success', 5: 'info' }
  return map[status] || 'info'
}

onMounted(() => {
  fetchStatistics()
  fetchChargers()
})
</script>

<style scoped>
.valet-management {
  padding: 20px;
}

.statistics-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  margin-bottom: 20px;
}
</style>
