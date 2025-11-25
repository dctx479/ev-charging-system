<template>
  <div class="operator-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>企业运营商管理</span>
          <el-button type="primary" @click="handleRefresh">刷新</el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已激活" value="ACTIVE" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已停用" value="SUSPENDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="用户名/手机号/企业名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="companyName" label="企业名称" width="200" />
        <el-table-column prop="businessLicense" label="信用代码" width="200" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              @click="handleReview(row, true)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="handleReview(row, false)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning"
              size="small"
              @click="handleSuspend(row)"
            >
              停用
            </el-button>
            <el-button
              v-if="row.status === 'SUSPENDED'"
              type="success"
              size="small"
              @click="handleActivate(row)"
            >
              激活
            </el-button>
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 审核拒绝对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝审核"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="企业运营商详情"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户名">
          {{ currentUser.username }}
        </el-descriptions-item>
        <el-descriptions-item label="企业名称">
          {{ currentUser.companyName }}
        </el-descriptions-item>
        <el-descriptions-item label="信用代码" :span="2">
          {{ currentUser.businessLicense }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ currentUser.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ currentUser.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentUser.status)">
            {{ getStatusText(currentUser.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatTime(currentUser.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="账户余额">
          ¥{{ currentUser.balance || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="碳积分">
          {{ currentUser.carbonCredits || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="currentUser.rejectReason">
          {{ currentUser.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const queryForm = reactive({
  status: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const tableData = ref([])
const loading = ref(false)
const rejectDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentUser = ref(null)

const rejectForm = reactive({
  reason: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined
    }

    const response = await request({
      url: '/admin/users/operators',
      method: 'get',
      params
    })

    if (response.code === 200) {
      tableData.value = response.data.content || []
      pagination.total = response.data.totalElements
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.status = ''
  queryForm.keyword = ''
  pagination.page = 1
  fetchData()
}

const handleRefresh = () => {
  fetchData()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchData()
}

const handleReview = (row, approved) => {
  if (approved) {
    ElMessageBox.confirm('确认通过该企业运营商的审核？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    }).then(async () => {
      await reviewUser(row.id, true, null)
    }).catch(() => {})
  } else {
    currentUser.value = row
    rejectForm.reason = ''
    rejectDialogVisible.value = true
  }
}

const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  try {
    await reviewUser(currentUser.value.id, false, rejectForm.reason)
    rejectDialogVisible.value = false
  } catch (error) {
    // reviewUser内部已显示错误提示，对话框保持打开
  }
}

const reviewUser = async (userId, approved, reason) => {
  try {
    const response = await request({
      url: `/admin/users/${userId}/review`,
      method: 'post',
      data: {
        approved,
        reason
      }
    })

    if (response.code === 200) {
      ElMessage.success(approved ? '审核通过' : '审核拒绝')
      fetchData()
    } else {
      throw new Error(response.message || '审核失败')
    }
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error(error.message || '审核失败')
    throw error
  }
}

const handleSuspend = (row) => {
  ElMessageBox.confirm('确认停用该企业运营商？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await updateStatus(row.id, 'SUSPENDED')
  }).catch(() => {})
}

const handleActivate = (row) => {
  ElMessageBox.confirm('确认激活该企业运营商？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    await updateStatus(row.id, 'ACTIVE')
  }).catch(() => {})
}

const updateStatus = async (userId, status) => {
  try {
    const response = await request({
      url: `/admin/users/${userId}/status`,
      method: 'put',
      data: { status }
    })

    if (response.code === 200) {
      ElMessage.success('状态更新成功')
      fetchData()
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
  }
}

const handleViewDetail = async (row) => {
  try {
    const response = await request({
      url: `/admin/users/${row.id}`,
      method: 'get'
    })

    if (response.code === 200) {
      currentUser.value = response.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    ACTIVE: 'success',
    REJECTED: 'danger',
    SUSPENDED: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    PENDING: '待审核',
    ACTIVE: '已激活',
    REJECTED: '已拒绝',
    SUSPENDED: '已停用'
  }
  return textMap[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.operator-management {
  padding: 20px;
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
