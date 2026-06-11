<template>
  <button
    class="grab-btn"
    :class="[statusClass, { clickable: clickable }]"
    :disabled="!clickable"
    @click="clickable && emit('grab')"
  >
    <a-spin v-if="status === 'grabbing'" size="small" />
    <span>{{ btnText }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type GrabBtnStatus = 'before' | 'ongoing' | 'ended' | 'soldout' | 'grabbing'

const props = defineProps<{
  status: GrabBtnStatus
}>()

const emit = defineEmits<{
  grab: []
}>()

const btnText = computed(() => {
  const map: Record<GrabBtnStatus, string> = {
    before: '即将开始',
    ongoing: '立即抢购',
    ended: '已结束',
    soldout: '已售罄',
    grabbing: '排队中...',
  }
  return map[props.status]
})

const clickable = computed(() => props.status === 'ongoing')

const statusClass = computed(() => `grab-btn--${props.status}`)
</script>

<style scoped>
.grab-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  cursor: default;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 200ms ease;
}

.grab-btn--ongoing {
  background: var(--seckill-gradient);
  color: #fff;
  cursor: pointer;
}

.grab-btn--ongoing:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.grab-btn--before,
.grab-btn--ended,
.grab-btn--soldout {
  background: #D9D9D9;
  color: #999;
}

.grab-btn--grabbing {
  background: var(--seckill-gradient);
  color: #fff;
  opacity: 0.85;
}
</style>
