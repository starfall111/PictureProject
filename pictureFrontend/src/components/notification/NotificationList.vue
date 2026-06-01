<template>
  <div class="notification-list">
    <template v-if="groupedNotifications.length > 0">
      <div v-for="group in groupedNotifications" :key="group.label">
        <div class="time-group-label">{{ group.label }}</div>
        <NotificationItem
          v-for="item in group.items"
          :key="item.id"
          :notification="item"
          @read="$emit('read', $event)"
          @delete="$emit('delete', $event)"
          @viewDetail="$emit('viewDetail', $event)"
        />
      </div>
    </template>
    <a-empty v-else-if="!loading" description="暂无通知" />
    <div v-if="loading" class="list-loading">
      <a-spin />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import NotificationItem from './NotificationItem.vue'

interface Props {
  notifications: API.NotificationVO[]
  loading?: boolean
}

const props = defineProps<Props>()
defineEmits<{
  (e: 'read', id: number): void
  (e: 'delete', id: number): void
  (e: 'viewDetail', notification: API.NotificationVO): void
}>()

interface NotificationGroup {
  label: string
  items: API.NotificationVO[]
}

const groupedNotifications = computed(() => {
  const groups: NotificationGroup[] = []
  const today: API.NotificationVO[] = []
  const yesterday: API.NotificationVO[] = []
  const earlier: API.NotificationVO[] = []
  const now = new Date()

  for (const n of props.notifications) {
    if (!n.createTime) {
      earlier.push(n)
      continue
    }
    const date = new Date(n.createTime)
    const diffDays = Math.floor(
      (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
    )
    if (diffDays === 0) {
      today.push(n)
    } else if (diffDays === 1) {
      yesterday.push(n)
    } else {
      earlier.push(n)
    }
  }

  if (today.length > 0) groups.push({ label: '今天', items: today })
  if (yesterday.length > 0) groups.push({ label: '昨天', items: yesterday })
  if (earlier.length > 0) groups.push({ label: '更早', items: earlier })

  return groups
})
</script>

<style scoped>
.notification-list {
  min-height: 200px;
}

.time-group-label {
  padding: 8px 16px;
  font-size: 12px;
  color: var(--fg-muted, #9CA3AF);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-group-label::before {
  content: '';
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background-color: var(--accent, #635BFF);
  flex-shrink: 0;
}

.list-loading {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>
