<template>
  <div class="stats-grid">
    <div
      v-for="card in cards"
      :key="card.key"
      class="stat-card"
      :class="{ 'stat-card--active': activeFilter === card.key }"
      @click="emit('filter', card.key)"
    >
      <div class="stat-value">{{ card.value }}</div>
      <div class="stat-label">{{ card.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  stats: { total: number; ongoing: number; preheating: number; ended: number; cancelled: number }
  loading: boolean
  activeFilter: number | null
}>()

const emit = defineEmits<{
  filter: [status: number | null]
}>()

const cards = computed(() => [
  { key: null as number | null, label: '总批次', value: props.stats.total },
  { key: 2 as number | null, label: '进行中', value: props.stats.ongoing },
  { key: 1 as number | null, label: '预热中', value: props.stats.preheating },
  { key: 3 as number | null, label: '已结束', value: props.stats.ended },
  { key: 4 as number | null, label: '已取消', value: props.stats.cancelled },
])
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--surface-primary, #fff);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 150ms ease, background-color 150ms ease;
}

.stat-card:hover {
  background: var(--surface-secondary, #f7f8fa);
}

.stat-card--active {
  border-color: var(--accent, #1677ff);
  background: var(--accent-light, #e6f4ff);
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--fg-primary, #1a1a1a);
}

.stat-label {
  font-size: 13px;
  color: var(--fg-secondary, #4b5563);
  margin-top: 4px;
}
</style>
