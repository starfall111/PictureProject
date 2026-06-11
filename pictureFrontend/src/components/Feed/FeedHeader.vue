<template>
  <div class="feed-header">
    <div class="feed-header-left">
      <h2 class="feed-title">动态</h2>
      <a-badge
        v-if="unreadCount > 0"
        :count="unreadCount"
        :overflow-count="99"
      />
    </div>
    <a-button
      v-if="unreadCount > 0"
      type="link"
      size="small"
      :loading="markingRead"
      @click="handleMarkRead"
    >
      全部已读
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { feedControllerMarkRead } from '@/api/feedController'
import { message } from 'ant-design-vue'

interface Props {
  unreadCount?: number
}

withDefaults(defineProps<Props>(), {
  unreadCount: 0,
})

const emit = defineEmits<{
  (e: 'read'): void
}>()

const markingRead = ref(false)

const handleMarkRead = async () => {
  markingRead.value = true
  try {
    const res = await feedControllerMarkRead()
    if (res.data.code === 0) {
      emit('read')
      message.success('已全部标记为已读')
    }
  } catch {
    message.error('操作失败')
  } finally {
    markingRead.value = false
  }
}
</script>

<style scoped>
.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.feed-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.feed-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
  margin: 0;
}
</style>
