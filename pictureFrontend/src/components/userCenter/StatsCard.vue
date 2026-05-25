<template>
  <a-card :bordered="false" class="stats-card">
    <a-row :gutter="16">
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ uploadCount }}</div>
        <div class="stat-label">上传图片</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ favoriteCount }}</div>
        <div class="stat-label">收藏数</div>
      </a-col>
      <a-col :span="8" class="stat-item">
        <div class="stat-value">{{ viewCount }}</div>
        <div class="stat-label">浏览数</div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
interface Props {
  uploadCount: number;
  favoriteCount: number;
  viewCount: number;
}

defineProps<Props>();
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
