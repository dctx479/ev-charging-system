/**
 * WebSocket 工具类 - 增强版
 * 功能：自动重连、指数退避重连延迟、重连次数限制、内存泄漏防护
 */
class WebSocketClient {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectTimer = null
    this.reconnectDelay = 3000  // 初始重连延迟：3秒
    this.messageHandlers = []

    // 重连次数限制
    this.maxReconnectAttempts = 5
    this.reconnectAttempts = 0

    // 手动关闭标记（防止手动关闭后继续重连）
    this.manuallyClosed = false
  }

  connect() {
    // 如果已手动关闭，不再重连
    if (this.manuallyClosed) {
      console.log('WebSocket 已手动关闭，不再重连')
      return
    }

    // 如果重连次数超过限制，停止重连
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`WebSocket 重连失败次数已达上限（${this.maxReconnectAttempts}次），停止重连`)
      return
    }

    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功')
        // 重置重连计数
        this.reconnectAttempts = 0

        // 清除重连定时器
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.messageHandlers.forEach(handler => handler(data))
        } catch (error) {
          console.error('WebSocket 消息解析失败:', error)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error)
      }

      this.ws.onclose = () => {
        console.log('WebSocket 连接关闭')

        // 只有非手动关闭时才尝试重连
        if (!this.manuallyClosed) {
          this.reconnect()
        }
      }
    } catch (error) {
      console.error('WebSocket 连接失败:', error)

      // 只有非手动关闭时才尝试重连
      if (!this.manuallyClosed) {
        this.reconnect()
      }
    }
  }

  reconnect() {
    // 防止重复创建定时器
    if (this.reconnectTimer) return

    // 如果已手动关闭，不再重连
    if (this.manuallyClosed) return

    // 如果重连次数超过限制，停止重连
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`WebSocket 重连失败次数已达上限（${this.maxReconnectAttempts}次），停止重连`)
      return
    }

    this.reconnectAttempts++

    // 指数退避策略：延迟时间 = 初始延迟 × 2^(重连次数-1)，最大不超过 30 秒
    // 重连序列：3s -> 6s -> 12s -> 24s -> 30s(max)
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
      30000
    )

    console.log(`WebSocket 第 ${this.reconnectAttempts} 次重连，延迟 ${delay}ms`)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    } else {
      console.warn('WebSocket 未连接，无法发送消息')
    }
  }

  onMessage(handler) {
    if (typeof handler === 'function') {
      this.messageHandlers.push(handler)
    }
  }

  /**
   * 关闭WebSocket连接并清理资源
   * @param {boolean} manual - 是否为手动关闭
   */
  close(manual = true) {
    // 标记为手动关闭
    this.manuallyClosed = manual

    // 清除重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    // 关闭WebSocket连接
    if (this.ws) {
      // 移除事件监听器，防止内存泄漏
      this.ws.onopen = null
      this.ws.onmessage = null
      this.ws.onerror = null
      this.ws.onclose = null

      // 关闭连接
      if (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING) {
        this.ws.close()
      }

      this.ws = null
    }

    // 清空消息处理器
    this.messageHandlers = []

    console.log('WebSocket 已关闭并清理资源')
  }

  /**
   * 重置WebSocket状态（用于需要重新连接的场景）
   */
  reset() {
    this.manuallyClosed = false
    this.reconnectAttempts = 0
  }
}

export default WebSocketClient
