<template>
  <div class="fault-management">
    <h2>故障管理</h2>

    <!-- 筛选器 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="站点">
          <el-select
            v-model="queryParams.stationId"
            placeholder="选择站点"
            clearable
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

        <el-form-item label="维修状态">
          <el-select
            v-model="queryParams.repairStatus"
            placeholder="选择状态"
            clearable
            style="width: 150px"
          >
            <el-option label="待维修" :value="0" />
            <el-option label="维修中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="故障类型">
          <el-select
            v-model="queryParams.faultType"
            placeholder="选择类型"
            clearable
            style="width: 150px"
          >
            <el-option label="通信故障" :value="1" />
            <el-option label="硬件故障" :value="2" />
            <el-option label="软件故障" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="Refresh">重置</el-button>
          <el-button type="success" @click="handleReport" :icon="Plus">上报故障</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 故障列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="faultList"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="故障ID" width="80" />

        <el-table-column prop="pileNo" label="充电桩编号" width="150">
          <template #default="{ row }">
            <el-tag type="info">{{ row.pileNo }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="stationName" label="所属站点" width="180" />

        <el-table-column label="故障类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getFaultTypeTagType(row.faultType)">
              {{ getFaultTypeText(row.faultType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="严重程度" width="120">
          <template #default="{ row }">
            <el-tag :type="getSeverityType(row.severity)">
              {{ getSeverityText(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="faultDescription" label="故障描述" min-width="200" />

        <el-table-column prop="faultTime" label="故障时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.faultTime) }}
          </template>
        </el-table-column>

        <el-table-column label="维修状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRepairStatusType(row.repairStatus)">
              {{ getRepairStatusText(row.repairStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="repairPerson" label="维修人员" width="100" />

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              v-if="row.repairStatus === 0"
              @click="handleAssign(row)"
              :icon="User"
            >
              指派维修
            </el-button>
            <el-button
              size="small"
              type="success"
              v-if="row.repairStatus === 1"
              @click="handleComplete(row)"
              :icon="Check"
            >
              完成维修
            </el-button>
            <el-button
              size="small"
              @click="handleViewDetail(row)"
              :icon="View"
            >
              详情
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
        @size-change="fetchFaultList"
        @current-change="fetchFaultList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 上报故障对话框 -->
    <el-dialog
      v-model="reportDialogVisible"
      title="上报故障"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-width="100px"
      >
        <el-form-item label="充电桩" prop="pileId">
          <el-select
            v-model="reportForm.pileId"
            placeholder="选择充电桩"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="pile in allPiles"
              :key="pile.id"
              :label="`${pile.pileNo} - ${pile.stationName}`"
              :value="pile.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="故障类型" prop="faultType">
          <el-select v-model="reportForm.faultType" placeholder="选择故障类型" style="width: 100%">
            <el-option label="通信故障" :value="1" />
            <el-option label="硬件故障" :value="2" />
            <el-option label="软件故障" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="严重程度" prop="severity">
          <el-radio-group v-model="reportForm.severity">
            <el-radio :label="1">轻微</el-radio>
            <el-radio :label="2">一般</el-radio>
            <el-radio :label="3">严重</el-radio>
            <el-radio :label="4">紧急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="故障代码" prop="faultCode">
          <el-input v-model="reportForm.faultCode" placeholder="请输入故障代码" />
        </el-form-item>

        <el-form-item label="故障描述" prop="faultDescription">
          <el-input
            v-model="reportForm.faultDescription"
            type="textarea"
            :rows="4"
            placeholder="请详细描述故障现象"
          />
        </el-form-item>

        <el-form-item label="上报来源" prop="reportSource">
          <el-radio-group v-model="reportForm.reportSource">
            <el-radio :label="1">系统自动</el-radio>
            <el-radio :label="2">用户上报</el-radio>
            <el-radio :label="3">运维发现</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReport" :loading="submitting">
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 指派维修对话框 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="指派维修"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="维修人员">
          <el-input v-model="assignForm.repairPerson" placeholder="请输入维修人员姓名" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAssign" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 故障详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="故障详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentFault">
        <el-descriptions-item label="故障ID">{{ currentFault.id }}</el-descriptions-item>
        <el-descriptions-item label="充电桩编号">{{ currentFault.pileNo }}</el-descriptions-item>
        <el-descriptions-item label="所属站点">{{ currentFault.stationName }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">
          <el-tag :type="getFaultTypeTagType(currentFault.faultType)">
            {{ getFaultTypeText(currentFault.faultType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="严重程度">
          <el-tag :type="getSeverityType(currentFault.severity)">
            {{ getSeverityText(currentFault.severity) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="故障代码">{{ currentFault.faultCode }}</el-descriptions-item>
        <el-descriptions-item label="故障时间" :span="2">
          {{ formatDateTime(currentFault.faultTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">
          {{ currentFault.faultDescription }}
        </el-descriptions-item>
        <el-descriptions-item label="维修状态">
          <el-tag :type="getRepairStatusType(currentFault.repairStatus)">
            {{ getRepairStatusText(currentFault.repairStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="维修人员">
          {{ currentFault.repairPerson || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="维修开始时间" :span="2">
          {{ formatDateTime(currentFault.repairStartTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="维修完成时间" :span="2">
          {{ formatDateTime(currentFault.repairEndTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Search, Refresh, Plus, User, Check, View } from '@element-plus/icons-vue'
import { getFaultList, reportFault, updateRepairStatus, getFaultDetail } from '@/api/fault'
import { getAllPiles } from '@/api/pile'
import WebSocketClient from '@/utils/websocket'

// 查询参数
const queryParams = ref({
  stationId: null,
  repairStatus: null,
  faultType: null,
  page: 1,
  size: 10
})

// 数据列表
const faultList = ref([])
const stations = ref([])
const allPiles = ref([])
const total = ref(0)
const loading = ref(false)

// 对话框
const reportDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const submitting = ref(false)

// 表单
const reportFormRef = ref(null)
const reportForm = ref({
  pileId: null,
  faultType: null,
  severity: 2,
  faultCode: '',
  faultDescription: '',
  reportSource: 3
})

const assignForm = ref({
  repairPerson: ''
})

const currentFault = ref(null)

// WebSocket 实例
let wsClient = null

// 表单验证规则
const reportRules = {
  pileId: [{ required: true, message: '请选择充电桩', trigger: 'change' }],
  faultType: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
}

// 获取故障列表
const fetchFaultList = async () => {
  loading.value = true
  try {
    const res = await getFaultList(queryParams.value)
    faultList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取故障列表失败')
  } finally {
    loading.value = false
  }
}

// 获取充电桩列表
const fetchAllPiles = async () => {
  try {
    const res = await getAllPiles()
    allPiles.value = res.data || []

    // 提取站点列表（去重）
    const stationMap = new Map()
    allPiles.value.forEach(pile => {
      if (pile.stationId && pile.stationName) {
        stationMap.set(pile.stationId, { id: pile.stationId, name: pile.stationName })
      }
    })
    stations.value = Array.from(stationMap.values())
  } catch (error) {
    console.error('获取充电桩列表失败:', error)
  }
}

// 查询
const handleQuery = () => {
  queryParams.value.page = 1
  fetchFaultList()
}

// 重置
const handleReset = () => {
  queryParams.value = {
    stationId: null,
    repairStatus: null,
    faultType: null,
    page: 1,
    size: 10
  }
  fetchFaultList()
}

// 上报故障
const handleReport = () => {
  reportForm.value = {
    pileId: null,
    faultType: null,
    severity: 2,
    faultCode: '',
    faultDescription: '',
    reportSource: 3
  }
  reportDialogVisible.value = true
}

// 提交上报
const handleSubmitReport = async () => {
  if (!reportFormRef.value) return

  await reportFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await reportFault(reportForm.value)
      ElMessage.success('故障上报成功')
      reportDialogVisible.value = false
      fetchFaultList()
    } catch (error) {
      ElMessage.error('故障上报失败')
    } finally {
      submitting.value = false
    }
  })
}

// 指派维修
const handleAssign = (fault) => {
  currentFault.value = fault
  assignForm.value.repairPerson = ''
  assignDialogVisible.value = true
}

// 提交指派
const handleSubmitAssign = async () => {
  if (!assignForm.value.repairPerson) {
    ElMessage.warning('请输入维修人员姓名')
    return
  }

  submitting.value = true
  try {
    await updateRepairStatus(currentFault.value.id, {
      repairStatus: 1,
      repairPerson: assignForm.value.repairPerson
    })
    ElMessage.success('指派成功')
    assignDialogVisible.value = false
    fetchFaultList()
  } catch (error) {
    ElMessage.error('指派失败')
  } finally {
    submitting.value = false
  }
}

// 完成维修
const handleComplete = async (fault) => {
  ElMessageBox.confirm('确认该故障已维修完成？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateRepairStatus(fault.id, {
        repairStatus: 2
      })
      ElMessage.success('维修完成')
      fetchFaultList()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

// 查看详情
const handleViewDetail = async (fault) => {
  try {
    const res = await getFaultDetail(fault.id)
    currentFault.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 辅助函数
const getFaultTypeText = (type) => {
  const map = { 1: '通信故障', 2: '硬件故障', 3: '软件故障', 4: '其他' }
  return map[type] || '未知'
}

const getFaultTypeTagType = (type) => {
  const map = { 1: 'warning', 2: 'danger', 3: 'info', 4: '' }
  return map[type] || ''
}

const getSeverityText = (severity) => {
  const map = { 1: '轻微', 2: '一般', 3: '严重', 4: '紧急' }
  return map[severity] || '未知'
}

const getSeverityType = (severity) => {
  const map = { 1: 'success', 2: '', 3: 'warning', 4: 'danger' }
  return map[severity] || ''
}

const getRepairStatusText = (status) => {
  const map = { 0: '待维修', 1: '维修中', 2: '已完成' }
  return map[status] || '未知'
}

const getRepairStatusType = (status) => {
  const map = { 0: 'danger', 1: 'warning', 2: 'success' }
  return map[status] || ''
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 初始化 WebSocket 连接
const initWebSocket = () => {
  wsClient = new WebSocketClient('ws://localhost:8080/api/ws/pile-status')
  wsClient.connect()

  // 监听 WebSocket 消息
  wsClient.onMessage((message) => {
    console.log('故障管理收到 WebSocket 消息:', message)

    // 处理故障告警消息
    if (message.type === 'fault_alert') {
      const { pileId, pileNo, severity } = message

      // 获取严重程度文本和通知类型
      const severityText = getSeverityText(severity)
      const notificationType = getNotificationType(severity)

      // 弹出通知
      ElNotification({
        title: '新故障告警',
        message: `充电桩 ${pileNo} 发生${severityText}故障，请及时处理`,
        type: notificationType,
        duration: 0, // 不自动关闭
        position: 'top-right'
      })

      // 自动刷新故障列表
      fetchFaultList()
    }
  })
}

// 根据严重程度获取通知类型
const getNotificationType = (severity) => {
  const map = { 1: 'info', 2: 'warning', 3: 'warning', 4: 'error' }
  return map[severity] || 'warning'
}

// 生命周期
onMounted(() => {
  fetchFaultList()
  fetchAllPiles()

  // 连接 WebSocket
  initWebSocket()
})

onUnmounted(() => {
  // 断开 WebSocket 连接
  if (wsClient) {
    wsClient.close()
  }
})
</script>

<style scoped>
.fault-management {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}
</style>
