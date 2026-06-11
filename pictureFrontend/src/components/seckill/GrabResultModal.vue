<template>
  <a-modal
    :open="visible"
    :footer="null"
    :width="400"
    centered
    @cancel="emit('close')"
  >
    <div class="result-content" v-if="result">
      <div class="result-icon" :class="result.success ? 'success' : 'fail'">
        <CheckCircleFilled v-if="result.success" />
        <CloseCircleFilled v-else />
      </div>
      <h3 class="result-title">{{ result.success ? '抢购成功！' : '抢购失败' }}</h3>
      <p class="result-message">{{ result.success ? successMessage : result.message }}</p>
      <div class="result-actions">
        <a-button @click="emit('close')">
          {{ result.success ? '继续抢购' : '返回' }}
        </a-button>
        <a-button
          v-if="result.success"
          type="primary"
          @click="emit('viewCoupon')"
        >
          查看我的券包
        </a-button>
        <a-button
          v-if="!result.success"
          type="primary"
          @click="emit('retry')"
        >
          重新抢购
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons-vue'
import type { GrabResult } from '@/composables/useSeckill'
import { COUPON_TYPE_MAP } from '@/constants/seckill'

const props = defineProps<{
  visible: boolean
  result: GrabResult | null
}>()

const emit = defineEmits<{
  close: []
  viewCoupon: []
  retry: []
}>()

const successMessage = computed(() => {
  if (!props.result?.couponType) return '恭喜获得优惠券！'
  const typeName = COUPON_TYPE_MAP[props.result.couponType]?.text ?? 'VIP优惠券'
  return `您已获得 ${typeName}`
})
</script>

<style scoped>
.result-content {
  text-align: center;
  padding: 16px 0;
}

.result-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.result-icon.success {
  color: #52c41a;
}

.result-icon.fail {
  color: #ff4d4f;
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.result-message {
  font-size: 14px;
  color: var(--fg-secondary);
  margin: 0 0 24px;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
