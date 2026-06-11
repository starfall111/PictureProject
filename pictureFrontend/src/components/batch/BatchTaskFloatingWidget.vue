<template>
  <Transition name="slide-up">
    <div
      v-if="hasBackgroundTasks"
      class="batch-floating-widget"
      @click="navigateToTask"
      role="button"
      tabindex="0"
      @keyup.enter="navigateToTask"
    >
      <div class="widget-header">
        <a-spin :size="16" />
        <span class="widget-title">批量任务 #{{ latestTask?.taskId }}</span>
      </div>

      <a-progress
        v-if="progress.total > 0"
        :percent="progress.percent"
        :size="['100%', 6]"
        :show-info="false"
        :stroke-color="progress.percent === 100 ? '#52c41a' : '#1677ff'"
        style="margin-top: 8px"
      />

      <div class="widget-footer">
        <span class="widget-count">
          {{ progress.completed }} / {{ progress.total }}
        </span>
        <span class="widget-hint">点击查看</span>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useBatchTaskStore } from '@/stores/batchTask'

const router = useRouter()
const batchTaskStore = useBatchTaskStore()

const hasBackgroundTasks = computed(() => batchTaskStore.hasBackgroundTasks)
const runningTasks = computed(() => batchTaskStore.runningTasks)
const latestTask = computed(() => {
  const tasks = runningTasks.value
  return tasks.length > 0 ? tasks[tasks.length - 1] : null
})

const progress = computed(() => {
  const task = latestTask.value
  if (!task) return { completed: 0, total: 0, percent: 0 }
  const completed = task.wsFinished ?? (task.successCount ?? 0) + (task.failCount ?? 0)
  const total = task.wsTotal ?? task.totalCount ?? 0
  const percent = total > 0 ? Math.round(completed / total * 100) : 0
  return { completed, total, percent }
})

function navigateToTask() {
  const task = latestTask.value
  if (task?.taskId) {
    router.push(`/add_picture/batch/${task.taskId}`)
  }
}
</script>

<style scoped>
.batch-floating-widget {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 200;
  min-width: 200px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.batch-floating-widget:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);
}

.batch-floating-widget:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.widget-title {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.widget-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.widget-count {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.widget-hint {
  font-size: 12px;
  color: #1677ff;
}

/* 进出动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
