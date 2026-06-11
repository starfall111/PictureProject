<template>
  <a-spin :spinning="loading">
    <div class="stats-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-label">{{ card.label }}</div>
      </div>
    </div>

    <div class="rate-section" v-if="stats">
      <div class="rate-item">
        <span class="rate-label">领取率</span>
        <a-progress
          :percent="formatPercent(stats.claimRate)"
          :stroke-color="'#1677ff'"
          :format="() => formatPercent(stats.claimRate) + '%'"
        />
      </div>
      <div class="rate-item">
        <span class="rate-label">激活率</span>
        <a-progress
          :percent="formatPercent(stats.activationRate)"
          :stroke-color="'#52c41a'"
          :format="() => formatPercent(stats.activationRate) + '%'"
        />
      </div>
    </div>
  </a-spin>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { adminBatchControllerGetSeckillStats } from '@/api/adminBatchController'

const stats = ref<API.SeckillStatsVO | null>(null)
const loading = ref(false)

async function fetchStats() {
  loading.value = true
  try {
    const res = await adminBatchControllerGetSeckillStats()
    if (res.data.code === 0) {
      stats.value = res.data.data ?? null
    }
  } finally {
    loading.value = false
  }
}

function formatPercent(val?: number) {
  if (val == null) return 0
  return Math.round(val * 100) / 100
}

const cards = computed(() => [
  { label: '总批次数', value: stats.value?.totalBatches ?? 0 },
  { label: '总券数', value: stats.value?.totalCoupons ?? 0 },
  { label: '已领取', value: stats.value?.claimedCount ?? 0 },
  { label: '已激活', value: stats.value?.activatedCount ?? 0 },
  { label: '已过期', value: stats.value?.expiredCount ?? 0 },
])

onMounted(() => {
  fetchStats()
})
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

.rate-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 500px;
}

.rate-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rate-label {
  font-size: 13px;
  color: var(--fg-secondary, #4b5563);
  white-space: nowrap;
  min-width: 60px;
}
</style>
