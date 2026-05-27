<template>
  <div class="space-banner">
    <div class="space-banner-info">
      <h2 class="space-banner-title">{{ space.spaceName }}</h2>
      <!-- <p v-if="space.spaceDesc" class="space-banner-desc">{{ space.spaceDesc }}</p> -->
    </div>
    <div class="space-banner-actions">
      <a-button type="primary" :href="`/add_picture?spaceId=${spaceId}`" target="_blank">
        + 创建图片
      </a-button>
      <div class="space-banner-capacity">
        <div class="capacity-label">
          已用 {{ totalCount }} / {{ space.maxCount ?? 0 }} 张
        </div>
        <a-progress
          :percent="capacityPercent"
          :strokeColor="'var(--accent, #33A1C9)'"
          :showInfo="false"
          size="small"
        />
        <a-tooltip :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
          <div class="capacity-size">
            {{ formatSize(space.totalSize) }} / {{ formatSize(space.maxSize) }}
          </div>
        </a-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  space: API.SpaceVO
  spaceId: string | number
  totalCount: number
}

const props = defineProps<Props>()

const capacityPercent = computed(() => {
  if (!props.space.maxCount) return 0
  return Number(((props.totalCount / props.space.maxCount) * 100).toFixed(1))
})

const formatSize = (size: number | string | undefined): string => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let s = Number(size)
  if (isNaN(s)) return '0 B'
  while (s >= 1024 && i < units.length - 1) {
    s /= 1024
    i++
  }
  return `${s.toFixed(1)} ${units[i]}`
}
</script>

<style scoped>
.space-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--surface-primary, #FFFFFF);
  border-radius: var(--radius-card, 12px);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
}

.space-banner-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
  margin: 0 0 4px;
}

.space-banner-desc {
  font-size: 14px;
  color: var(--fg-secondary, #4B5563);
  margin: 0;
}

.space-banner-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.space-banner-capacity {
  min-width: 180px;
}

.capacity-label {
  font-size: 12px;
  color: var(--fg-muted, #9CA3AF);
  margin-bottom: 4px;
}

.capacity-size {
  font-size: 11px;
  color: var(--fg-muted, #9CA3AF);
  margin-top: 2px;
  cursor: help;
}

@media (max-width: 640px) {
  .space-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
