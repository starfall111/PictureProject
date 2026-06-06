<template>
  <div class="feedback-timeline">
    <a-timeline v-if="logs?.length">
      <a-timeline-item v-for="log in logs" :key="log.id" :color="getTimelineColor(log)">
        <div class="timeline-item">
          <div class="timeline-status">
            <a-tag v-if="log.fromStatus" :color="getStatusColor(log.fromStatus)">
              {{ getStatusLabel(log.fromStatus) }}
            </a-tag>
            <span v-if="log.fromStatus && log.toStatus" class="arrow">→</span>
            <a-tag v-if="log.toStatus" :color="getStatusColor(log.toStatus)">
              {{ getStatusLabel(log.toStatus) }}
            </a-tag>
          </div>
          <div class="timeline-meta">
            <a-tag size="small" :color="getOperatorColor(log.operatorType)">
              {{ getOperatorLabel(log.operatorType) }}
            </a-tag>
            <span class="timeline-remark" v-if="log.remark">{{ log.remark }}</span>
            <span class="timeline-time">{{ formatTime(log.createTime) }}</span>
          </div>
        </div>
      </a-timeline-item>
    </a-timeline>
    <a-empty v-else description="暂无状态变更记录" />
  </div>
</template>

<script setup lang="ts">
import type { API } from '@/api/typings'
import { FEEDBACK_STATUS_MAP, FEEDBACK_STATUS_COLOR } from '@/constants/feedback'
import { formatTime } from '@/utils/formatTime'

interface Props {
  logs?: API.FeedbackStatusLogVO[]
}

withDefaults(defineProps<Props>(), {
  logs: () => [],
})

const getStatusLabel = (status?: string) =>
  FEEDBACK_STATUS_MAP[status ?? ''] ?? status ?? ''

const getStatusColor = (status?: string) =>
  FEEDBACK_STATUS_COLOR[status ?? ''] ?? 'default'

const getOperatorLabel = (type?: string) => {
  const map: Record<string, string> = { USER: '用户', ADMIN: '管理员', SYSTEM: '系统' }
  return map[type ?? ''] ?? type ?? ''
}

const getOperatorColor = (type?: string) => {
  const map: Record<string, string> = { USER: 'blue', ADMIN: 'green', SYSTEM: 'default' }
  return map[type ?? ''] ?? 'default'
}

const getTimelineColor = (log: API.FeedbackStatusLogVO) => {
  if (log.toStatus === 'RESOLVED') return 'green'
  if (log.toStatus === 'REJECTED') return 'red'
  if (log.toStatus === 'PENDING') return 'orange'
  return 'blue'
}
</script>

<style scoped>
.feedback-timeline {
  padding: 8px 0;
}
.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.timeline-status {
  display: flex;
  align-items: center;
  gap: 4px;
}
.arrow {
  color: #999;
}
.timeline-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.timeline-remark {
  font-size: 12px;
  color: #666;
}
.timeline-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}
</style>
