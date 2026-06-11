<template>
  <div class="countdown">
    <span class="countdown-label">距开始</span>
    <div class="countdown-digits">
      <span class="digit">{{ days }}</span><span class="sep">天</span>
      <span class="digit">{{ hours }}</span><span class="sep">:</span>
      <span class="digit">{{ minutes }}</span><span class="sep">:</span>
      <span class="digit">{{ seconds }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  targetTime: string
}>()

const emit = defineEmits<{
  start: []
}>()

const days = ref('00')
const hours = ref('00')
const minutes = ref('00')
const seconds = ref('00')

let timer: ReturnType<typeof setInterval> | null = null

function update() {
  const target = new Date(props.targetTime).getTime()
  const now = Date.now()
  const diff = target - now

  if (diff <= 0) {
    days.value = '00'
    hours.value = '00'
    minutes.value = '00'
    seconds.value = '00'
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    emit('start')
    return
  }

  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)

  days.value = String(d).padStart(2, '0')
  hours.value = String(h).padStart(2, '0')
  minutes.value = String(m).padStart(2, '0')
  seconds.value = String(s).padStart(2, '0')
}

onMounted(() => {
  update()
  timer = setInterval(update, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.countdown {
  text-align: center;
  padding: 8px 0;
}

.countdown-label {
  font-size: 13px;
  color: var(--fg-muted);
  margin-bottom: 4px;
  display: block;
}

.countdown-digits {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.digit {
  font-size: 28px;
  font-weight: 700;
  color: var(--fg-primary);
  font-variant-numeric: tabular-nums;
}

.sep {
  font-size: 20px;
  color: var(--fg-secondary);
}
</style>
