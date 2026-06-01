<template>
  <a-popover
    v-if="!isMobile"
    v-model:open="popoverOpen"
    trigger="click"
    placement="bottomRight"
    :overlay-style="{ padding: 0 }"
  >
    <template #content>
      <NotificationPanel @viewDetail="handleViewDetail" />
    </template>
    <div class="notification-bell" @click="handleClick">
      <a-badge :count="unreadCount" :overflow-count="99" :offset="[-4, 4]">
        <BellOutlined class="bell-icon" />
      </a-badge>
    </div>
  </a-popover>
  <div v-else class="notification-bell" @click="goToNotificationPage">
    <a-badge :count="unreadCount" :overflow-count="99" :offset="[-4, 4]">
      <BellOutlined class="bell-icon" />
    </a-badge>
  </div>

  <!-- 系统消息详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="系统消息"
    :footer="null"
    :width="520"
    @cancel="detailVisible = false"
  >
    <div v-if="detailNotification" class="system-message-detail">
      <h3 class="detail-title">{{ detailNotification.title }}</h3>
      <div class="detail-meta">
        <span v-if="detailNotification.senderName">发送者：{{ detailNotification.senderName }}</span>
        <span>{{ dayjs(detailNotification.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </div>
      <a-divider />
      <div class="detail-content">{{ detailNotification.content }}</div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { BellOutlined } from '@ant-design/icons-vue'
import { Grid } from 'ant-design-vue'
import { useNotificationStore } from '@/stores/notification'
import { userLoginUserStore } from '@/stores/user'
import NotificationPanel from './NotificationPanel.vue'

const router = useRouter()
const store = useNotificationStore()
const loginUserStore = userLoginUserStore()
const screens = Grid.useBreakpoint()
const isMobile = computed(() => !screens.value.md)

const unreadCount = computed(() => store.unreadCount)
const popoverOpen = ref(false)

// 系统消息详情弹窗
const detailVisible = ref(false)
const detailNotification = ref<API.NotificationVO | null>(null)

function handleViewDetail(notification: API.NotificationVO) {
  store.markAsRead(notification.id)
  popoverOpen.value = false
  detailNotification.value = notification
  detailVisible.value = true
}

function handleClick() {
  // 桌面端：Popover 自动打开面板，同时刷新数据
  store.fetchNotifications(true)
}

function goToNotificationPage() {
  router.push('/notifications')
}

onMounted(() => {
  if (loginUserStore.loginUser.id) {
    store.initialize()
  }
})

onUnmounted(() => {
  store.disconnectSSE()
})
</script>

<style scoped>
.notification-bell {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.notification-bell:hover {
  background-color: var(--surface-secondary, #F7F8FA);
}

.bell-icon {
  font-size: 18px;
  color: var(--fg-secondary, #4B5563);
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
