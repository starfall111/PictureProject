<template>
  <div class="waterfall-card" @click="doClickPicture">
    <!-- 图片区域（含悬浮遮罩 + 底部信息条） -->
    <div class="waterfall-card-img-wrapper">
      <img class="waterfall-card-img" :alt="picture.name"
        :src="picture.url" @error="onImgError" />
      <!-- hover 遮罩 -->
      <div class="waterfall-card-overlay" />
      <!-- 底部悬浮信息条 -->
      <div class="waterfall-card-hover-bar">
        <div class="hover-bar-name" :title="picture.name">{{ picture.name ?? '未命名' }}</div>
        <div class="hover-bar-row">
          <div class="hover-bar-left" @click.stop="goUserProfile">
            <a-avatar :size="20" :src="picture.userVO?.userAvatar">
              {{ picture.userVO?.userName?.charAt(0) ?? '?' }}
            </a-avatar>
            <span class="hover-bar-nickname">{{ picture.userVO?.userName ?? '匿名' }}</span>
          </div>
          <div class="hover-bar-right">
            <EyeOutlined class="hover-bar-icon" />
            <span class="hover-bar-count">{{ formatCount(picture.socialInfo?.viewCount) }}</span>
            <HeartOutlined class="hover-bar-heart" />
            <span class="hover-bar-like-count">{{ formatCount(picture.socialInfo?.likeCount) }}</span>
          </div>
        </div>
      </div>
    </div>
    <!-- 右上角收藏标识（仅已收藏时显示） -->
    <StarFilled
      v-if="picture.socialInfo?.isFavorited"
      class="waterfall-card-favorite-badge"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HeartOutlined, StarFilled, EyeOutlined } from '@ant-design/icons-vue'
import { formatCount } from '@/utils/formatCount'

interface Props {
  picture: API.PictureVO
}

const props = defineProps<Props>()

const router = useRouter()

const onImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src =
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjIwIiBoZWlnaHQ9IjIyMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjIwIiBoZWlnaHQ9IjIyMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjY2NjIiBmb250LXNpemU9IjE0Ij7lm77niYc8L3RleHQ+PC9zdmc+'
}

const doClickPicture = () => {
  router.push({ path: `/picture/${props.picture.id}` })
}

const goUserProfile = () => {
  if (props.picture.userVO?.id) {
    router.push(`/user/${props.picture.userVO.id}`)
  }
}
</script>

<style scoped>
/* ===== 卡片容器 ===== */
.waterfall-card {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.3s ease;
  background: #fff;
}

.waterfall-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

/* ===== 图片区域 ===== */
.waterfall-card-img-wrapper {
  position: relative;
  width: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

.waterfall-card-img {
  width: 100%;
  display: block;
  object-fit: cover;
}

/* ===== 悬浮遮罩 ===== */
.waterfall-card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0);
  transition: background 0.3s ease;
  pointer-events: none;
  z-index: 1;
}

.waterfall-card:hover .waterfall-card-overlay {
  background: linear-gradient(to top, rgba(0, 0, 0, 0.45) 0%, rgba(0, 0, 0, 0.1) 50%, transparent 100%);
}

/* ===== 底部悬浮信息条 ===== */
.waterfall-card-hover-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 12px 8px;
  background: transparent;
  transform: translateY(100%);
  opacity: 0;
  transition: transform 0.25s ease, opacity 0.25s ease;
  z-index: 2;
}

.waterfall-card:hover .waterfall-card-hover-bar {
  transform: translateY(0);
  opacity: 1;
}

.hover-bar-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.hover-bar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hover-bar-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.hover-bar-left:hover {
  opacity: 0.75;
}

.hover-bar-nickname {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.hover-bar-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 8px;
}

.hover-bar-icon {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.hover-bar-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1;
}

.hover-bar-heart {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.hover-bar-like-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1;
}

/* ===== 右上角收藏标识 ===== */
.waterfall-card-favorite-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 16px;
  color: #faad14;
  z-index: 3;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
  pointer-events: none;
}

/* ===== 移动端：始终显示信息条 ===== */
@media (hover: none) {
  .waterfall-card-hover-bar {
    transform: translateY(0);
    opacity: 1;
  }

  .waterfall-card-overlay {
    background: linear-gradient(to top, rgba(0, 0, 0, 0.4) 0%, transparent 100%);
  }
}

/* ===== 瀑布流卡片飞入动画 ===== */
.animate__animated {
  animation-fill-mode: both;
  animation-duration: 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
