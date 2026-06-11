<template>
  <div class="stock-progress">
    <a-progress
      :percent="percent"
      :show-info="false"
      :stroke-color="strokeColor"
      size="small"
    />
    <div class="stock-text">
      剩余 {{ remaining }} / {{ total }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  remaining: number
}>()

const percent = computed(() => {
  if (props.total === 0) return 0
  return Math.round((props.remaining / props.total) * 100)
})

const status = computed(() => {
  if (percent.value <= 10) return 'danger'
  if (percent.value <= 30) return 'warning'
  return 'normal'
})

const strokeColor = computed(() => {
  if (status.value === 'danger') return '#FF4757'
  if (status.value === 'warning') return '#FFAA00'
  return '#2ED573'
})
</script>

<style scoped>
.stock-progress {
  padding: 8px 0;
}

.stock-text {
  font-size: 13px;
  color: var(--fg-secondary);
  margin-top: 4px;
}
</style>
