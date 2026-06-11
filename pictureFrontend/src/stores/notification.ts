import {
  notificationControllerGetUnreadCount,
  notificationControllerListNotifications,
  notificationControllerMarkAsRead,
  notificationControllerMarkAllAsRead,
  notificationControllerDeleteNotification,
  notificationControllerCleanReadNotifications,
} from '@/api/notificationController'
import { message } from 'ant-design-vue'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref<API.NotificationVO[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const hasMore = ref(true)

  let eventSource: EventSource | null = null
  let pollingTimer: number | null = null

  /**
   * 获取未读数
   */
  async function fetchUnreadCount() {
    try {
      const res = await notificationControllerGetUnreadCount()
      if (res.data.code === 0) {
        unreadCount.value = res.data.data ?? 0
      }
    } catch (e) {
      console.error('获取未读数失败:', e)
    }
  }

  /**
   * 获取通知列表
   */
  async function fetchNotifications(refresh = false) {
    if (refresh) {
      currentPage.value = 1
      notifications.value = []
      hasMore.value = true
    }
    if (!hasMore.value) return

    loading.value = true
    try {
      const res = await notificationControllerListNotifications({
        current: currentPage.value,
        pageSize: 10,
      })
      if (res.data.code === 0 && res.data.data) {
        const records = res.data.data.records ?? []
        if (refresh) {
          notifications.value = records
        } else {
          notifications.value.push(...records)
        }
        hasMore.value = records.length === 10
        currentPage.value++
      }
    } catch (e) {
      console.error('获取通知列表失败:', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * 标记单条已读
   */
  async function markAsRead(id: number) {
    const idx = notifications.value.findIndex((n) => n.id === id)
    if (idx !== -1 && !notifications.value[idx].isRead) {
      // 乐观更新
      notifications.value[idx].isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      try {
        await notificationControllerMarkAsRead({ id })
      } catch {
        // 回滚
        notifications.value[idx].isRead = 0
        unreadCount.value++
      }
    }
  }

  /**
   * 全部标记已读
   */
  async function markAllAsRead() {
    try {
      const res = await notificationControllerMarkAllAsRead()
      if (res.data.code === 0) {
        notifications.value.forEach((n) => (n.isRead = 1))
        unreadCount.value = 0
        message.success('已全部标为已读')
      }
    } catch {
      message.error('操作失败')
    }
  }

  /**
   * 删除通知
   */
  async function deleteNotification(id: number) {
    try {
      const res = await notificationControllerDeleteNotification({ id })
      if (res.data.code === 0) {
        const idx = notifications.value.findIndex((n) => n.id === id)
        if (idx !== -1) {
          if (!notifications.value[idx].isRead) {
            unreadCount.value = Math.max(0, unreadCount.value - 1)
          }
          notifications.value.splice(idx, 1)
        }
      }
    } catch {
      message.error('删除失败')
    }
  }

  /**
   * 清空已读通知
   */
  async function cleanRead() {
    try {
      const res = await notificationControllerCleanReadNotifications()
      if (res.data.code === 0) {
        notifications.value = notifications.value.filter((n) => !n.isRead)
        message.success(`已清除 ${res.data.data} 条已读通知`)
      }
    } catch {
      message.error('操作失败')
    }
  }

  /**
   * 连接 SSE
   */
  function connectSSE() {
    disconnectSSE()
    try {
      eventSource = new EventSource('http://localhost:4040/api/notification/sse', {
        withCredentials: true,
      })

      eventSource.addEventListener('unread', (event) => {
        const count = parseInt(event.data, 10)
        if (!isNaN(count)) {
          unreadCount.value = count
        }
      })

      eventSource.onerror = () => {
        console.warn('SSE 连接失败，降级到轮询')
        disconnectSSE()
        startPolling()
      }
    } catch {
      startPolling()
    }
  }

  /**
   * 断开 SSE
   */
  function disconnectSSE() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    stopPolling()
  }

  /**
   * 开始轮询降级
   */
  function startPolling(interval = 30000) {
    stopPolling()
    pollingTimer = window.setInterval(() => {
      fetchUnreadCount()
    }, interval)
  }

  /**
   * 停止轮询
   */
  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  /**
   * 初始化（登录后调用）
   */
  async function initialize() {
    await Promise.all([fetchUnreadCount(), fetchNotifications(true)])
    connectSSE()
  }

  return {
    unreadCount,
    notifications,
    loading,
    hasMore,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    cleanRead,
    initialize,
    disconnectSSE,
  }
})
