<template>
  <div class="report-target-info" v-if="target">
    <!-- 图片类型 -->
    <div v-if="target.type === 'PICTURE'" class="target-picture">
      <a-image
        :src="target.thumbnailUrl"
        :width="64"
        :height="64"
        class="target-thumb"
        fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN88P/BfwAJhAPk2iMa1AAAAABJRU5ErkJggg=="
      />
      <div class="target-detail">
        <span class="target-title">{{ target.title || '图片' }}</span>
        <span class="target-author" v-if="target.authorName">作者：{{ target.authorName }}</span>
      </div>
    </div>
    <!-- 用户类型 -->
    <div v-else-if="target.type === 'USER'" class="target-user">
      <a-avatar :src="target.authorAvatar" :size="40">
        {{ target.authorName?.charAt(0) }}
      </a-avatar>
      <span class="target-title">{{ target.authorName || '用户' }}</span>
    </div>
    <!-- 兜底 -->
    <div v-else class="target-unknown">
      <span>{{ target.title || '未知对象' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { API } from '@/api/typings'

interface Props {
  target?: API.ReportTargetVO
}

defineProps<Props>()
</script>

<style scoped>
.report-target-info {
  display: inline-flex;
  align-items: center;
}
.target-picture {
  display: flex;
  align-items: center;
  gap: 12px;
}
.target-thumb {
  border-radius: 6px;
  object-fit: cover;
}
.target-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.target-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.target-title {
  font-weight: 500;
  font-size: 14px;
}
.target-author {
  font-size: 12px;
  color: #999;
}
</style>
