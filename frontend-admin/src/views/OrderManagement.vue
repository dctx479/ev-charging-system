<template>
  <div class="order-management">
    <h2>订单管理</h2>

    <!-- 搜索筛选区域 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryParams" label-width="80px">
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.keyword"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="订单状态">
          <el-select
            v-model="queryParams.orderStatus"
            placeholder="选择订单状态"
            clearable
            style="width: 150px"
          >
            <el-option label="待支付" :value="1" />
            <el-option label="充电中" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="异常" :value="5" />
          </el-select>
        </el-form-item>

        <el-form-item label="支付状态">
          <el-select
            v-model="queryParams.paymentStatus"
            placeholder="选择支付状态"
            clearable
            style="width: 150px"
          >
            <el-option label="未支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退款" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="充电站">
          <el-select
            v-model="queryParams.stationId"
            placeholder="选择充电站"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="station in stations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="Refresh">重置</el-button>
          <el-button type="success" @click="handleExport" :icon="Download">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="orderList"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="订单号" width="200" fixed />

        <el-table-column label="用户信息" width="200">
          <template #default="{ row }">
            <div>
              <div><strong>{{ row.userName }}</strong></div>
              <div style="color: #909399; font-size: 12px">{{ row.userPhone }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="充电站/桩" width="250">
          <template #default="{ row }">
            <div>
              <div>{{ row.stationName }}</div>
              <div style="color: #909399; font-size: 12px">
                <el-tag size="small" type="info">{{ row.pileNo }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="充电量" width="120" align="right">
          <template #default="{ row }">
            <span style="color: #409eff; font-weight: bold">
              {{ row.chargeAmount ? row.chargeAmount.toFixed(2) : '0.00' }} kWh
            </span>
          </template>
        </el-table-column>

        <el-table-column label="费用" width="120" align="right">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">
              ¥{{ row.totalFee ? row.totalFee.toFixed(2) : '0.00' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderStatus)">
              {{ getOrderStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="支付状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getPaymentStatusType(row.paymentStatus)">
              {{ getPaymentStatusText(row.paymentStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
              :icon="View"
            >
              详情
            </el-button>
            <el-button
              type="danger"
              size="small"
              v-if="row.paymentStatus === 1"
              @click="handleRefund(row)"
              :icon="RefreshLeft"
            >
              退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="800px"
      destroy-on-close
    >
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">
          <el-tag>{{ currentOrder.orderNo }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="用户姓名">
          {{ currentOrder.userName }}
        </el-descriptions-item>
        <el-descriptions-item label="用户手机">
          {{ currentOrder.userPhone }}
        </el-descriptions-item>

        <el-descriptions-item label="充电站">
          {{ currentOrder.stationName }}
        </el-descriptions-item>
        <el-descriptions-item label="充电桩">
          <el-tag type="info">{{ currentOrder.pileNo }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="开始时间">
          {{ formatDateTime(currentOrder.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ formatDateTime(currentOrder.endTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="充电时长">
          {{ currentOrder.chargeDuration || 0 }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="充电量">
          <span style="color: #409eff; font-weight: bold">
            {{ currentOrder.chargeAmount ? currentOrder.chargeAmount.toFixed(2) : '0.00' }} kWh
          </span>
        </el-descriptions-item>

        <el-descriptions-item label="电费">
          ¥{{ currentOrder.electricityFee ? currentOrder.electricityFee.toFixed(2) : '0.00' }}
        </el-descriptions-item>
        <el-descriptions-item label="服务费">
          ¥{{ currentOrder.serviceFee ? currentOrder.serviceFee.toFixed(2) : '0.00' }}
        </el-descriptions-item>

        <el-descriptions-item label="总费用" :span="2">
          <span style="color: #f56c6c; font-weight: bold; font-size: 16px">
            ¥{{ currentOrder.totalFee ? currentOrder.totalFee.toFixed(2) : '0.00' }}
          </span>
        </el-descriptions-item>

        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatusType(currentOrder.orderStatus)">
            {{ getOrderStatusText(currentOrder.orderStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPaymentStatusType(currentOrder.paymentStatus)">
            {{ getPaymentStatusText(currentOrder.paymentStatus) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="支付方式" v-if="currentOrder.paymentMethod">
          {{ getPaymentMethodText(currentOrder.paymentMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付时间" v-if="currentOrder.paymentTime">
          {{ formatDateTime(currentOrder.paymentTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间">
          {{ formatDateTime(currentOrder.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(currentOrder.updateTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="备注" :span="2" v-if="currentOrder.remark">
          {{ currentOrder.remark }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退款确认对话框 -->
    <el-dialog
      v-model="refundDialogVisible"
      title="订单退款"
      width="500px"
      destroy-on-close
    >
      <el-form :model="refundForm" :rules="refundRules" ref="refundFormRef" label-width="100px">
        <el-form-item label="订单号">
          <el-tag>{{ currentOrder?.orderNo }}</el-tag>
        </el-form-item>

        <el-form-item label="订单金额">
          <span style="color: #f56c6c; font-weight: bold">
            ¥{{ currentOrder?.totalFee ? currentOrder.totalFee.toFixed(2) : '0.00' }}
          </span>
        </el-form-item>

        <el-form-item label="退款金额" prop="refundAmount">
          <el-input-number
            v-model="refundForm.refundAmount"
            :precision="2"
            :step="0.1"
            :max="currentOrder?.totalFee || 0"
            :min="0.01"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="退款原因" prop="refundReason">
          <el-input
            v-model="refundForm.refundReason"
            type="textarea"
            :rows="3"
            placeholder="请输入退款原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmRefund" :loading="refundLoading">
          确认退款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, View, RefreshLeft } from '@element-plus/icons-vue'
import {
  getOrderList,
  getOrderDetail,
  refundOrder,
  exportOrders,
  getStationList
} from '@/api/order'

// 查询参数
const queryParams = reactive({
  keyword: '',  // 后端使用keyword字段搜索订单号
  orderStatus: null,
  paymentStatus: null,
  stationId: null,
  startTime: '',
  endTime: '',
  page: 0,  // Spring Page从0开始索引
  size: 10
})

// 日期范围
const dateRange = ref([])

// 订单列表
const orderList = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)  // 响应式的当前页，用于分页组件绑定

// 充电站列表
const stations = ref([])

// 订单详情对话框
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

// 退款对话框
const refundDialogVisible = ref(false)
const refundLoading = ref(false)
const refundFormRef = ref(null)
const refundForm = reactive({
  refundAmount: 0,
  refundReason: ''
})

const refundRules = {
  refundAmount: [
    { required: true, message: '请输入退款金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '退款金额必须大于0', trigger: 'blur' }
  ],
  refundReason: [
    { required: true, message: '请输入退款原因', trigger: 'blur' },
    { min: 5, max: 200, message: '退款原因长度在5-200个字符', trigger: 'blur' }
  ]
}

// 获取订单列表
const fetchOrderList = async () => {
  loading.value = true
  try {
    // 处理日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    } else {
      queryParams.startTime = ''
      queryParams.endTime = ''
    }

    const res = await getOrderList(queryParams)
    // Spring Page返回content字段，兼容多种格式
    orderList.value = res.data.content || res.data.records || res.data.list || []
    total.value = res.data.totalElements || res.data.total || 0
    // 同步 currentPage 与 queryParams.page
    currentPage.value = queryParams.page + 1
  } catch (error) {
    ElMessage.error('获取订单列表失败')
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取充电站列表
const fetchStations = async () => {
  try {
    const res = await getStationList()
    stations.value = res.data || []
  } catch (error) {
    console.error('获取充电站列表失败:', error)
  }
}

// 查询
const handleQuery = () => {
  queryParams.page = 0
  fetchOrderList()
}

// 重置
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.orderStatus = null
  queryParams.paymentStatus = null
  queryParams.stationId = null
  queryParams.startTime = ''
  queryParams.endTime = ''
  dateRange.value = []
  queryParams.page = 0
  fetchOrderList()
}

// 分页大小变化
const handleSizeChange = (val) => {
  queryParams.size = val
  queryParams.page = 0
  fetchOrderList()
}

// 当前页变化 (el-pagination uses 1-based page, backend uses 0-based)
const handleCurrentChange = (val) => {
  queryParams.page = val - 1  // 转换为0-based
  fetchOrderList()
}

// 查看订单详情
const handleViewDetail = async (row) => {
  try {
    const res = await getOrderDetail(row.id)
    currentOrder.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
    console.error('获取订单详情失败:', error)
  }
}

// 打开退款对话框
const handleRefund = (row) => {
  currentOrder.value = row
  refundForm.refundAmount = row.totalFee
  refundForm.refundReason = ''
  refundDialogVisible.value = true
}

// 确认退款
const confirmRefund = async () => {
  if (!refundFormRef.value) return

  await refundFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await ElMessageBox.confirm(
        `确定要退款 ¥${refundForm.refundAmount.toFixed(2)} 吗？`,
        '退款确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      refundLoading.value = true
      await refundOrder(currentOrder.value.id, refundForm)
      ElMessage.success('退款成功')
      refundDialogVisible.value = false
      fetchOrderList()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('退款失败')
        console.error('退款失败:', error)
      }
    } finally {
      refundLoading.value = false
    }
  })
}

// 导出Excel
const handleExport = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要导出当前筛选条件下的订单数据吗？',
      '导出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true

    // 处理日期范围
    const exportParams = { ...queryParams }
    if (dateRange.value && dateRange.value.length === 2) {
      exportParams.startTime = dateRange.value[0]
      exportParams.endTime = dateRange.value[1]
    }

    const response = await exportOrders(exportParams)

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `订单列表_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('导出失败')
      console.error('导出失败:', error)
    }
  } finally {
    loading.value = false
  }
}

// 订单状态文本
const getOrderStatusText = (status) => {
  const statusMap = {
    1: '待支付',
    2: '充电中',
    3: '已完成',
    4: '已取消',
    5: '异常'
  }
  return statusMap[status] || '未知'
}

// 订单状态标签类型
const getOrderStatusType = (status) => {
  const typeMap = {
    1: 'warning', // 橙色（待支付）
    2: '',        // 蓝色（充电中，默认）
    3: 'success', // 绿色（已完成）
    4: 'info',    // 灰色（已取消）
    5: 'danger'   // 红色（异常）
  }
  return typeMap[status] || 'info'
}

// 支付状态文本
const getPaymentStatusText = (status) => {
  const statusMap = {
    0: '未支付',
    1: '已支付',
    2: '已退款'
  }
  return statusMap[status] || '未知'
}

// 支付状态标签类型
const getPaymentStatusType = (status) => {
  const typeMap = {
    0: 'warning',  // 橙色（未支付）
    1: 'success',  // 绿色（已支付）
    2: 'info'      // 灰色（已退款）
  }
  return typeMap[status] || 'info'
}

// 支付方式文本
const getPaymentMethodText = (method) => {
  const methodMap = {
    1: '微信支付',
    2: '支付宝',
    3: '余额支付',
    4: '银行卡'
  }
  return methodMap[method] || '未知'
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 页面加载时获取数据
onMounted(() => {
  fetchOrderList()
  fetchStations()
})
</script>

<style scoped>
.order-management {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

h2 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 20px;
  font-weight: bold;
}
</style>
