# 前端集成指南 - 管理后台订单管理

## 快速开始

### API 基础路径
```
http://localhost:8080/admin/orders
```

### 数据字典

#### 订单状态 (orderStatus)
```javascript
const ORDER_STATUS = {
  IN_PROGRESS: 0,  // 进行中
  COMPLETED: 1,    // 已完成
  CANCELLED: 2,    // 已取消
  ABNORMAL: 3      // 异常
}

const ORDER_STATUS_TEXT = {
  0: '进行中',
  1: '已完成',
  2: '已取消',
  3: '异常'
}
```

#### 支付状态 (paymentStatus)
```javascript
const PAYMENT_STATUS = {
  UNPAID: 0,    // 未支付
  PAID: 1,      // 已支付
  REFUNDED: 2   // 已退款
}

const PAYMENT_STATUS_TEXT = {
  0: '未支付',
  1: '已支付',
  2: '已退款'
}

const PAYMENT_STATUS_TAG_TYPE = {
  0: 'warning',
  1: 'success',
  2: 'info'
}
```

#### 支付方式 (paymentMethod)
```javascript
const PAYMENT_METHOD = {
  WECHAT: 1,     // 微信
  ALIPAY: 2,     // 支付宝
  BALANCE: 3     // 余额
}

const PAYMENT_METHOD_TEXT = {
  1: '微信',
  2: '支付宝',
  3: '余额'
}
```

## Vue3 + Element Plus 示例

### 1. API 请求封装 (`api/order.js`)

```javascript
import request from '@/utils/request'

/**
 * 获取订单列表
 */
export function getOrderListAPI(params) {
  return request({
    url: '/admin/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetailAPI(id) {
  return request({
    url: `/admin/orders/${id}`,
    method: 'get'
  })
}

/**
 * 更新订单状态
 */
export function updateOrderStatusAPI(id, status) {
  return request({
    url: `/admin/orders/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 订单退款
 */
export function refundOrderAPI(id, data) {
  return request({
    url: `/admin/orders/${id}/refund`,
    method: 'post',
    data
  })
}

/**
 * 导出订单Excel
 */
