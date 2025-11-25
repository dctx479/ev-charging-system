<template>
  <div class="charging-buddy">
    <!-- 顶部导航 -->
    <van-nav-bar
      title="充电搭子"
      left-arrow
      @click-left="$router.back()"
      :fixed="true"
      placeholder
    >
      <template #right>
        <van-icon name="user-o" @click="showOnlineUsers = true" />
      </template>
    </van-nav-bar>

    <!-- 聊天消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-item', msg.senderId === currentUserId ? 'self' : 'other']"
      >
        <div class="message-avatar">
          <van-image
            round
            width="40"
            height="40"
            :src="msg.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
          />
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="nickname">{{ msg.nickname }}</span>
            <span class="time">{{ formatTime(msg.sendTime) }}</span>
          </div>
          <div class="message-bubble">
            <span v-if="msg.messageType === 1">{{ msg.content }}</span>
            <van-image v-else-if="msg.messageType === 2" :src="msg.content" width="150" />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <van-empty
        v-if="messages.length === 0"
        description="暂无消息，快来聊天吧~"
      />
    </div>

    <!-- 输入框 -->
    <div class="input-bar">
      <van-field
        v-model="inputMessage"
        placeholder="说点什么..."
        @keyup.enter="sendMessage"
      >
        <template #button>
          <van-button size="small" type="primary" @click="sendMessage">
            发送
          </van-button>
        </template>
      </van-field>
    </div>

    <!-- 在线用户列表 -->
    <van-popup
      v-model:show="showOnlineUsers"
      position="right"
      :style="{ width: '80%', height: '100%' }"
    >
      <div class="online-users">
        <h3 class="popup-title">在线用户</h3>
        <van-cell-group>
          <van-cell
            v-for="user in onlineUsers"
            :key="user.userId"
            :title="user.nickname"
            :label="`车牌: ${user.carPlate || '未知'}`"
          >
            <template #icon>
              <van-image
                round
                width="40"
                height="40"
                :src="user.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
                style="margin-right: 12px;"
              />
            </template>
            <template #right-icon>
              <van-tag type="success" v-if="user.isCharging">充电中</van-tag>
              <van-tag v-else>等待中</van-tag>
            </template>
          </van-cell>
        </van-cell-group>
        <van-empty v-if="onlineUsers.length === 0" description="暂无在线用户" />
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getChatHistory, getOnlineUsers } from '@/api/chat'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const stationId = ref(route.query.stationId || 1)
const currentUserId = ref(userStore.userId)

// 消息列表
const messages = ref([])
const messageListRef = ref(null)
const MAX_MESSAGES = 200

// 输入框
const inputMessage = ref('')

// 在线用户
const showOnlineUsers = ref(false)
const onlineUsers = ref([])

// WebSocket连接
let ws = null
// 重连相关配置
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 5
const INITIAL_RECONNECT_DELAY = 1000 // 初始重连延迟1秒
let reconnectTimer = null

