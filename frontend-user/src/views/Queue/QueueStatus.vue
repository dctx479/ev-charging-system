<template>
  <div class="queue-status">
    <van-nav-bar
      title="排队状态"
      left-arrow
      @click-left="onBack"
    />

    <div class="content" v-if="queueInfo">
      <!-- 排队号码显示 -->
      <div class="queue-number-card">
        <div class="queue-number">{{ queueInfo.queueNo }}</div>
        <div class="queue-label">您的排队号</div>
        <van-tag :type="getStatusType(queueInfo.status)" size="large">
          {{ queueInfo.statusText }}
        </van-tag>
      </div>

      <!-- 排队状态信息 -->
      <van-cell-group title="排队信息" inset>
        <van-cell title="充电站" :value="queueInfo.stationName" />
        <van-cell title="当前位置" :value="`第${queueInfo.queuePosition}位`" />
        <van-cell
          v-if="queueInfo.status === 0"
          title="前面排队人数"
          :value="`${queueInfo.peopleAhead}人`"
        />
        <van-cell
          title="预计等待时间"
          :value="`约${queueInfo.estimatedWaitTime}分钟`"
        />
        <van-cell
          title="加入时间"
          :value="formatTime(queueInfo.joinTime)"
        />
        <van-cell
          v-if="queueInfo.callTime"
          title="叫号时间"
          :value="formatTime(queueInfo.callTime)"
        />
      </van-cell-group>

      <!-- 已叫号提示 -->
      <div v-if="queueInfo.status === 1" class="call-notice">
        <van-notice-bar
          :text="noticeText"
          background="#fff7e6"
          color="#ff9800"
          left-icon="volume-o"
        />
      </div>

      <!-- 即将过号警告 -->
      <div v-if="queueInfo.willExpireSoon" class="expire-warning">
        <van-notice-bar
          text="提醒：请尽快到达充电站开始充电，否则将自动过号！"
          background="#ffebee"
          color="#f44336"
          left-icon="warning-o"
        />
      </div>

      <!-- 操作按钮 -->
      <div class="button-group">
        <van-button
          v-if="queueInfo.status === 1"
          type="primary"
          size="large"
          @click="handleStartCharging"
        >
          开始充电
        </van-button>
        <van-button
          v-if="queueInfo.status === 0 || queueInfo.status === 1"
          type="danger"
          size="large"
          plain
          :loading="leaving"
          @click="handleLeaveQueue"
        >
          离开队列
        </van-button>
        <van-button
          v-if="queueInfo.status === 2 || queueInfo.status === 3"
          type="primary"
          size="large"
          @click="goToStation"
        >
          重新排队
        </van-button>
      </div>

      <!-- 提示信息 -->
      <div class="tips">
        <van-cell-group title="温馨提示" inset>
          <van-cell>
            <template #default>
              <div class="tip-content">
                <p>• 叫号后请在15分钟内到达充电站</p>
                <p>• 超时未到将自动过号，需重新排队</p>
                <p>• 请保持手机畅通，以便接收通知</p>
              </div>
            </template>
          </van-cell>
        </van-cell-group>
      </div>
    </div>

    <!-- 无排队记录 -->
    <van-empty
      v-else-if="!loading"
      description="暂无排队记录"
    >
      <van-button type="primary" @click="goToHome">去充电</van-button>
    </van-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getQueueStatus, leaveQueue } from '@/api/queue'
import dayjs from 'dayjs'

const router = useRouter()

const queueInfo = ref(null)
const loading = ref(true)
const leaving = ref(false)

let refreshTimer = null

const noticeText = computed(() => {
  if (!queueInfo.value || queueInfo.value.status !== 1) return ''

  const expireTime = dayjs(queueInfo.value.expireTime)
  const now = dayjs()
  const remaining = expireTime.diff(now, 'minute')

  return `已轮到您充电！请在${remaining}分钟内到达充电站，否则将过号`
})

onMounted(() => {
  loadQueueStatus()
  // 每10秒刷新一次状态
  refreshTimer = setInterval(() => {
    loadQueueStatus()
  }, 10000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})

const loadQueueStatus = async () => {
  try {
    const res = await getQueueStatus()
    queueInfo.value = res.data
    loading.value = false
  } catch (error) {
    if (error.response?.data?.code === 404) {
      // 无排队记录
      queueInfo.value = null
    } else {
      showToast('加载失败')
    }
    loading.value = false
  }
}

const handleLeaveQueue = async () => {
  try {
    await showConfirmDialog({
      title: '确认离开',
      message: '确定要离开队列吗？离开后需要重新排队。'
    })

    leaving.value = true
    await leaveQueue()
    showToast('已离开队列')

    // 返回上一页
    setTimeout(() => {
      router.back()
    }, 1000)
  } catch (error) {
    if (error !== 'cancel') {
      showToast('操作失败')
    }
  } finally {
    leaving.value = false
  }
}

const handleStartCharging = () => {
  // 跳转到创建订单页面
  router.push({
    name: 'CreateOrder',
    query: {
      stationId: queueInfo.value.stationId,
      pileId: queueInfo.value.pileId
    }
  })
}

const goToStation = () => {
  router.push({
    name: 'StationDetail',
    params: { id: queueInfo.value.stationId }
  })
}

const goToHome = () => {
  router.push('/')
}

const onBack = () => {
  router.back()
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'primary',  // 排队中
    1: 'success',  // 已叫号
    2: 'warning',  // 已过号
    3: 'default'   // 已取消
  }
  return typeMap[status] || 'default'
}
</script>

<style scoped lang="scss">
.queue-status {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 20px;

  .content {
    padding: 16px;
  }

  .queue-number-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 40px 20px;
    text-align: center;
    color: white;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);

    .queue-number {
      font-size: 48px;
      font-weight: bold;
      margin-bottom: 8px;
      letter-spacing: 2px;
    }

    .queue-label {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 16px;
    }

    .van-tag {
      background-color: rgba(255, 255, 255, 0.2);
      color: white;
      border: 1px solid rgba(255, 255, 255, 0.3);
      padding: 6px 16px;
    }
  }

  .call-notice {
    margin: 16px 0;
  }

  .expire-warning {
    margin: 16px 0;
  }

  .button-group {
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 0 16px;
  }

  .tips {
    margin-top: 24px;

    .tip-content {
      font-size: 13px;
      color: #666;
      line-height: 1.8;

      p {
        margin: 8px 0;
      }
    }
  }
}
</style>
