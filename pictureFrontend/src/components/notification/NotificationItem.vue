<template>
  <div
    class="notification-item"
    :class="{ unread: !notification.isRead }"
    @click="handleClick"
  >
    <div class="notification-avatar">
      <a-avatar :size="36" :src="notification.senderAvatar">
        {{ notification.senderName?.[0] ?? '?' }}
      </a-avatar>
      <span class="type-badge" :style="{ backgroundColor: typeColor }">
        <component :is="typeIcon" class="type-icon-inner" />
      </span>
    </div>
    <div class="notification-content">
      <div class="notification-title">
        <span class="sender-name">{{ notification.senderName }}</span>
        <span class="action-text">{{ notification.title }}</span>
      </div>
      <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
    </div>
    <div class="notification-actions">
      <div v-if="!notification.isRead" class="unread-dot" />
      <a-button
        type="text"
        size="small"
        class="delete-btn"
        @click.stop="$emit('delete', notification.id)"
      >
        <template #icon><DeleteOutlined /></template>
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  HeartFilled,
  StarFilled,
  MessageFilled,
  UserAddOutlined,
  BellOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'

interface Props {
  notification: API.NotificationVO
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'read', id: number): void
  (e: 'delete', id: number): void
  (e: 'viewDetail', notification: API.NotificationVO): void
}>()

const router = useRouter()

const typeConfig: Record<string, { icon: Component; color: string }> = {
  LIKE: { icon: HeartFilled, color: '#ff4d4f' },
  FAVORITE: { icon: StarFilled, color: '#faad14' },
  COMMENT: { icon: MessageFilled, color: '#52c41a' },
  FOLLOW: { icon: UserAddOutlined, color: '#722ed1' },
  SYSTEM: { icon: BellOutlined, color: '#8c8c8c' },
}

const config = computed(() => typeConfig[props.notification.type ?? 'SYSTEM'] ?? typeConfig.SYSTEM)
const typeIcon = computed(() => config.value.icon)
const typeColor = computed(() => config.value.color)

function handleClick() {
  if (!props.notification.isRead) {
    emit('read', props.notification.id)
  }
  // 系统通知：弹出详情弹窗
  if (props.notification.type === 'SYSTEM') {
    emit('viewDetail', props.notification)
    return
  }
  // 其他类型：跳转到关联资源
  if (props.notification.resourceUrl) {
    router.push(props.notification.resourceUrl)
  }
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString()
}
</script>

<style scoped>
.notification-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 12px;
  border-radius: 8px;
}

.notification-item:hover {
  background-color: var(--surface-secondary, #F7F8FA);
}

.notification-item.unread {
  background-color: #E8F6FA;
}

.notification-item.unread:hover {
  background-color: #D8EFF5;
}

.notification-avatar {
  position: relative;
  flex-shrink: 0;
}

.type-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #FFFFFF;
}

.type-icon-inner {
  font-size: 8px;
  color: #FFFFFF;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.sender-name {
  font-weight: 600;
  color: var(--fg-primary, #1A1A1A);
}

.action-text {
  color: var(--fg-secondary, #4B5563);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: var(--fg-muted, #9CA3AF);
  margin-top: 2px;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #33A1C9;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: var(--fg-muted, #9CA3AF);
}

.delete-btn:hover {
  color: var(--fg-secondary, #4B5563);
}

.notification-item:hover .delete-btn {
  opacity: 1;
}
</style>