// 加载聊天历史
const loadChatHistory = async () => {
  try {
    const res = await getChatHistory({ stationId: stationId.value })
    if (res.code === 200) {
      // Map backend ChatMessageVO to frontend message format
      messages.value = (res.data || []).map(msg => ({
        id: msg.messageId,
        senderId: msg.userId,
        nickname: msg.userNickname,
        avatar: msg.userAvatar,
        content: msg.content,
        messageType: msg.messageType,
        sendTime: msg.createTime
      }))
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  }
}

// 加载在线用户
const loadOnlineUsers = async () => {
  try {
    const res = await getOnlineUsers({ stationId: stationId.value })
    if (res.code === 200) {
      onlineUsers.value = res.data || []
    }
  } catch (error) {
    console.error('加载在线用户失败:', error)
  }
}

// 连接WebSocket
const connectWebSocket = () => {
  // 清除可能存在的重连定时器
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  // 检查token
  const token = localStorage.getItem('user_token')
  if (!token) {
    showToast('请先登录')
    return
  }

  // 使用相对路径，通过Vite代理连接
  // 开发环境: ws://localhost:5173/ws/chat (Vite代理到 ws://localhost:8080/ws/chat)
  // 生产环境: 使用当前域名的WebSocket协议
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host

  // 构建WebSocket URL，通过query参数传递token和stationId
  // 后端从query参数提取token进行JWT验证
  const wsUrl = `${protocol}//${host}/ws/chat?stationId=${stationId.value}&token=${encodeURIComponent(token)}`

  console.log('正在连接WebSocket:', wsUrl.replace(/token=.*/, 'token=***'))

  try {
    ws = new WebSocket(wsUrl)
  } catch (error) {
    console.error('创建WebSocket连接失败:', error)
    showToast('连接失败，请检查网络')
    return
  }

  ws.onopen = () => {
    console.log('WebSocket连接成功')
    reconnectAttempts = 0 // 重置重连次数
    showToast('连接成功')
  }

  ws.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)

      // 根据消息类型处理
      if (message.type === 'chat') {
        // 新聊天消息
        messages.value.push({
          id: message.messageId,
          senderId: message.userId,
          nickname: message.userNickname,
          avatar: message.userAvatar,
          content: message.content,
          messageType: 1,
          sendTime: message.createTime
        })
        // 限制消息数量防止内存泄漏
        if (messages.value.length > MAX_MESSAGES) {
          messages.value = messages.value.slice(-MAX_MESSAGES)
        }
        nextTick(() => {
          scrollToBottom()
        })
      } else if (message.type === 'system') {
        // 系统消息（用户加入/离开）
        showToast(message.content)
        loadOnlineUsers()
      }
    } catch (error) {
      console.error('处理WebSocket消息失败:', error, event.data)
    }
  }

  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
    const errorMsg = error.message || '连接出错，正在重试'
    showToast(errorMsg)
  }

  ws.onclose = (event) => {
    console.log(`WebSocket连接关闭: code=${event.code}, reason=${event.reason}`)

    // 检查是否达到最大重连次数
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.log('已达到最大重连次数，停止重连')
      showToast('连接失败次数过多，请刷新页面重试')
      return
    }

    // 使用指数退避策略重连
    const delay = INITIAL_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
    reconnectAttempts++

    console.log(`将在 ${delay}ms 后进行第 ${reconnectAttempts} 次重连`)
    reconnectTimer = setTimeout(() => {
      if (!ws || ws.readyState === WebSocket.CLOSED) {
        connectWebSocket()
      }
    }, delay)
  }
}

// 断开WebSocket连接
const disconnectWebSocket = () => {
  // 清除重连定时器
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  // 关闭连接
  if (ws) {
    // 设置重连次数为最大值，防止onclose回调中重连
    reconnectAttempts = MAX_RECONNECT_ATTEMPTS
    ws.close()
    ws = null
  }
}

// 发送消息
const sendMessage = () => {
  const trimmed = inputMessage.value.trim()
  if (!trimmed) {
    return
  }

  if (trimmed.length > 500) {
    showToast('消息不能超过500个字符')
    return
  }

  if (ws && ws.readyState === WebSocket.OPEN) {
    const message = {
      type: 'chat',
      stationId: stationId.value,
      content: trimmed,
      messageType: 1 // 文本消息
    }

    ws.send(JSON.stringify(message))
    inputMessage.value = ''
  } else {
    showToast('连接已断开，请稍后重试')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''

  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  // 一分钟内显示"刚刚"
  if (diff < 60000) {
    return '刚刚'
  }

  // 一小时内显示"X分钟前"
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }

  // 今天显示"HH:MM"
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 其他显示"MM-DD HH:MM"
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadChatHistory()
  loadOnlineUsers()
  connectWebSocket()
})

onBeforeUnmount(() => {
  disconnectWebSocket()
})

watch(showOnlineUsers, (show) => {
  if (show) {
    loadOnlineUsers()
  }
})
</script>

<style scoped>
.charging-buddy {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 80px;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.self {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-item.self .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #969799;
}

.message-item.self .message-header {
  flex-direction: row-reverse;
}

.nickname {
  font-weight: 500;
}

.message-bubble {
  background: #fff;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-item.self .message-bubble {
  background: #07c160;
  color: #fff;
}

.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebedf0;
  padding: 8px;
}

.online-users {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.popup-title {
  padding: 16px;
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #ebedf0;
}
</style>
