<template>
  <a-row :gutter="[16, 16]" class="feedback-stats-cards">
    <a-col v-for="card in cards" :key="card.key" :xs="12" :sm="8" :md="6">
      <a-card
        size="small"
        hoverable
        :loading="loading"
        :class="{ active: activeStatus === card.key }"
        @click="handleClick(card.key)"
      >
        <a-statistic :title="card.label" :value="card.value">
          <template #prefix>
            <component :is="card.icon" :style="{ color: card.color }" />
          </template>
        </a-statistic>
        <a-badge v-if="card.badge" :count="card.badge" :offset="[0, -40]" />
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FileTextOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'

interface Props {
  stats?: API.FeedbackStatsVO
  loading?: boolean
  activeStatus?: string
  /** 管理端模式：显示 P0 紧急 badge */
  admin?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  stats: () => ({}),
  loading: false,
  admin: false,
})

const emit = defineEmits<{
  (e: 'filter', status: string): void
}>()

const cards = computed(() => {
  const s = props.stats
  const base = [
    { key: 'TOTAL', label: '全部', value: s.totalCount ?? 0, icon: FileTextOutlined, color: '#1890ff' },
    { key: 'PENDING', label: '待处理', value: s.pendingCount ?? 0, icon: ClockCircleOutlined, color: '#fa8c16' },
    { key: 'PROCESSING', label: '处理中', value: s.processingCount ?? 0, icon: SyncOutlined, color: '#1890ff' },
    { key: 'RESOLVED', label: '已解决', value: s.resolvedCount ?? 0, icon: CheckCircleOutlined, color: '#52c41a' },
    { key: 'CLOSED', label: '已关闭', value: s.closedCount ?? 0, icon: CloseCircleOutlined, color: '#f5222d' }
  ]
  if (props.admin) {
    base.push(
      { key: 'REJECTED', label: '已拒绝', value: s.rejectedCount ?? 0, icon: CloseCircleOutlined, color: '#f5222d' },
      {
        key: 'URGENT',
        label: 'P0 紧急',
        value: s.p0Count ?? 0,
        icon: CloseCircleOutlined,
        color: '#f5222d',
      },
    )
  }
  return base
})

const handleClick = (key: string) => {
  emit('filter', key)
}
</script>

<style scoped>
.feedback-stats-cards .ant-card {
  transition: all 0.2s;
}
.feedback-stats-cards .ant-card.active {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}
</style>
