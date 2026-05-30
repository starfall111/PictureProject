<template>
  <div class="notification-panel">
    <div class="panel-header">
      <span class="panel-title">通知</span>
      <button class="mark-all-btn" @click="handleMarkAllRead" :disabled="!hasUnread">
        全部已读
      </button>
    </div>
    <div class="panel-body">
      <div v-if="loading && notifications.length === 0" class="panel-loading">
        <a-spin />
      </div>
      <template v-else-if="notifications.length > 0">
        <NotificationItem
          v-for="item in notifications.slice(0, 5)"
          :key="item.id"
          :notification="item"
          @read="handleRead"
          @delete="handleDelete"
          @viewDetail="handleViewDetail"
        />
      </template>
      <div v-else class="panel-empty">
        <BellOutlined class="empty-icon" />
        <span class="empty-text">暂无通知</span>
      </div>
    </div>
    <div class="panel-footer">
      <button class="view-all-btn" @click="goToNotificationPage">
        {{ notifications.length > 0 ? '查看全部' : '进入消息中心' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { BellOutlined } from '@ant-design/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import NotificationItem from './NotificationItem.vue'

const emit = defineEmits<{
  (e: 'viewDetail', notification: API.NotificationVO): void
}>()

const router = useRouter()
const store = useNotificationStore()

const notifications = computed(() => store.notifications)
const loading = computed(() => store.loading)
const hasUnread = computed(() => store.unreadCount > 0)

function handleRead(id: number) {
  store.markAsRead(id)
}

function handleDelete(id: number) {
  store.deleteNotification(id)
}

function handleMarkAllRead() {
  store.markAllAsRead()
}

function handleViewDetail(notification: API.NotificationVO) {
  emit('viewDetail', notification)
}

function goToNotificationPage() {
  router.push('/notifications')
}
</script>

<style scoped>
.notification-panel {
  width: 400px;
  max-height: 500px;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color, #E5E7EB);
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
}

.mark-all-btn {
  border: none;
  background: none;
  font-size: 12px;
  font-weight: 500;
  color: var(--accent, #635BFF);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 9999px;
  transition: background-color 0.2s;
}

.mark-all-btn:hover:not(:disabled) {
  background-color: var(--surface-secondary, #F7F8FA);
}

.mark-all-btn:disabled {
  color: var(--fg-muted, #9CA3AF);
  cursor: not-allowed;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  max-height: 380px;
  padding: 4px;
}

.panel-loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  gap: 8px;
}

.empty-icon {
  font-size: 32px;
  color: var(--fg-muted, #9CA3AF);
}

.empty-text {
  font-size: 13px;
  color: var(--fg-muted, #9CA3AF);
}

.panel-footer {
  border-top: 1px solid var(--border-color, #E5E7EB);
  padding: 8px 16px;
}

.view-all-btn {
  width: 100%;
  border: 1px solid var(--border-color, #E5E7EB);
  background: #FFFFFF;
  border-radius: 9999px;
  padding: 6px 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--fg-secondary, #4B5563);
  cursor: pointer;
  transition: all 0.2s;
}

.view-all-btn:hover {
  border-color: var(--accent, #635BFF);
  color: var(--accent, #635BFF);
  background-color: var(--surface-secondary, #F7F8FA);
}
</style>
