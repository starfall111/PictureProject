<template>
  <div class="space-selector">
    <a-select
      :value="modelValue"
      @update:value="handleChange"
      placeholder="请选择保存空间"
      :loading="loading"
      allow-clear
      style="width: 100%"
      :disabled="disabled"
    >
      <a-select-option v-for="space in spaces" :key="space.id" :value="space.id">
        {{ space.spaceName }}（{{ space.totalCount ?? 0 }}/{{ space.maxCount ?? 0 }} 张）
      </a-select-option>
    </a-select>

    <!-- 容量信息 -->
    <div v-if="selectedSpace" class="space-capacity">
      <div class="capacity-row">
        <span class="capacity-label">图片数量</span>
        <a-progress
          :percent="countPercent"
          :size="[160, 8]"
          :stroke-color="countPercent > 80 ? '#ff4d4f' : countPercent > 60 ? '#faad14' : '#1890ff'"
        />
        <span class="capacity-text">{{ selectedSpace.totalCount ?? 0 }} / {{ selectedSpace.maxCount ?? 0 }}</span>
      </div>
      <div class="capacity-row">
        <span class="capacity-label">存储大小</span>
        <a-progress
          :percent="sizePercent"
          :size="[160, 8]"
          :stroke-color="sizePercent > 80 ? '#ff4d4f' : sizePercent > 60 ? '#faad14' : '#1890ff'"
        />
        <span class="capacity-text">{{ formatSize(selectedSpace.totalSize) }} / {{ formatSize(selectedSpace.maxSize) }}</span>
      </div>
    </div>

    <!-- 无空间提示 -->
    <a-button v-if="!loading && spaces.length === 0" type="link" @click="handleCreateSpace">
      还没有空间？去创建
    </a-button>

    <!-- 容量不足警告 -->
    <a-alert
      v-if="insufficient"
      type="error"
      show-icon
      message="空间容量不足，请减少数量或选择其他空间"
      style="margin-top: 8px"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatSize } from '@/util/format'

const props = defineProps<{
  modelValue?: number
  spaces: API.SpaceVO[]
  loading?: boolean
  disabled?: boolean
  /** 请求获取的数量，用于容量不足判断 */
  requestCount?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
  'createSpace': []
}>()

const selectedSpace = computed(() =>
  props.spaces.find(s => s.id === props.modelValue)
)

const countPercent = computed(() => {
  if (!selectedSpace.value?.maxCount) return 0
  return Number(((selectedSpace.value.totalCount ?? 0) / selectedSpace.value.maxCount * 100).toFixed(1))
})

const sizePercent = computed(() => {
  if (!selectedSpace.value?.maxSize) return 0
  return Number(((selectedSpace.value.totalSize ?? 0) / selectedSpace.value.maxSize * 100).toFixed(1))
})

/** 容量不足判断 */
const insufficient = computed(() => {
  if (!selectedSpace.value || !props.requestCount) return false
  const remaining = (selectedSpace.value.maxCount ?? 0) - (selectedSpace.value.totalCount ?? 0)
  return remaining < props.requestCount
})

const handleChange = (value: number | undefined) => {
  emit('update:modelValue', value)
}

const handleCreateSpace = () => {
  emit('createSpace')
}
</script>

<style scoped>
.space-selector {
  width: 100%;
}

.space-capacity {
  margin-top: 8px;
  padding: 8px 0;
}

.capacity-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.capacity-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  white-space: nowrap;
  min-width: 60px;
}

.capacity-text {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  white-space: nowrap;
}

@media (max-width: 640px) {
  .capacity-row {
    flex-wrap: wrap;
  }
}
</style>
