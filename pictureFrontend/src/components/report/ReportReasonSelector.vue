<template>
  <div class="report-reason-selector">
    <div
      v-for="item in reasonList"
      :key="item.value"
      class="reason-card"
      :class="{ active: modelValue === item.value }"
      :style="{ '--reason-color': item.color }"
      @click="emit('update:modelValue', item.value)"
    >
      <component :is="item.iconComp" class="reason-icon" />
      <span class="reason-label">{{ item.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  StopOutlined,
  ThunderboltOutlined,
  WarningOutlined,
  CopyrightOutlined,
  DeleteOutlined,
  FrownOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import {
  REPORT_REASON_MAP,
  REPORT_REASON_SEVERITY,
  REPORT_REASON_COLOR,
} from '@/constants/report'

interface Props {
  modelValue?: string
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const iconMap: Record<string, any> = {
  PORNOGRAPHY: StopOutlined,
  VIOLENCE: ThunderboltOutlined,
  SCAM: WarningOutlined,
  COPYRIGHT: CopyrightOutlined,
  SPAM: DeleteOutlined,
  HARASSMENT: FrownOutlined,
  OTHER: MoreOutlined,
}

const reasonList = Object.keys(REPORT_REASON_MAP).map((key) => ({
  value: key,
  label: REPORT_REASON_MAP[key],
  color: REPORT_REASON_COLOR[REPORT_REASON_SEVERITY[key]],
  iconComp: iconMap[key],
}))
</script>

<style scoped>
.report-reason-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.reason-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 80px;
  height: 72px;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.reason-card:hover {
  border-color: var(--reason-color);
  background: color-mix(in srgb, var(--reason-color) 5%, white);
}
.reason-card.active {
  border-color: var(--reason-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--reason-color) 20%, transparent);
  background: color-mix(in srgb, var(--reason-color) 8%, white);
}
.reason-icon {
  font-size: 22px;
  color: var(--reason-color);
}
.reason-label {
  font-size: 11px;
  color: #333;
  text-align: center;
}
</style>
