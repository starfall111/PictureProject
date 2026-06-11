<template>
  <div class="batch-result-card">
    <!-- 空闲状态 -->
    <div v-if="status === 'idle'" class="result-idle">
      <a-typography-text type="secondary">
        配置完成后点击开始抓取
      </a-typography-text>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="status === 'loading'" class="result-loading">
      <a-spin size="large" />
      <p style="margin-top: 16px; color: rgba(0, 0, 0, 0.45)">
        {{ isPending ? '任务排队中...' : '正在抓取图片...' }}
      </p>

      <!-- WebSocket 实时进度 -->
      <div v-if="wsProgress" class="ws-progress">
        <a-progress
          :percent="wsPercent"
          :size="[240, 10]"
          :stroke-color="wsPercent === 100 ? '#52c41a' : undefined"
        />
        <p class="ws-text">
          已获取 {{ wsProgress.completed }} / {{ wsProgress.total }} 张
        </p>
        <div v-if="wsProgress.success > 0 || wsProgress.fail > 0" class="ws-stats">
          <a-tag color="success">{{ wsProgress.success }} 成功</a-tag>
          <a-tag v-if="wsProgress.fail > 0" color="error">{{ wsProgress.fail }} 失败</a-tag>
        </div>
      </div>
    </div>

    <!-- 成功状态 -->
    <a-result
      v-else-if="status === 'success'"
      status="success"
      :title="resultCount === 0 ? '未找到匹配图片' : '抓取完成'"
      :sub-title="successSubtitle"
    >
      <template #extra>
        <a-button v-if="spaceId && resultCount > 0" type="primary" :href="`/space/${spaceId}`" target="_blank">
          查看空间
        </a-button>
        <a-button @click="emit('reset')">继续抓取</a-button>
      </template>
    </a-result>

    <!-- 失败状态 -->
    <a-result
      v-else-if="status === 'error'"
      status="error"
      title="抓取失败"
      :sub-title="errorMessage"
    >
      <template #extra>
        <a-button type="primary" @click="emit('retry')">重试</a-button>
        <a-button @click="emit('reset')">重新配置</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EnrichedBatchTaskVO } from '@/stores/batchTask'

export interface WsProgress {
  completed: number
  total: number
  success: number
  fail: number
}

const props = defineProps<{
  status: 'idle' | 'loading' | 'success' | 'error'
  resultCount?: number
  errorMessage?: string
  spaceName?: string
  spaceId?: number
  /** WebSocket 实时进度 */
  wsProgress?: WsProgress
  /** 任务详情（查看模式） */
  task?: EnrichedBatchTaskVO
}>()

const emit = defineEmits<{
  'retry': []
  'reset': []
}>()

/** 是否处于 PENDING 状态（排队中） */
const isPending = computed(() => props.task?.status === 'PENDING')

const wsPercent = computed(() => {
  if (!props.wsProgress || !props.wsProgress.total) return 0
  return Math.round(props.wsProgress.completed / props.wsProgress.total * 100)
})

const successSubtitle = computed(() => {
  if (!props.task) {
    return props.resultCount === 0
      ? '请更换关键字后重试'
      : `共成功获取 ${props.resultCount} 张图片`
  }
  const success = props.task.successCount ?? 0
  const fail = props.task.failCount ?? 0
  if (fail > 0) {
    return `成功 ${success} 张，失败 ${fail} 张`
  }
  return `共成功获取 ${success} 张图片`
})
</script>

<style scoped>
.batch-result-card {
  padding: 24px;
  border-radius: 12px;
  background: var(--surface-primary, #fff);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
}

.result-idle {
  text-align: center;
  padding: 32px 16px;
}

.result-loading {
  text-align: center;
  padding: 32px 16px;
}

.ws-progress {
  margin-top: 16px;
}

.ws-text {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.ws-stats {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
