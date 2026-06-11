<template>
  <div class="degrade-control">
    <div class="degrade-header">
      <span class="degrade-label">当前降级等级:</span>
      <a-select
        :value="currentLevel"
        style="width: 160px"
        @change="handleChange"
      >
        <a-select-option v-for="(v, k) in DEGRADE_LEVEL_MAP" :key="k" :value="Number(k)">
          {{ v.text }}
        </a-select-option>
      </a-select>
    </div>

    <div class="degrade-levels">
      <div
        v-for="(v, k) in DEGRADE_LEVEL_MAP"
        :key="k"
        class="level-item"
        :class="{ 'level-item--active': Number(k) === currentLevel }"
      >
        <a-tag :color="v.color">{{ k }}</a-tag>
        <span class="level-desc">{{ getLevelDesc(Number(k)) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Modal, message } from 'ant-design-vue'
import { adminVipControllerSetDegradeLevel } from '@/api/adminVipController'
import { DEGRADE_LEVEL_MAP } from '@/constants/vip'

const props = defineProps<{
  currentLevel: number
}>()

const emit = defineEmits<{
  change: [level: number]
}>()

function getLevelDesc(level: number) {
  const descs: Record<number, string> = {
    0: '正常运行',
    1: '跳过 Redis 限流，降级为 DB 限流',
    2: 'MQ 降级，同步写 DB',
    3: '暂停抢购，仅可查询结果',
    4: '全部降级',
  }
  return descs[level] ?? ''
}

function handleChange(level: number) {
  Modal.confirm({
    title: '确认修改降级等级',
    content: `将降级等级修改为「${DEGRADE_LEVEL_MAP[level]?.text ?? level}」，确认？`,
    async onOk() {
      try {
        const res = await adminVipControllerSetDegradeLevel({ level })
        if (res.data.code === 0) {
          message.success('修改成功')
          emit('change', level)
        } else {
          message.error(res.data.message || '修改失败')
        }
      } catch (e: any) {
        message.error(e?.message || '修改失败')
      }
    },
  })
}
</script>

<style scoped>
.degrade-control {
  max-width: 500px;
}

.degrade-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.degrade-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--fg-primary, #1a1a1a);
}

.degrade-levels {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.level-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--surface-primary, #fff);
  border: 1px solid transparent;
}

.level-item--active {
  border-color: var(--accent, #1677ff);
  background: var(--accent-light, #e6f4ff);
}

.level-desc {
  font-size: 13px;
  color: var(--fg-secondary, #4b5563);
}
</style>
