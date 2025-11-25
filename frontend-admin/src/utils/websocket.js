/**
 * WebSocket 工具类
 */
class WebSocketClient {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectTimer = null
    this.reconnectDelay = 3000
    this.messageHandlers = []
  }

  connect() {
    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功')
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
      }

      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        this.messageHandlers.forEach(handler => handler(data))
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error)
      }

      this.ws.onclose = () => {
        console.log('WebSocket 连接关闭')
        this.reconnect()
      }
    } catch (error) {
      console.error('WebSocket 连接失败:', error)
      this.reconnect()
    }
  }

  reconnect() {
    if (this.reconnectTimer) return

    this.reconnectTimer = setTimeout(() => {
      console.log('尝试重新连接 WebSocket...')
      this.connect()
    }, this.reconnectDelay)
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  onMessage(handler) {
    this.messageHandlers.push(handler)
  }

  close() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    if (this.ws) {
      this.ws.close()
    }
  }
}

export default WebSocketClient
