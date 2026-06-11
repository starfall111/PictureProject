<template>
  <div class="coupon-card" :class="statusClass">
    <div class="coupon-left" :style="{ background: leftBgColor }">
      <span class="coupon-left-label">VIP</span>
      <span class="coupon-left-days">{{ coupon.type }}天</span>
    </div>
    <div class="coupon-right">
      <div class="coupon-header">
        <h4 class="coupon-name">{{ coupon.typeName || `${coupon.type}天VIP券` }}</h4>
        <a-tag :color="statusTagColor">{{ coupon.statusName }}</a-tag>
      </div>
      <div class="coupon-info">
        <span class="coupon-code" :title="coupon.code">券编码: {{ coupon.code }}</span>
      </div>
      <div class="coupon-info">
        <span>领取时间: {{ formatTime(coupon.issuedAt) }}</span>
      </div>
      <div class="coupon-info" v-if="coupon.status === COUPON_STATUS.CLAIMED && coupon.remainingDays != null">
        <span class="coupon-remaining">剩余 {{ coupon.remainingDays }} 天可用</span>
      </div>
      <div class="coupon-info" v-else-if="coupon.activatedAt">
        <span>激活时间: {{ formatTime(coupon.activatedAt) }}</span>
      </div>
      <div class="coupon-info" v-if="coupon.expireAt">
        <span>到期时间: {{ formatTime(coupon.expireAt) }}</span>
      </div>
      <div class="coupon-action" v-if="coupon.status === COUPON_STATUS.CLAIMED">
        <button class="activate-btn" @click="emit('activate', coupon.id!)">立即激活</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { COUPON_STATUS, COUPON_STATUS_MAP } from '@/constants/coupon'
import { COUPON_TYPE_MAP } from '@/constants/seckill'

const props = defineProps<{
  coupon: API.CouponVO
}>()

const emit = defineEmits<{
  activate: [couponId: number]
}>()

const leftBgColor = computed(() => {
  if (props.coupon.status === COUPON_STATUS.EXPIRED) return '#D9D9D9'
  return COUPON_TYPE_MAP[props.coupon.type ?? 0]?.color ?? '#1677ff'
})

const statusClass = computed(() => {
  if (props.coupon.status === COUPON_STATUS.EXPIRED) return 'coupon-card--expired'
  if (props.coupon.status === COUPON_STATUS.ACTIVATED) return 'coupon-card--activated'
  return 'coupon-card--unused'
})

const statusTagColor = computed(() => {
  return COUPON_STATUS_MAP[props.coupon.status ?? 0]?.color ?? 'default'
})

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.coupon-card {
  display: flex;
  background: var(--surface-primary, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.06));
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.coupon-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.1));
}

.coupon-card--expired {
  opacity: 0.7;
}

.coupon-card--unused {
  background: var(--coupon-bg-unused);
}

.coupon-card--activated {
  background: var(--coupon-bg-activated);
}

.coupon-left {
  width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-weight: 700;
}

.coupon-left-label {
  font-size: 14px;
}

.coupon-left-days {
  font-size: 16px;
  margin-top: 2px;
}

.coupon-right {
  flex: 1;
  padding: 16px 20px;
}

.coupon-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.coupon-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--fg-primary);
}

.coupon-info {
  font-size: 13px;
  color: var(--fg-secondary);
  margin-bottom: 4px;
}

.coupon-code {
  font-family: 'Courier New', monospace;
  max-width: 200px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.coupon-remaining {
  color: var(--seckill-primary);
  font-weight: 500;
}

.coupon-action {
  margin-top: 12px;
  text-align: right;
}

.activate-btn {
  background: var(--seckill-gradient);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.activate-btn:hover {
  filter: brightness(1.1);
  box-shadow: var(--shadow-md);
}

@media (max-width: 768px) {
  .coupon-left {
    width: 60px;
  }

  .coupon-left-label {
    font-size: 12px;
  }

  .coupon-left-days {
    font-size: 14px;
  }
}
</style>
