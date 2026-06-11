<template>
  <a-modal
    :open="visible"
    title="确认激活"
    :width="420"
    centered
    @cancel="emit('cancel')"
  >
    <template #footer>
      <a-button @click="emit('cancel')">取消</a-button>
      <a-button type="primary" :loading="loading" @click="emit('confirm', coupon!.id!)">确认激活</a-button>
    </template>
    <div class="confirm-content" v-if="coupon">
      <p>您即将激活「<strong>{{ coupon.typeName || `${coupon.type}天VIP券` }}</strong>」</p>
      <p>激活后将立即生效，有效期 {{ coupon.type }} 天</p>
      <div class="vip-benefits">
        <p class="benefits-title">VIP 权益：</p>
        <ul>
          <li>创建专业版空间</li>
          <li>更多存储容量</li>
        </ul>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  coupon: API.CouponVO | null
  loading?: boolean
}>()

const emit = defineEmits<{
  confirm: [couponId: number]
  cancel: []
}>()
</script>

<style scoped>
.confirm-content p {
  margin: 8px 0;
  color: var(--fg-primary);
}

.vip-benefits {
  margin-top: 12px;
  padding: 12px;
  background: var(--surface-secondary);
  border-radius: 8px;
}

.benefits-title {
  font-weight: 500;
  margin-bottom: 8px;
}

.vip-benefits ul {
  margin: 0;
  padding-left: 20px;
}

.vip-benefits li {
  color: var(--fg-secondary);
  font-size: 13px;
  margin-bottom: 4px;
}
</style>