export function exportOrdersAPI(params) {
  return request({
    url: '/admin/orders/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
```

### 2. 订单管理页面 (`views/OrderManagement.vue`)

```vue
<template>
  <div class="order-management">
    <!-- 筛选器 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="订单状态">
          <el-select v-model="queryParams.orderStatus" placeholder="请选择" clearable>
            <el-option label="进行中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="异常" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="支付状态">
          <el-select v-model="queryParams.paymentStatus" placeholder="请选择" clearable>
            <el-option label="未支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退款" :value="2" />
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
          />
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="订单号"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card class="table-card">
      <el-table
        :data="orderList"
        border
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userPhone" label="用户手机号" width="120" />
        <el-table-column prop="stationName" label="充电站" width="150" />
        <el-table-column prop="pileNo" label="充电桩编号" width="120" />
        <el-table-column label="充电量" width="100">
          <template #default="{ row }">
            {{ row.chargeAmount }} kWh
          </template>
        </el-table-column>
        <el-table-column label="总费用" width="100">
          <template #default="{ row }">
            ¥{{ row.totalFee }}
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getPaymentStatusType(row.paymentStatus)">
              {{ row.paymentStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderStatus)">
              {{ row.orderStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.orderStatus === 0"
              size="small"
              type="warning"
              @click="handleSetAbnormal(row)"
            >
              标记异常
            </el-button>
            <el-button
              v-if="row.paymentStatus === 1"
              size="small"
              type="danger"
              @click="handleRefund(row)"
            >
              退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">
          {{ orderDetail.orderNo }}
        </el-descriptions-item>
        <el-descriptions-item label="用户手机号">
          {{ orderDetail.userPhone }}
        </el-descriptions-item>
        <el-descriptions-item label="充电站">
          {{ orderDetail.stationName }}
        </el-descriptions-item>
        <el-descriptions-item label="充电桩编号">
          {{ orderDetail.pileNo }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ orderDetail.startTime }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ orderDetail.endTime }}
        </el-descriptions-item>
        <el-descriptions-item label="充电时长">
          {{ orderDetail.chargeDuration }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="充电量">
          {{ orderDetail.chargeAmount }} kWh
        </el-descriptions-item>
        <el-descriptions-item label="电费">
          ¥{{ orderDetail.electricityFee }}
        </el-descriptions-item>
        <el-descriptions-item label="服务费">
          ¥{{ orderDetail.serviceFee }}
        </el-descriptions-item>
        <el-descriptions-item label="总费用">
          ¥{{ orderDetail.totalFee }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPaymentStatusType(orderDetail.paymentStatus)">
            {{ orderDetail.paymentStatusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatusType(orderDetail.orderStatus)">
            {{ orderDetail.orderStatusText }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 退款对话框 -->
    <el-dialog v-model="refundDialogVisible" title="订单退款" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入退款原因"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="refundForm.operator" placeholder="请输入操作人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOrderListAPI,
  getOrderDetailAPI,
  updateOrderStatusAPI,
  refundOrderAPI,
  exportOrdersAPI
} from '@/api/order'

// 数据
const loading = ref(false)
const orderList = ref([])
const total = ref(0)
const detailDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const orderDetail = ref({})
const currentOrder = ref(null)
const dateRange = ref([])

const queryParams = reactive({
  orderStatus: null,
  paymentStatus: null,
  keyword: '',
  startTime: '',
  endTime: '',
  page: 0,
  size: 10
})

const refundForm = reactive({
  reason: '',
  operator: ''
})

// 方法
const handleQuery = async () => {
  loading.value = true
  try {
    // 处理时间范围
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    } else {
      queryParams.startTime = ''
      queryParams.endTime = ''
    }

    const { data } = await getOrderListAPI(queryParams)
    orderList.value = data.content
    total.value = data.totalElements
  } catch (error) {
    ElMessage.error('查询订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryParams.orderStatus = null
  queryParams.paymentStatus = null
  queryParams.keyword = ''
  queryParams.startTime = ''
  queryParams.endTime = ''
  dateRange.value = []
  handleQuery()
}

const handleViewDetail = async (row) => {
  try {
    const { data } = await getOrderDetailAPI(row.id)
    orderDetail.value = data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  }
}

const handleSetAbnormal = async (row) => {
  ElMessageBox.confirm('确认将该订单标记为异常状态吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateOrderStatusAPI(row.id, 3) // 3=异常
      ElMessage.success('操作成功')
      handleQuery()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

const handleRefund = (row) => {
  currentOrder.value = row
  refundForm.reason = ''
  refundForm.operator = ''
  refundDialogVisible.value = true
}

const handleConfirmRefund = async () => {
  if (!refundForm.reason) {
    ElMessage.warning('请输入退款原因')
    return
  }

  try {
    await refundOrderAPI(currentOrder.value.id, refundForm)
    ElMessage.success('退款成功')
    refundDialogVisible.value = false
    handleQuery()
  } catch (error) {
    ElMessage.error('退款失败')
  }
}

const handleExport = async () => {
  try {
    const params = {
      orderStatus: queryParams.orderStatus,
      paymentStatus: queryParams.paymentStatus,
      keyword: queryParams.keyword,
      startTime: queryParams.startTime,
      endTime: queryParams.endTime
    }

    const blob = await exportOrdersAPI(params)

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `充电订单_${Date.now()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const getPaymentStatusType = (status) => {
  const typeMap = { 0: 'warning', 1: 'success', 2: 'info' }
  return typeMap[status] || ''
}

const getOrderStatusType = (status) => {
  const typeMap = { 0: 'primary', 1: 'success', 2: 'info', 3: 'danger' }
  return typeMap[status] || ''
}

// 生命周期
onMounted(() => {
  handleQuery()
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
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
```

### 3. Axios 请求工具配置 (`utils/request.js`)

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加token等
    return config
  },
  error => {
    console.error('请求错误', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 处理文件下载
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    console.error('响应错误', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

## 常用代码片段

### 状态筛选下拉框
```vue
<el-select v-model="orderStatus" placeholder="订单状态">
  <el-option label="进行中" :value="0" />
  <el-option label="已完成" :value="1" />
  <el-option label="已取消" :value="2" />
  <el-option label="异常" :value="3" />
</el-select>
```

### 支付状态标签
```vue
<el-tag :type="getPaymentStatusType(row.paymentStatus)">
  {{ getPaymentStatusText(row.paymentStatus) }}
</el-tag>
```

### 时间范围选择器
```vue
<el-date-picker
  v-model="dateRange"
  type="datetimerange"
  range-separator="至"
  start-placeholder="开始时间"
  end-placeholder="结束时间"
  value-format="YYYY-MM-DD HH:mm:ss"
/>
```

### Excel导出
```javascript
const handleExport = async () => {
  const blob = await exportOrdersAPI(params)
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `订单_${Date.now()}.xlsx`
  link.click()
  window.URL.revokeObjectURL(url)
}
```

## 注意事项

1. **时间格式**: 后端接收的时间格式为 `yyyy-MM-dd HH:mm:ss`
2. **分页**: 页码从0开始（后端Spring Data JPA规范）
3. **Excel导出**: 需要设置 `responseType: 'blob'`
4. **状态值**: 使用数字类型，不是字符串
5. **权限控制**: 生产环境需要添加管理员权限验证

## 调试技巧

### 1. 查看请求参数
```javascript
console.log('查询参数:', queryParams)
```

### 2. 查看响应数据
```javascript
console.log('订单列表:', data.content)
console.log('总数:', data.totalElements)
```

### 3. 错误处理
```javascript
try {
  await getOrderListAPI(params)
} catch (error) {
  console.error('接口错误:', error)
  ElMessage.error(error.message)
}
```

## 完整的类型定义 (TypeScript)

```typescript
// types/order.ts
export interface OrderQuery {
  orderStatus?: number
  paymentStatus?: number
  stationId?: number
  pileId?: number
  keyword?: string
  startTime?: string
  endTime?: string
  page: number
  size: number
}

export interface OrderListItem {
  id: number
  orderNo: string
  userId: number
  userPhone: string
  userNickname: string
  stationId: number
  stationName: string
  pileId: number
  pileNo: string
  startTime: string
  endTime: string
  chargeDuration: number
  chargeAmount: number
  totalFee: number
  paymentStatus: number
  paymentStatusText: string
  paymentMethod: number
  paymentMethodText: string
  paymentTime: string
  orderStatus: number
  orderStatusText: string
  createTime: string
}

export interface RefundForm {
  reason: string
  operator: string
}
```

## 路由配置示例

```javascript
// router/index.js
{
  path: '/admin',
  component: Layout,
  children: [
    {
      path: 'orders',
      name: 'OrderManagement',
      component: () => import('@/views/OrderManagement.vue'),
      meta: { title: '订单管理', icon: 'order' }
    }
  ]
}
```

---

需要帮助？查看完整API文档: `backend/docs/API_OrderManagement.md`
