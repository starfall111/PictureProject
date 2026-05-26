<template>
  <div class="social-actions" @click.stop>
    <!-- 点赞 -->
    <span class="social-btn" :class="{ active: isLiked }" @click="handleLike">
      <HeartFilled v-if="isLiked" class="social-icon liked" />
      <HeartOutlined v-else class="social-icon" />
      <span class="social-count">{{ likeCount }}</span>
    </span>
    <!-- 收藏 -->
    <span class="social-btn" :class="{ active: isFavorited }" @click="handleFavorite">
      <StarFilled v-if="isFavorited" class="social-icon favorited" />
      <StarOutlined v-else class="social-icon" />
      <span class="social-count">{{ favoriteCount }}</span>
    </span>
    <!-- 分享 -->
    <span class="social-btn" @click="handleShare">
      <ShareAltOutlined class="social-icon" />
      <span class="social-count">分享</span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { HeartOutlined, HeartFilled, StarOutlined, StarFilled, ShareAltOutlined } from '@ant-design/icons-vue'
import { listCategoryUsingGet } from '@/api/categoryController';
import { recordShareUsingPost, toggleFavoriteUsingPost, toggleLikeUsingPost } from '@/api/pictureController';

interface Props {
  pictureId: number
  likeCount?: number
  favoriteCount?: number
  isLiked?: boolean
  isFavorited?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  likeCount: 0,
  favoriteCount: 0,
  isLiked: false,
  isFavorited: false,
})

const emit = defineEmits<{
  (e: 'update:isLiked', val: boolean): void
  (e: 'update:isFavorited', val: boolean): void
  (e: 'update:likeCount', val: number): void
  (e: 'update:favoriteCount', val: number): void
  (e: 'share', pictureId: number): void
}>()

const likeCount = ref(props.likeCount)
const favoriteCount = ref(props.favoriteCount)
const isLiked = ref(props.isLiked)
const isFavorited = ref(props.isFavorited)

const handleLike = async () => {
  try {
    // TODO: 替换为真实 API
    const res = await toggleLikeUsingPost(
      { pictureId: props.pictureId }
    )
    if (res.data.code === 0) {
      isLiked.value = !isLiked.value
      likeCount.value += isLiked.value ? 1 : -1
      emit('update:isLiked', isLiked.value)
      emit('update:likeCount', likeCount.value)
    }
  } catch {
    message.error('操作失败')
  }
}

const handleFavorite = async () => {
  try {
    // TODO: 替换为真实 API
    const res = await toggleFavoriteUsingPost(
      { pictureId: props.pictureId }
    )
    if (res.data.code === 0) {
      isFavorited.value = !isFavorited.value
      favoriteCount.value += isFavorited.value ? 1 : -1
      emit('update:isFavorited', isFavorited.value)
      emit('update:favoriteCount', favoriteCount.value)
    }
  } catch {
    message.error('操作失败')
  }
}

const handleShare = async () => {
  emit('share', props.pictureId)
  // todo 允许虚空分享 后端要么直接不居鲁分享方式，要么前端直接进行处理，倾向于后端只记录分享数，不记录分享方式
  // const res = await recordShareUsingPost({
  //   pictureId: props.pictureId
  // })



}
</script>

<style scoped>
.social-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #8c8c8c;
  font-size: 18px;
  transition: transform 0.2s ease;
  user-select: none;
}

.social-btn:hover {
  transform: scale(1.1);
}

.social-btn.active .social-icon {
  animation-duration: 0.4s;
}

.social-icon {
  font-size: 20px;
  transition: color 0.2s ease;
}

.social-icon.liked {
  color: #ff4d4f;
  animation: heartbeat 0.4s ease;
}

.social-icon.favorited {
  color: #faad14;
  animation: starlight 0.4s ease;
}

.social-count {
  font-size: 14px;
}

@keyframes heartbeat {
  0% {
    transform: scale(1);
  }

  25% {
    transform: scale(1.3);
  }

  50% {
    transform: scale(0.9);
  }

  75% {
    transform: scale(1.15);
  }

  100% {
    transform: scale(1);
  }
}

@keyframes starlight {
  0% {
    transform: scale(1) rotate(0deg);
  }

  25% {
    transform: scale(1.3) rotate(15deg);
  }

  50% {
    transform: scale(0.9) rotate(-10deg);
  }

  75% {
    transform: scale(1.15) rotate(5deg);
  }

  100% {
    transform: scale(1) rotate(0deg);
  }
}
</style>
