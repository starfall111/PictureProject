<template>
  <div class="batch-card">
    <div class="batch-tag">
      <a-tag :color="couponTypeColor">{{ couponTypeName }}</a-tag>
    </div>

    <CountdownTimer
      v-if="isUpcoming"
      :target-time="batch.startTime!"
      @start="emit('countdownEnd')"
    />
    <StockProgress
      v-else
      :total="batch.totalStock ?? 0"
      :remaining="batch.remainStock ?? 0"
    />

    <h4 class="batch-name">{{ batch.name }}</h4>
    <div class="batch-meta">
      <span>总量: {{ batch.totalStock }}</span>
      <span v-if="batch.remainStock != null">剩余: {{ batch.remainStock }}</span>
    </div>
    <div class="batch-time" v-if="batch.endTime">
      结束: {{ formatTime(batch.endTime) }}
    </div>
    <div class="batch-time" v-else>抢完即止</div>

    <GrabButton
      :status="btnStatus"
      @grab="emit('grab', batch.id!)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StockProgress from './StockProgress.vue'
import CountdownTimer from './CountdownTimer.vue'
import GrabButton from './GrabButton.vue'
import { COUPON_TYPE_MAP, BATCH_STATUS } from '@/constants/seckill'

const props = defineProps<{
  batch: API.PublicBatchVO
  grabbingBatchId: number | null
}>()

const emit = defineEmits<{
  grab: [batchId: number]
  countdownEnd: []
}>()

const couponTypeName = computed(() => {
  return COUPON_TYPE_MAP[props.batch.type ?? 0]?.text ?? 'VIP券'
})

const couponTypeColor = computed(() => {
  return COUPON_TYPE_MAP[props.batch.type ?? 0]?.color ?? '#1677ff'
})

const isUpcoming = computed(() => {
  if (props.batch.status === BATCH_STATUS.ONGOING) return false
  if (props.batch.status === BATCH_STATUS.DRAFT || props.batch.status === BATCH_STATUS.PREHEATING) {
    return props.batch.startTime ? new Date(props.batch.startTime) > new Date() : true
  }
  return false
})

const btnStatus = computed(() => {
  if (props.grabbingBatchId != null && props.grabbingBatchId === props.batch.id) return 'grabbing'
  if (props.batch.status === BATCH_STATUS.ENDED || props.batch.status === BATCH_STATUS.CANCELLED) return 'ended'
  if (isUpcoming.value) return 'before'
  if ((props.batch.remainStock ?? 0) <= 0) return 'soldout'
  return 'ongoing'
})

function formatTime(time: string | number) {
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.batch-card {
  background: var(--surface-primary, #fff);
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.06));
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.batch-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.1));
}

.batch-tag {
  margin-bottom: 12px;
}

.batch-name {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: var(--fg-primary);
}

.batch-meta {
  font-size: 13px;
  color: var(--fg-secondary);
  display: flex;
  gap: 12px;
}

.batch-time {
  font-size: 13px;
  color: var(--fg-muted);
  margin: 4px 0 16px;
}
</style>
