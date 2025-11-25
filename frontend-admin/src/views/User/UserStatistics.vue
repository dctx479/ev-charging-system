<template>
  <div class="user-statistics">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6" v-for="stat in statistics" :key="stat.key">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon :size="30">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 用户类型分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户类型分布</span>
          </template>
          <div ref="userTypeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 用户状态分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户状态分布</span>
          </template>
          <div ref="userStatusChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待审核列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>待审核用户</span>
          <el-button type="primary" size="small" @click="viewAllPending">
            查看全部
          </el-button>
        </div>
      </template>
      <el-table :data="pendingUsers" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userType" label="用户类型" width="120">
          <template #default="{ row }">
            {{ getUserTypeText(row.userType) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleReview(row, true)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="handleReview(row, false)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝审核" width="500px">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, UserFilled, OfficeBuilding, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const router = useRouter()

// 统计数据
const statistics = ref([
  { key: 'total', label: '总用户数', value: 0, icon: User, color: '#409EFF' },
  { key: 'personal', label: '个人用户', value: 0, icon: UserFilled, color: '#67C23A' },
  { key: 'operator', label: '企业运营商', value: 0, icon: OfficeBuilding, color: '#E6A23C' },
  { key: 'valet', label: '代充师傅', value: 0, icon: UserFilled, color: '#F56C6C' }
])

// 待审核用户
const pendingUsers = ref([])

// 图表实例
const userTypeChartRef = ref(null)
const userStatusChartRef = ref(null)
let userTypeChart = null
let userStatusChart = null

// 拒绝对话框
const rejectDialogVisible = ref(false)
const currentUser = ref(null)
const rejectForm = reactive({
  reason: ''
})

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const response = await request({
      url: '/admin/users/statistics',
      method: 'get'
    })

    if (response.code === 200) {
      const data = response.data
      statistics.value[0].value = data.totalUsers || 0
      statistics.value[1].value = data.personalUsers || 0
      statistics.value[2].value = data.operators || 0
      statistics.value[3].value = data.valetChargers || 0

      // 更新图表
      updateUserTypeChart(data)
      updateUserStatusChart(data)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取待审核用户
const fetchPendingUsers = async () => {
  try {
    const response = await request({
      url: '/admin/users/list',
      method: 'get',
      params: {
        page: 0,
        size: 5,
        status: 'PENDING'
      }
    })

    if (response.code === 200) {
      pendingUsers.value = response.data.content || []
    }
  } catch (error) {
    console.error('获取待审核用户失败:', error)
  }
}

// 更新用户类型图表
const updateUserTypeChart = (data) => {
  if (!userTypeChart) {
    userTypeChart = echarts.init(userTypeChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '用户类型',
        type: 'pie',
        radius: '50%',
        data: [
          { value: data.personalUsers || 0, name: '个人用户' },
          { value: data.operators || 0, name: '企业运营商' },
          { value: data.valetChargers || 0, name: '代充师傅' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  userTypeChart.setOption(option)
}

// 更新用户状态图表
const updateUserStatusChart = (data) => {
  if (!userStatusChart) {
    userStatusChart = echarts.init(userStatusChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: ['待审核', '已激活', '已拒绝', '已停用']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '用户数',
        type: 'bar',
        data: [
          data.pendingUsers || 0,
          data.activeUsers || 0,
          data.rejectedUsers || 0,
          data.suspendedUsers || 0
        ],
        itemStyle: {
          color: function(params) {
            const colors = ['#E6A23C', '#67C23A', '#F56C6C', '#909399']
            return colors[params.dataIndex]
          }
        }
      }
    ]
  }

  userStatusChart.setOption(option)
}

// 审核
const handleReview = (row, approved) => {
  if (approved) {
    ElMessageBox.confirm('确认通过该用户的审核？', '提示', {
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

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  await reviewUser(currentUser.value.id, false, rejectForm.reason)
  rejectDialogVisible.value = false
}

// 审核用户
const reviewUser = async (userId, approved, reason) => {
  try {
    const response = await request({
      url: `/admin/users/${userId}/review`,
      method: 'post',
      data: { approved, reason }
    })

    if (response.code === 200) {
      ElMessage.success(approved ? '审核通过' : '审核拒绝')
      fetchStatistics()
      fetchPendingUsers()
    }
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error(error.response?.data?.message || '审核失败')
  }
}

// 查看全部待审核
const viewAllPending = () => {
  router.push('/users/valet-chargers?status=PENDING')
}

// 用户类型文本
const getUserTypeText = (type) => {
  const map = {
    USER: '个人用户',
    OPERATOR: '企业运营商',
    VALET_CHARGER: '代充师傅'
  }
  return map[type] || type
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleResize = () => {
  userTypeChart?.resize()
  userStatusChart?.resize()
}

onMounted(async () => {
  await fetchStatistics()
  await fetchPendingUsers()

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (userTypeChart) {
    userTypeChart.dispose()
    userTypeChart = null
  }
  if (userStatusChart) {
    userStatusChart.dispose()
    userStatusChart = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.user-statistics {
  padding: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
