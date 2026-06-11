<template>
  <div class="stats-grid">
    <div v-for="card in cards" :key="card.label" class="stat-card">
      <div class="stat-value">{{ card.value }}</div>
      <div class="stat-label">{{ card.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  stats: API.VipStatsVO
  loading: boolean
}>()

const cards = computed(() => [
  { label: 'VIP总数', value: props.stats.totalVipUsers ?? 0 },
  { label: '活跃VIP', value: props.stats.activeVipUsers ?? 0 },
  { label: '今日新增', value: props.stats.todayNewVip ?? 0 },
  { label: '即将过期', value: props.stats.expiringVipUsers ?? 0 },
])
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--surface-primary, #fff);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
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
