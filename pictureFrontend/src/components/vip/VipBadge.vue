<template>
  <span v-if="isVip" :class="['vip-badge', `vip-badge--${size}`]">
    <span v-if="size === 'small'" class="vip-badge-crown">👑</span>
    <span v-else class="vip-badge-pill">VIP</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { userLoginUserStore } from '@/stores/user'

const props = withDefaults(defineProps<{
  size?: 'small' | 'default'
}>(), {
  size: 'default',
})

const loginUserStore = userLoginUserStore()

const isVip = computed(() => {
  const user = loginUserStore.loginUser
  if (user.vipType !== 1) return false
  if (user.vipExpireTime && new Date(user.vipExpireTime).getTime() < Date.now()) return false
  return true
})
</script>

<style scoped>
.vip-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.vip-badge--small {
  width: 18px;
  height: 18px;
  font-size: 12px;
  animation: badge-bounce 300ms ease-out;
}

.vip-badge-crown {
  line-height: 1;
}

.vip-badge--default {
  padding: 1px 8px;
  border-radius: 9999px;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
}

.vip-badge-pill {
  line-height: 1;
}

@keyframes badge-bounce {
  0% {
    transform: scale(0);
  }
  70% {
    transform: scale(1.15);
  }
  100% {
    transform: scale(1);
  }
}
</style>
