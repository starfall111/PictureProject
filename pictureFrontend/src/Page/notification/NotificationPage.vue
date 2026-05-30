<template>
  <div class="notification-page">
    <div class="page-header">
      <h2 class="page-title">消息通知</h2>
      <div class="header-actions">
        <button
          class="action-btn action-btn-outline"
          @click="store.markAllAsRead()"
          :disabled="!store.unreadCount"
        >
          全部已读
        </button>
        <button class="action-btn action-btn-ghost" @click="store.cleanRead()">
          清除已读
        </button>
      </div>
    </div>

    <!-- Pill 形标签筛选栏 -->
    <div class="filter-bar">
      <button
        v-for="item in filterOptions"
        :key="item.value"
        class="filter-tag"
        :class="{ active: typeFilter === item.value }"
        @click="typeFilter = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <NotificationList
      :notifications="filteredNotifications"
      :loading="store.loading"
      @read="store.markAsRead($event)"
      @delete="store.deleteNotification($event)"
      @viewDetail="handleViewDetail"
    />

    <!-- 系统消息详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="系统消息"
      :footer="null"
      :width="520"
      @cancel="detailVisible = false"
    >
      <div v-if="selectedNotification" class="system-message-detail">
        <h3 class="detail-title">{{ selectedNotification.title }}</h3>
        <div class="detail-meta">
          <span v-if="selectedNotification.senderName">发送者：{{ selectedNotification.senderName }}</span>
          <span>{{ dayjs(selectedNotification.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
        </div>
        <a-divider />
        <div class="detail-content">{{ selectedNotification.content }}</div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import dayjs from 'dayjs'
import { useNotificationStore } from '@/stores/notification'
import NotificationList from '@/components/notification/NotificationList.vue'

const store = useNotificationStore()
const typeFilter = ref<string | undefined>(undefined)

// 系统消息详情弹窗
const detailVisible = ref(false)
const selectedNotification = ref<API.NotificationVO | null>(null)

function handleViewDetail(notification: API.NotificationVO) {
  store.markAsRead(notification.id)
  selectedNotification.value = notification
  detailVisible.value = true
}

const filterOptions = [
  { label: '全部', value: undefined as string | undefined },
  { label: '点赞', value: 'LIKE' },
  { label: '收藏', value: 'FAVORITE' },
  { label: '评论', value: 'COMMENT' },
  { label: '关注', value: 'FOLLOW' },
  { label: '系统', value: 'SYSTEM' },
]

const filteredNotifications = computed(() => {
  if (!typeFilter.value) return store.notifications
  return store.notifications.filter((n) => n.type === typeFilter.value)
})

// 加载第一页数据
store.fetchNotifications(true)
</script>

<style scoped>
.notification-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  border: none;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: 9999px;
  transition: all 0.2s;
  white-space: nowrap;
}

.action-btn-outline {
  background: #FFFFFF;
  border: 1px solid var(--border-color, #E5E7EB);
  color: var(--fg-secondary, #4B5563);
}

.action-btn-outline:hover:not(:disabled) {
  border-color: var(--accent, #635BFF);
  color: var(--accent, #635BFF);
}

.action-btn-outline:disabled {
  color: var(--fg-muted, #9CA3AF);
  cursor: not-allowed;
  opacity: 0.6;
}

.action-btn-ghost {
  background: none;
  color: var(--fg-muted, #9CA3AF);
  padding: 6px 10px;
}

.action-btn-ghost:hover {
  color: var(--fg-secondary, #4B5563);
  background-color: var(--surface-secondary, #F7F8FA);
}

.filter-bar {
  display: flex;
  gap: 8px;
  padding: 0 0 16px 0;
  flex-wrap: wrap;
}

.filter-tag {
  border: none;
  height: 28px;
  border-radius: 9999px;
  padding: 0 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  background-color: var(--surface-secondary, #F7F8FA);
  color: var(--fg-secondary, #4B5563);
  font-weight: normal;
}

.filter-tag:hover {
  opacity: 0.85;
}

.filter-tag.active {
  background-color: var(--accent, #635BFF);
  color: #FFFFFF;
  font-weight: 600;
}

.system-message-detail .detail-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
}

.system-message-detail .detail-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--fg-muted, #9CA3AF);
}

.system-message-detail .detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--fg-secondary, #4B5563);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
