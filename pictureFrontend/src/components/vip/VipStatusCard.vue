<template>
  <div class="vip-status-card" v-if="vipStatus.isVip">
    <div class="vip-status-header">
      <span class="vip-crown">👑</span>
      <span class="vip-title">VIP 会员</span>
    </div>

    <div class="vip-status-body">
      <div class="vip-info-row">
        <span class="vip-info-label">到期时间</span>
        <span class="vip-info-value">{{ formatTime(vipStatus.expireTime) }}</span>
      </div>
      <div class="vip-info-row" v-if="vipStatus.remainingDays != null">
        <span class="vip-info-label">剩余</span>
        <span class="vip-info-value vip-remaining">{{ vipStatus.remainingDays }} 天</span>
      </div>
      <a-progress
        v-if="vipStatus.remainingDays != null && vipStatus.totalDays"
        :percent="Math.min(100, Math.round((vipStatus.remainingDays / vipStatus.totalDays) * 100))"
        :stroke-color="{ '0%': '#FFD700', '100%': '#FFA500' }"
        :show-info="false"
        size="small"
      />
      <div class="vip-info-row" v-if="vipStatus.totalDays">
        <span class="vip-info-label">累计开通</span>
        <span class="vip-info-value">{{ vipStatus.totalDays }} 天</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  vipStatus: API.VipStatusVO
}>()

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}
</script>

<style scoped>
.vip-status-card {
  background: #FFFBEB;
  border: 1px solid #FFE082;
  border-radius: 12px;
  padding: 16px 20px;
}

.vip-status-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.vip-crown {
  font-size: 20px;
}

.vip-title {
  font-size: 16px;
  font-weight: 600;
  color: #B8860B;
}

.vip-status-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.vip-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.vip-info-label {
  color: var(--fg-secondary, #4b5563);
}

.vip-info-value {
  color: var(--fg-primary, #1a1a1a);
  font-weight: 500;
}

.vip-remaining {
  color: #B8860B;
  font-weight: 600;
}
</style>
