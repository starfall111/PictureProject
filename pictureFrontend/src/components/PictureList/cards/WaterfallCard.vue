<template>
  <div class="waterfall-card" @click="doClickPicture">
    <!-- 图片区域 -->
    <div class="waterfall-card-img-wrapper">
      <img class="waterfall-card-img" :alt="picture.name"
        :src="picture.url" @error="onImgError" />
    </div>
    <!-- 用户信息 -->
    <div class="waterfall-card-info">
      <div class="waterfall-card-user">
        <a-avatar :size="36" :src="picture.userVO?.userAvatar">
          {{ picture.userVO?.userName?.charAt(0) ?? '?' }}
        </a-avatar>
        <span class="waterfall-card-nickname">{{ picture.userVO?.userName ?? '匿名' }}</span>
      </div>
      <div class="waterfall-card-name" :title="picture.name">{{ picture.name ?? '未命名' }}</div>
    </div>
    <!-- 社交按钮 -->
    <div class="waterfall-card-social">
      <SocialActions :pictureId="picture.id!" :likeCount="picture.socialInfo?.likeCount ?? 0"
        :favoriteCount="picture.socialInfo?.favoriteCount ?? 0" :isLiked="picture.socialInfo?.isLiked ?? false"
        :isFavorited="picture.socialInfo?.isFavorited ?? false" @share="handleShare" />
    </div>
    <!-- 分享弹窗 -->
    <ShareModal v-model:open="shareModalOpen" :picture="picture" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SocialActions from '@/components/SocialActions.vue'
import ShareModal from '@/components/ShareModal.vue'

interface Props {
  picture: API.PictureVO
}

const props = defineProps<Props>()

const router = useRouter()
const shareModalOpen = ref(false)

const onImgError = () => {
  // 图片加载失败时无需特殊处理
}

const doClickPicture = () => {
  router.push({ path: `/picture/${props.picture.id}` })
}

const handleShare = () => {
  shareModalOpen.value = true
}
</script>

<style scoped>
.waterfall-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.3s ease;
  margin-bottom: 0;
}

.waterfall-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.waterfall-card-img-wrapper {
  position: relative;
  width: 100%;
  background: #f5f5f5;
}

.waterfall-card-img {
  width: 100%;
  display: block;
  object-fit: cover;
}

.waterfall-card-info {
  padding: 12px 16px 8px;
}

.waterfall-card-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.waterfall-card-nickname {
  font-size: 18px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.waterfall-card-name {
  font-size: 19px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.waterfall-card-social {
  padding: 0 16px 12px;
}

/* 瀑布流卡片飞入动画 */
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
