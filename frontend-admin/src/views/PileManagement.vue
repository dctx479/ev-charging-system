<template>
  <div class="pile-management">
    <h2>充电桩管理</h2>

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

        <el-form-item label="充电桩状态">
          <el-select
            v-model="queryParams.status"
            placeholder="选择状态"
            clearable
            style="width: 150px"
          >
            <el-option label="空闲" :value="1" />
            <el-option label="充电中" :value="2" />
            <el-option label="预约中" :value="3" />
            <el-option label="故障" :value="4" />
            <el-option label="离线" :value="5" />
          </el-select>
        </el-form-item>

        <el-form-item label="充电桩类型">
          <el-select
            v-model="queryParams.type"
            placeholder="选择类型"
            clearable
            style="width: 150px"
          >
            <el-option label="交流慢充" :value="1" />
            <el-option label="直流快充" :value="2" />
            <el-option label="超级快充" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 充电桩列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="pileList"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="pileNo" label="充电桩编号" width="150" fixed="left">
          <template #default="{ row }">
            <el-tag type="info">{{ row.pileNo }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="stationName" label="所属站点" width="200" />

        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getPileTypeTagType(row.type)">
              {{ getPileTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="power" label="功率 (kW)" width="100">
          <template #default="{ row }">
            {{ row.power }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="健康度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.healthScore || 0"
              :color="getHealthScoreColor(row.healthScore)"
              :status="row.healthScore < 60 ? 'exception' : row.healthScore < 80 ? 'warning' : 'success'"
            />
          </template>
        </el-table-column>

        <el-table-column prop="totalChargeCount" label="累计充电次数" width="140" />

        <el-table-column prop="totalChargeAmount" label="累计充电量 (kWh)" width="160">
          <template #default="{ row }">
            {{ row.totalChargeAmount?.toFixed(2) || 0 }}
          </template>
        </el-table-column>

        <el-table-column prop="currentPower" label="当前功率 (kW)" width="140">
          <template #default="{ row }">
            {{ row.currentPower || 0 }}
          </template>
        </el-table-column>

        <el-table-column prop="lastMaintenanceTime" label="上次维护时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastMaintenanceTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="handleViewDetail(row)"
              :icon="View"
            >
              详情
            </el-button>
            <el-button
              size="small"
              type="success"
              v-if="row.status === 5"
              @click="handleToggleStatus(row, 1)"
              :icon="VideoPlay"
            >
              启用
            </el-button>
            <el-button
              size="small"
              type="warning"
              v-if="row.status === 1"
              @click="handleToggleStatus(row, 5)"
              :icon="VideoPause"
            >
              禁用
            </el-button>
            <el-button
              size="small"
              type="info"
              @click="handleViewHealth(row)"
              :icon="Notebook"
            >
              健康报告
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
        @size-change="fetchPileList"
        @current-change="fetchPileList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 充电桩详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="充电桩详情"
      width="800px"
    >
      <el-descriptions :column="2" border v-if="currentPile">
        <el-descriptions-item label="充电桩编号">{{ currentPile.pileNo }}</el-descriptions-item>
        <el-descriptions-item label="所属站点">{{ currentPile.stationName }}</el-descriptions-item>
        <el-descriptions-item label="充电桩类型">
          <el-tag :type="getPileTypeTagType(currentPile.type)">
            {{ getPileTypeText(currentPile.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="功率">{{ currentPile.power }} kW</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentPile.status)">
            {{ getStatusText(currentPile.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="健康度">
          {{ currentPile.healthScore || 0 }}%
        </el-descriptions-item>
        <el-descriptions-item label="累计充电次数">
          {{ currentPile.totalChargeCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="累计充电量">
          {{ currentPile.totalChargeAmount?.toFixed(2) || 0 }} kWh
        </el-descriptions-item>
        <el-descriptions-item label="当前功率">
          {{ currentPile.currentPower || 0 }} kW
        </el-descriptions-item>
        <el-descriptions-item label="当前电压">
          {{ currentPile.currentVoltage || 0 }} V
        </el-descriptions-item>
        <el-descriptions-item label="当前电流">
          {{ currentPile.currentCurrent || 0 }} A
        </el-descriptions-item>
        <el-descriptions-item label="温度">
          {{ currentPile.temperature || 0 }}°C
        </el-descriptions-item>
        <el-descriptions-item label="上次维护时间" :span="2">
          {{ formatDate(currentPile.lastMaintenanceTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="投入使用时间" :span="2">
          {{ formatDate(currentPile.installTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 健康报告对话框 -->
    <el-dialog
      v-model="healthDialogVisible"
      title="充电桩健康报告"
      width="700px"
    >
      <div v-if="currentPile">
        <el-alert
          :title="`健康度评分：${currentPile.healthScore || 0}%`"
          :type="currentPile.healthScore >= 80 ? 'success' : currentPile.healthScore >= 60 ? 'warning' : 'error'"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <div style="margin-top: 10px">
              <el-progress
                :percentage="currentPile.healthScore || 0"
                :color="getHealthScoreColor(currentPile.healthScore)"
              />
            </div>
          </template>
        </el-alert>

        <el-card shadow="never">
          <template #header>
            <span>运行数据统计</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="stat-item">
                <span class="label">累计充电次数：</span>
                <span class="value">{{ currentPile.totalChargeCount || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <span class="label">累计充电量：</span>
                <span class="value">{{ currentPile.totalChargeAmount?.toFixed(2) || 0 }} kWh</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <span class="label">平均每日使用次数：</span>
                <span class="value">{{ calculateAvgDailyUsage(currentPile) }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-item">
                <span class="label">距上次维护：</span>
                <span class="value">{{ calculateDaysSinceLastMaintenance(currentPile) }} 天</span>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="never" style="margin-top: 20px">
          <template #header>
            <span>维护建议</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(suggestion, index) in getMaintenanceSuggestions(currentPile)"
              :key="index"
              :type="suggestion.type"
              :icon="suggestion.icon"
            >
              {{ suggestion.text }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, VideoPlay, VideoPause, Notebook } from '@element-plus/icons-vue'
import { getAllPiles, getPilesByStation, updatePileStatus } from '@/api/pile'
import WebSocketClient from '@/utils/websocket'

// 查询参数
const queryParams = ref({
  stationId: null,
  status: null,
  type: null,
  page: 1,
  size: 10
})

// 数据列表
const pileList = ref([])
const stations = ref([])
const total = ref(0)
const loading = ref(false)

// 对话框
const detailDialogVisible = ref(false)
const healthDialogVisible = ref(false)
const currentPile = ref(null)

// WebSocket 实例
let wsClient = null

// 获取充电桩列表
const fetchPileList = async () => {
  loading.value = true
  try {
    let res
    if (queryParams.value.stationId) {
      res = await getPilesByStation(queryParams.value.stationId)
    } else {
      res = await getAllPiles()
    }

    let data = res.data || []

    // 客户端过滤
    if (queryParams.value.status !== null) {
      data = data.filter(pile => pile.status === queryParams.value.status)
    }
    if (queryParams.value.type !== null) {
      data = data.filter(pile => pile.type === queryParams.value.type)
    }

    // 分页处理
    total.value = data.length
    const start = (queryParams.value.page - 1) * queryParams.value.size
    const end = start + queryParams.value.size
    pileList.value = data.slice(start, end)

    // 提取站点列表（去重）
    if (!stations.value.length) {
      const stationMap = new Map()
      data.forEach(pile => {
        if (pile.stationId && pile.stationName) {
          stationMap.set(pile.stationId, { id: pile.stationId, name: pile.stationName })
        }
      })
      stations.value = Array.from(stationMap.values())
    }
  } catch (error) {
    ElMessage.error('获取充电桩列表失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryParams.value.page = 1
  fetchPileList()
}

// 重置
const handleReset = () => {
  queryParams.value = {
    stationId: null,
    status: null,
    type: null,
    page: 1,
    size: 10
  }
  fetchPileList()
}

// 查看详情
const handleViewDetail = (pile) => {
  currentPile.value = pile
  detailDialogVisible.value = true
}

// 查看健康报告
const handleViewHealth = (pile) => {
  currentPile.value = pile
  healthDialogVisible.value = true
}

// 切换状态
const handleToggleStatus = async (pile, newStatus) => {
  const statusText = newStatus === 1 ? '启用' : '禁用'

  ElMessageBox.confirm(`确认${statusText}充电桩 ${pile.pileNo}？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updatePileStatus(pile.id, newStatus)
      ElMessage.success(`${statusText}成功`)
      fetchPileList()
    } catch (error) {
      ElMessage.error(`${statusText}失败`)
    }
  })
}

// 辅助函数
const getPileTypeText = (type) => {
  const map = { 1: '交流慢充', 2: '直流快充', 3: '超级快充' }
  return map[type] || '未知'
}

const getPileTypeTagType = (type) => {
  const map = { 1: 'info', 2: 'success', 3: 'warning' }
  return map[type] || ''
}

const getStatusText = (status) => {
  const map = { 1: '空闲', 2: '充电中', 3: '预约中', 4: '故障', 5: '离线' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 1: 'success', 2: '', 3: 'warning', 4: 'danger', 5: 'info' }
  return map[status] || ''
}

const getHealthScoreColor = (score) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const calculateAvgDailyUsage = (pile) => {
  if (!pile.installTime || !pile.totalChargeCount) return 0
  const days = Math.max(1, Math.floor((Date.now() - new Date(pile.installTime).getTime()) / (1000 * 60 * 60 * 24)))
  return (pile.totalChargeCount / days).toFixed(1)
}

const calculateDaysSinceLastMaintenance = (pile) => {
  if (!pile.lastMaintenanceTime) return '-'
  return Math.floor((Date.now() - new Date(pile.lastMaintenanceTime).getTime()) / (1000 * 60 * 60 * 24))
}

const getMaintenanceSuggestions = (pile) => {
  const suggestions = []
  const healthScore = pile.healthScore || 0
  const daysSinceMaintenance = calculateDaysSinceLastMaintenance(pile)

  if (healthScore < 60) {
    suggestions.push({
      type: 'danger',
      icon: 'WarningFilled',
      text: '健康度低于60%，建议立即安排全面检修'
    })
  } else if (healthScore < 80) {
    suggestions.push({
      type: 'warning',
      icon: 'Warning',
      text: '健康度偏低，建议近期安排检查维护'
    })
  } else {
    suggestions.push({
      type: 'success',
      icon: 'SuccessFilled',
      text: '健康度良好，继续保持正常运维'
    })
  }

  if (daysSinceMaintenance > 90) {
    suggestions.push({
      type: 'warning',
      icon: 'Clock',
      text: `距上次维护已超过${daysSinceMaintenance}天，建议尽快安排定期维护`
    })
  }

  if (pile.totalChargeCount > 10000) {
    suggestions.push({
      type: 'info',
      icon: 'InfoFilled',
      text: '累计充电次数较高，建议检查充电接口和电缆磨损情况'
    })
  }

  if (pile.status === 4) {
    suggestions.push({
      type: 'danger',
      icon: 'CircleCloseFilled',
      text: '充电桩处于故障状态，请及时处理故障并恢复运行'
    })
  }

  return suggestions
}

// 初始化 WebSocket 连接
const initWebSocket = () => {
  wsClient = new WebSocketClient('ws://localhost:8080/api/ws/pile-status')
  wsClient.connect()

  // 监听 WebSocket 消息
  wsClient.onMessage((message) => {
    console.log('充电桩管理收到 WebSocket 消息:', message)

    // 处理充电桩状态更新消息
    if (message.type === 'pile_status_update') {
      const { pileId, status, pileNo } = message

      // 在列表中查找对应的充电桩并更新状态
      const pile = pileList.value.find(p => p.id === pileId)
      if (pile) {
        pile.status = status
        ElMessage.info(`充电桩 ${pileNo} 状态已更新为 ${getStatusText(status)}`)
      } else {
        // 如果当前页面没有该充电桩，刷新列表
        fetchPileList()
      }
    }

    // 处理故障告警消息
    if (message.type === 'fault_alert') {
      const { pileId, pileNo, severity } = message

      // 更新充电桩状态为故障
      const pile = pileList.value.find(p => p.id === pileId)
      if (pile) {
        pile.status = 4 // 故障状态
      }

      // 显示告警通知
      const severityText = getSeverityTextForAlert(severity)
      ElMessage.warning(`充电桩 ${pileNo} 发生${severityText}故障，请及时处理`)
    }
  })
}

// 获取告警严重程度文本（用于WebSocket消息）
const getSeverityTextForAlert = (severity) => {
  const map = { 1: '轻微', 2: '一般', 3: '严重', 4: '紧急' }
  return map[severity] || ''
}

// 生命周期
onMounted(() => {
  fetchPileList()

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
.pile-management {
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

.stat-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-item .label {
  color: #909399;
  font-size: 14px;
}

.stat-item .value {
  color: #303133;
  font-size: 16px;
  font-weight: bold;
  margin-left: 10px;
}
</style>
