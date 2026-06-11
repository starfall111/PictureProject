<template>
  <a-row :gutter="[16, 16]" class="batch-task-stats-cards">
    <a-col v-for="card in cards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="3">
      <a-card
        size="small"
        hoverable
        :loading="loading"
        :class="{ active: activeStatus === card.key }"
        @click="handleClick(card.key)"
      >
        <a-statistic
          :title="card.label"
          :value="card.displayValue"
          :precision="card.precision"
        >
          <template #prefix>
            <component :is="card.icon" :style="{ color: card.color }" />
          </template>
          <template v-if="card.suffix" #suffix>
            {{ card.suffix }}
          </template>
        </a-statistic>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import {
  FileTextOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  RiseOutlined,
  TrophyOutlined,
} from '@ant-design/icons-vue'
import type { API } from '@/api/typings'

const props = withDefaults(defineProps<{
  stats?: API.BatchTaskStatsVO
  loading?: boolean
  activeStatus?: string
}>(), {
  stats: () => ({}),
  loading: false,
  activeStatus: '',
})

const emit = defineEmits<{
  filter: [status: string]
}>()

// 可筛选状态集合（点击卡片可触发筛选）
const filterableKeys = new Set(['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'])

const cards = computed(() => {
  const s = props.stats
  return [
    {
      key: 'TOTAL',
      label: '全部任务',
      displayValue: s.totalCount ?? 0,
      icon: FileTextOutlined,
      color: '#1890ff',
      precision: 0,
    },
    {
      key: 'PENDING',
      label: '待处理',
      displayValue: s.pendingCount ?? 0,
      icon: ClockCircleOutlined,
      color: '#fa8c16',
      precision: 0,
    },
    {
      key: 'PROCESSING',
      label: '处理中',
      displayValue: s.processingCount ?? 0,
      icon: SyncOutlined,
      color: '#1890ff',
      precision: 0,
    },
    {
      key: 'COMPLETED',
      label: '已完成',
      displayValue: s.completedCount ?? 0,
      icon: CheckCircleOutlined,
      color: '#52c41a',
      precision: 0,
    },
    {
      key: 'FAILED',
      label: '已失败',
      displayValue: s.failedCount ?? 0,
      icon: CloseCircleOutlined,
      color: '#f5222d',
      precision: 0,
    },
    {
      key: 'TODAY',
      label: '今日新增',
      displayValue: s.todayNewCount ?? 0,
      icon: RiseOutlined,
      color: '#722ed1',
      precision: 0,
    },
    {
      key: 'SUCCESS_RATE',
      label: '成功率',
      displayValue: (s.successRate ?? 0) * 100,
      icon: TrophyOutlined,
      color: '#52c41a',
      precision: 1,
      suffix: '%',
    },
  ]
})

const handleClick = (key: string) => {
  if (!filterableKeys.has(key)) return
  // 再次点击同一卡片取消筛选
  emit('filter', props.activeStatus === key ? '' : key)
}
</script>

<style scoped>
.batch-task-stats-cards :deep(.ant-card) {
  transition: border-color 0.2s, box-shadow 0.2s;
  cursor: default;
}

.batch-task-stats-cards :deep(.ant-card.hoverable:hover) {
  cursor: default;
}

.batch-task-stats-cards :deep(.ant-card.active) {
  border-color: #33A1C9;
  box-shadow: 0 0 0 2px rgba(51, 161, 201, 0.15);
}
</style>
