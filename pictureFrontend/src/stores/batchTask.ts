import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { message } from 'ant-design-vue'
import { useBatchWebSocket, type BatchWsMessage } from '@/composables/useBatchWebSocket'
import { pictureControllerPictureUploadByBatch } from '@/api/pictureController'
import { batchTaskControllerGetTask, batchTaskControllerListTasks } from '@/api/batchTaskController'

// ==================== 类型 ====================

/** 扩展 BatchTaskVO，附加 WebSocket 实时字段 */
export interface EnrichedBatchTaskVO extends API.BatchTaskVO {
  wsFinished?: number
  wsTotal?: number
  wsSuccess?: number
  wsFail?: number
}

// ==================== Store ====================

export const useBatchTaskStore = defineStore('batchTask', () => {
  // ---------- 状态 ----------
  const tasks = ref<Record<string, EnrichedBatchTaskVO>>({})
  const activeTaskIds = ref<string[]>([])
  const viewingTaskId = ref<string | null>(null)

  // WebSocket + 轮询
  const ws = useBatchWebSocket()
  let wsHandler: ((msg: BatchWsMessage) => void) | null = null
  let pollTimer: ReturnType<typeof setInterval> | null = null
  let initialized = false

  // ---------- Computed ----------

  /** 当前活跃（运行中）的任务列表 */
  const runningTasks = computed(() =>
    activeTaskIds.value.map(id => tasks.value[id]).filter(Boolean),
  )

  /** 是否有后台运行的任务（用户未在查看的） */
  const hasBackgroundTasks = computed(() => {
    if (activeTaskIds.value.length === 0) return false
    // 所有活跃任务都不是当前查看的
    return activeTaskIds.value.every(id => id !== viewingTaskId.value)
  })

  // ---------- Actions ----------

  /** 获取单个任务 */
  function getTask(taskId: string) {
    return computed(() => tasks.value[taskId])
  }

  /** 提交新任务 */
  async function submitTask(dto: API.PictureUploadByBatchDTO): Promise<API.BatchTaskVO> {
    const res = await pictureControllerPictureUploadByBatch(dto)
    if (res.data.code === 0 && res.data.data) {
      const taskVO = res.data.data
      const key = String(taskVO.taskId!)
      tasks.value[key] = { ...taskVO }
      if (taskVO.status === 'PENDING' || taskVO.status === 'PROCESSING') {
        addActiveTask(key)
      }
      return taskVO
    }
    throw new Error(res.data.message ?? '提交失败')
  }

  /** 从 API 拉取单个任务 */
  async function fetchTask(taskId: string) {
    try {
      const res = await batchTaskControllerGetTask({ taskId: taskId as unknown as number })
      if (res.data.code === 0 && res.data.data) {
        const taskVO = res.data.data
        const existing = tasks.value[taskId]
        tasks.value[taskId] = {
          ...taskVO,
          // 保留 WebSocket 实时字段（如果有的话）
          wsFinished: existing?.wsFinished,
          wsTotal: existing?.wsTotal,
          wsSuccess: existing?.wsSuccess,
          wsFail: existing?.wsFail,
        }
        syncActiveStatus(taskId, taskVO.status)
      }
    } catch (e) {
      console.error('[BatchTaskStore] fetchTask 失败', e)
    }
  }

  /** 拉取用户所有任务 */
  async function fetchAllTasks() {
    try {
      const res = await batchTaskControllerListTasks()
      if (res.data.code === 0 && res.data.data) {
        const list = res.data.data
        for (const taskVO of list) {
          const key = String(taskVO.taskId!)
          const existing = tasks.value[key]
          tasks.value[key] = {
            ...taskVO,
            wsFinished: existing?.wsFinished,
            wsTotal: existing?.wsTotal,
            wsSuccess: existing?.wsSuccess,
            wsFail: existing?.wsFail,
          }
          syncActiveStatus(key, taskVO.status)
        }
      }
    } catch (e) {
      console.error('[BatchTaskStore] fetchAllTasks 失败', e)
    }
  }

  /** 设置当前查看的任务 ID */
  function setViewingTaskId(id: string | null) {
    viewingTaskId.value = id
  }

  /** 初始化：连接 WebSocket + 拉取任务列表 */
  async function initialize() {
    if (initialized) return
    initialized = true

    // 注册 WebSocket 消息处理器
    wsHandler = handleWsMessage
    ws.onMessage(wsHandler)

    // 连接 WebSocket
    ws.connect()

    // 拉取已有任务，恢复未完成的
    await fetchAllTasks()

    // 启动轮询兜底
    startPolling()
  }

  /** 断开连接 */
  function disconnectWs() {
    if (wsHandler) {
      ws.removeMessageHandler(wsHandler)
      wsHandler = null
    }
    ws.disconnect()
    stopPolling()
    initialized = false
  }

  // ---------- WebSocket 消息处理 ----------

  function handleWsMessage(msg: BatchWsMessage) {
    const task = tasks.value[msg.taskId]

    if (msg.type === 'batch_progress') {
      if (task) {
        task.wsFinished = msg.finished
        task.wsTotal = msg.total
        task.wsSuccess = msg.success
        task.wsFail = msg.fail
        task.status = 'PROCESSING'
      } else {
        // 未知任务，拉取一次
        fetchTask(msg.taskId)
      }
    } else if (msg.type === 'batch_complete') {
      if (task) {
        task.successCount = msg.success
        task.failCount = msg.fail
        task.totalCount = msg.total
        task.status = 'COMPLETED'
      }
      removeActiveTask(msg.taskId)

      // 弹出通知
      if (msg.fail === 0) {
        message.success(`批量任务完成: 全部 ${msg.success} 张图片获取成功`)
      } else {
        message.warning(`批量任务完成: ${msg.success} 成功, ${msg.fail} 失败`)
      }
    }
  }

  // ---------- 活跃任务管理 ----------

  function addActiveTask(taskId: string) {
    if (!activeTaskIds.value.includes(taskId)) {
      activeTaskIds.value.push(taskId)
    }
  }

  function removeActiveTask(taskId: string) {
    activeTaskIds.value = activeTaskIds.value.filter(id => id !== taskId)
  }

  function syncActiveStatus(taskId: string, status?: string) {
    if (status === 'PENDING' || status === 'PROCESSING') {
      addActiveTask(taskId)
    } else {
      removeActiveTask(taskId)
    }
  }

  // ---------- 轮询兜底 ----------

  function startPolling() {
    if (pollTimer) return
    pollTimer = setInterval(() => {
      // WebSocket 已连接且正常，跳过轮询
      if (ws.connected.value) return
      // 只轮询活跃任务
      for (const taskId of activeTaskIds.value) {
        fetchTask(taskId)
      }
    }, 5000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return {
    // 状态
    tasks,
    activeTaskIds,
    viewingTaskId,
    // Computed
    runningTasks,
    hasBackgroundTasks,
    getTask,
    // Actions
    submitTask,
    fetchTask,
    fetchAllTasks,
    setViewingTaskId,
    initialize,
    disconnectWs,
  }
})
