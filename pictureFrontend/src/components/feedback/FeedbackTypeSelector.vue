<template>
  <div class="feedback-type-selector">
    <div
      v-for="item in typeList"
      :key="item.value"
      class="type-card"
      :class="{ active: modelValue === item.value }"
      :style="{
        '--type-color': item.color,
      }"
      @click="emit('update:modelValue', item.value)"
    >
      <component :is="item.iconComp" class="type-icon" />
      <span class="type-label">{{ item.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h } from 'vue'
import {
  BugOutlined,
  BulbOutlined,
  UserOutlined,
  HeartOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import { FEEDBACK_TYPE_MAP, FEEDBACK_TYPE_COLOR } from '@/constants/feedback'

interface Props {
  modelValue?: string
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const iconMap: Record<string, any> = {
  BUG: BugOutlined,
  FEATURE: BulbOutlined,
  ACCOUNT: UserOutlined,
  EXPERIENCE: HeartOutlined,
  OTHER: MoreOutlined,
}

const typeList = Object.keys(FEEDBACK_TYPE_MAP).map((key) => ({
  value: key,
  label: FEEDBACK_TYPE_MAP[key],
  color: FEEDBACK_TYPE_COLOR[key],
  iconComp: iconMap[key],
}))
</script>

<style scoped>
.feedback-type-selector {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 90px;
  height: 80px;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.type-card:hover {
  border-color: var(--type-color);
  background: color-mix(in srgb, var(--type-color) 5%, white);
}
.type-card.active {
  border-color: var(--type-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--type-color) 20%, transparent);
  background: color-mix(in srgb, var(--type-color) 8%, white);
}
.type-icon {
  font-size: 24px;
  color: var(--type-color);
}
.type-label {
  font-size: 12px;
  color: #333;
}
@media (max-width: 576px) {
  .type-card {
    width: calc(20% - 10px);
    height: 68px;
  }
}
</style>
