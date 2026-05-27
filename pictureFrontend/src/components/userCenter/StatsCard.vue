<template>
  <a-card :bordered="false" class="stats-card">
    <a-row :gutter="[16, 16]">
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(uploadCount) }}</div>
        <div class="stat-label">上传</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(likeCount) }}</div>
        <div class="stat-label">获赞</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(favoriteCount) }}</div>
        <div class="stat-label">收藏</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(viewCount) }}</div>
        <div class="stat-label">浏览</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(shareCount) }}</div>
        <div class="stat-label">分享</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ formatCount(downloadCount) }}</div>
        <div class="stat-label">下载</div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { formatCount } from '@/utils/formatCount'

interface Props {
  uploadCount?: number;
  likeCount?: number;
  favoriteCount?: number;
  viewCount?: number;
  shareCount?: number;
  downloadCount?: number;
}

withDefaults(defineProps<Props>(), {
  uploadCount: 0,
  likeCount: 0,
  favoriteCount: 0,
  viewCount: 0,
  shareCount: 0,
  downloadCount: 0,
});
</script>

<style scoped>
.stats-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  transition: box-shadow 0.3s ease;
}

.stats-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.5;
}
</style>
