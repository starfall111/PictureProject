<template>
  <div class="detail-hero">
    <div class="hero-image-container">
      <a-image
        :src="url"
        :alt="name"
        :fallback="fallbackImage"
        :wrapper-style="{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%' }"
      >
        <template #placeholder>
          <div class="hero-skeleton">
            <a-skeleton-image active style="width: 100%; height: 100%" />
          </div>
        </template>
      </a-image>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  url?: string
  name?: string
}

defineProps<Props>()

// 加载失败时的兜底 SVG（深色背景 + 提示文字）
const fallbackImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iIzJhMmEyYSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNjY2IiBmb250LXNpemU9IjE0Ij5Mb2FkIGVycm9yPC90ZXh0Pjwvc3ZnPg=='
</script>

<style scoped>
.detail-hero {
  background: #1a1a1a;
  padding: 24px;
  margin: -28px;
  margin-bottom: 0;
}

.hero-image-container {
  max-width: 960px;
  margin: 0 auto;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* --- a-image 包裹层 --- */
.hero-image-container :deep(.ant-image) {
  width: 100%;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* --- a-image 内部 img --- */
.hero-image-container :deep(.ant-image-img) {
  max-height: 75vh;
  max-width: 100%;
  object-fit: contain;
  display: block;
  margin: 0 auto;
  border-radius: 8px;
}

/* --- 预览 hover 遮罩圆角 --- */
.hero-image-container :deep(.ant-image-mask) {
  border-radius: 8px;
}

/* --- 骨架屏 --- */
.hero-skeleton {
  width: 100%;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-skeleton :deep(.ant-skeleton-image) {
  width: 100%;
  height: 400px;
}

@media (max-width: 767px) {
  .detail-hero {
    margin: -12px;
    margin-bottom: 0;
    padding: 16px;
  }

  .hero-image-container :deep(.ant-image) {
    min-height: 250px;
  }

  .hero-image-container :deep(.ant-image-img) {
    max-height: 50vh;
    border-radius: 4px;
  }

  .hero-image-container :deep(.ant-image-mask) {
    border-radius: 4px;
  }

  .hero-skeleton {
    min-height: 250px;
  }

  .hero-skeleton :deep(.ant-skeleton-image) {
    height: 250px;
  }
}
</style>
