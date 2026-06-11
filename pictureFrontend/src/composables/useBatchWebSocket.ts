import { readonly, ref } from 'vue'

// ==================== 类型定义 ====================

export interface BatchWsMessage {
  type: 'batch_progress' | 'batch_complete'
  taskId: string
  /** 已完成数（成功 + 失败） */
  finished?: number
  /** 总数 */
  total: number
  /** 成功数 */
  success: number
  /** 失败数 */
  fail: number
}

// ==================== 模块级单例状态 ====================

let ws: WebSocket | null = null
let reconnectAttempts = 0
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
const MAX_RECONNECT_ATTEMPTS = 10
const BASE_RECONNECT_DELAY = 1000
const MAX_RECONNECT_DELAY = 30000

const _connected = ref(false)
const messageHandlers = new Set<(msg: BatchWsMessage) => void>()

// ==================== 工具函数 ====================

function getWsUrl(): string {
  return 'ws://localhost:4040/api/ws'
}

function clearReconnectTimer() {
  if (reconnectTimer !== null) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function getReconnectDelay(): number {
  const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
  return Math.min(delay, MAX_RECONNECT_DELAY)
}

// ==================== 连接管理 ====================

function connect() {
  // 已连接或正在连接，跳过
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }

  clearReconnectTimer()

  try {
    ws = new WebSocket(getWsUrl())
  } catch (e) {
    console.error('[BatchWS] 创建连接失败', e)
    scheduleReconnect()
    return
  }

  ws.onopen = () => {
    console.info('[BatchWS] 连接建立')
    _connected.value = true
    reconnectAttempts = 0
  }

  ws.onmessage = (event: MessageEvent) => {
    try {
      const raw = event.data as string
      // 从原始字符串提取 taskId，避免 JSON.parse 大整数精度丢失
      const taskIdMatch = raw.match(/"taskId"\s*:\s*(\d+)/)
      const data = JSON.parse(raw) as BatchWsMessage
      if (taskIdMatch) {
        data.taskId = taskIdMatch[1]
      }
      if (data.type && data.taskId !== undefined) {
        messageHandlers.forEach(handler => {
          try {
            handler(data)
          } catch (e) {
            console.error('[BatchWS] 消息处理器异常', e)
          }
        })
      }
    } catch (e) {
      console.warn('[BatchWS] 消息解析失败', e)
    }
  }

  ws.onclose = (event: CloseStatus) => {
    console.warn('[BatchWS] 连接关闭', { code: event.code, reason: event.reason })
    _connected.value = false
    ws = null
    // 非主动关闭，尝试重连
    if (event.code !== 1000) {
      scheduleReconnect()
    }
  }

  ws.onerror = (event) => {
    console.error('[BatchWS] 连接错误', event)
    // onclose 会随后触发，处理重连逻辑
    _connected.value = false
  }
}

function disconnect() {
  clearReconnectTimer()
  reconnectAttempts = MAX_RECONNECT_ATTEMPTS // 阻止重连
  if (ws) {
    ws.close(1000, '主动断开')
    ws = null
  }
  _connected.value = false
}

function scheduleReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.warn('[BatchWS] 达到最大重连次数，停止重连')
    return
  }
  reconnectAttempts++
  const delay = getReconnectDelay()
  console.info(`[BatchWS] ${delay}ms 后尝试第 ${reconnectAttempts} 次重连`)
  reconnectTimer = setTimeout(() => {
    connect()
  }, delay)
}

// ==================== 消息处理 ====================

function onMessage(handler: (msg: BatchWsMessage) => void) {
  messageHandlers.add(handler)
}

function removeMessageHandler(handler: (msg: BatchWsMessage) => void) {
  messageHandlers.delete(handler)
}

// ==================== Composable ====================

export function useBatchWebSocket() {
  return {
    connected: readonly(_connected),
    connect,
    disconnect,
    onMessage,
    removeMessageHandler,
  }
}
