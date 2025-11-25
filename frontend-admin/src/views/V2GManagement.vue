<template>
  <div class="v2g-management">
    <el-card class="statistics-card">
      <template #header>
        <span>V2G统计数据</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="今日放电量" :value="statistics.todayDischarge || 0" suffix="kWh" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="今日收益" :value="statistics.todayIncome || 0" prefix="¥" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="放电次数" :value="statistics.dischargeCount || 0" suffix="次" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="参与用户" :value="statistics.userCount || 0" suffix="人" />
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>V2G放电记录</span>
          <el-button type="primary" @click="handleExport">导出数据</el-button>
        </div>
      </template>

      <!-- 筛选器 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="用户ID">
          <el-input v-model="queryForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="充电桩ID">
          <el-input v-model="queryForm.pileId" placeholder="请输入充电桩ID" clearable />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 记录表格 -->
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="pileId" label="充电桩ID" width="100" />
        <el-table-column prop="energyAmount" label="放电量(kWh)" width="120" />
        <el-table-column label="电价(元/kWh)" width="120">
          <template #default="{ row }">
            {{ row.electricityPrice?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="收益(元)" width="100">
          <template #default="{ row }">
            <span style="color: #67c23a; font-weight: bold;">
              ¥{{ row.profit?.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getRecordTypeTag(row.recordType)">
              {{ getRecordTypeText(row.recordType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="放电记录详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRecord.userId }}</el-descriptions-item>
        <el-descriptions-item label="充电桩ID">{{ currentRecord.pileId }}</el-descriptions-item>
        <el-descriptions-item label="放电量">{{ currentRecord.energyAmount }} kWh</el-descriptions-item>
        <el-descriptions-item label="电价">{{ currentRecord.electricityPrice?.toFixed(2) }} 元/kWh</el-descriptions-item>
        <el-descriptions-item label="收益">¥{{ currentRecord.profit?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="放电前电池健康度">{{ currentRecord.batteryHealthBefore }}%</el-descriptions-item>
        <el-descriptions-item label="放电后电池健康度">{{ currentRecord.batteryHealthAfter }}%</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentRecord.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentRecord.endTime }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getRecordTypeTag(currentRecord.recordType)">
            {{ getRecordTypeText(currentRecord.recordType) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getV2GRecordList, getV2GStatistics } from '@/api/v2g'

// 统计数据
const statistics = ref({})

// 查询表单
const queryForm = ref({
  userId: '',
  pileId: '',
  dateRange: []
})

// 全量记录（用于本地分页和筛选）
const allRecords = ref([])
const loading = ref(false)

// 分页
const pagination = ref({
  page: 1,
  size: 10
})

// 本地分页：先筛选，再切片
const filteredRecords = computed(() => {
  let list = allRecords.value
  const { userId, pileId, dateRange } = queryForm.value

  if (userId) {
    list = list.filter(r => String(r.userId).includes(userId))
  }
  if (pileId) {
    list = list.filter(r => String(r.pileId).includes(pileId))
  }
  if (dateRange && dateRange.length === 2) {
    const start = new Date(dateRange[0]).getTime()
    const end = new Date(dateRange[1]).getTime() + 86400000  // 含结束日
    list = list.filter(r => {
      const t = r.startTime ? new Date(r.startTime).getTime() : 0
      return t >= start && t <= end
    })
  }
  return list
})

// 当前页展示数据
const records = computed(() => {
  const { page, size } = pagination.value
  const start = (page - 1) * size
  return filteredRecords.value.slice(start, start + size)
})

// 总条数（来自筛选后全量列表）
const total = computed(() => filteredRecords.value.length)

// 详情对话框
const detailVisible = ref(false)
const currentRecord = ref(null)

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getV2GStatistics()
    if (res.code === 200) {
      statistics.value = res.data || {}
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取全量记录列表
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getV2GRecordList()
    if (res.code === 200) {
      allRecords.value = res.data || []
      // 查询时回到第一页
      pagination.value.page = 1
    }
  } catch (error) {
    console.error('获取记录列表失败:', error)
    ElMessage.error('获取记录列表失败')
  } finally {
    loading.value = false
  }
}

// 重置查询
const resetQuery = () => {
  queryForm.value = {
    userId: '',
    pileId: '',
    dateRange: []
  }
  pagination.value.page = 1
}

// 点击查询按钮：重置到第一页（filteredRecords computed 自动响应表单变化）
const handleQuery = () => {
  pagination.value.page = 1
}

// 查看详情
const handleViewDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

// 导出数据
const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

// 获取记录类型文字
const getRecordTypeText = (recordType) => {
  const map = {
    1: '充电',
    2: '放电'
  }
  return map[recordType] || '未知'
}

// 获取记录类型标签颜色
const getRecordTypeTag = (recordType) => {
  const map = {
    1: 'primary',
    2: 'success'
  }
  return map[recordType] || 'info'
}

onMounted(() => {
  fetchStatistics()
  fetchRecords()
})
</script>

<style scoped>
.v2g-management {
  padding: 20px;
}

.statistics-card {
  margin-bottom: 20px;
}

.table-card {
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
