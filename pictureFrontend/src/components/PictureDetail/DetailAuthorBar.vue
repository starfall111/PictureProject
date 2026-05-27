<template>
  <div class="author-bar" @click="goUserProfile">
    <a-avatar :size="40" :src="userVO?.userAvatar">
      {{ userVO?.userName?.charAt(0) ?? '?' }}
    </a-avatar>
    <div class="author-text">
      <div class="author-name-row">
        <span class="author-name">{{ userVO?.userName ?? '匿名用户' }}</span>
        <span class="author-dot">·</span>
        <a-tag v-if="categoryName" color="blue">{{ categoryName }}</a-tag>
      </div>
      <p v-if="introduction" class="author-intro">{{ introduction }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  userVO?: API.UserVO
  categoryName?: string
  introduction?: string
}

const props = withDefaults(defineProps<Props>(), {
  categoryName: '',
  introduction: '',
})

const router = useRouter()

const goUserProfile = () => {
  if (props.userVO?.id) {
    router.push(`/user/${props.userVO.id}`)
  }
}
</script>

<style scoped>
.author-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 24px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.author-bar:hover {
  opacity: 0.75;
}

.author-text {
  flex: 1;
  min-width: 0;
}

.author-name-row {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
}

.author-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.author-dot {
  color: #d9d9d9;
  margin: 0 8px;
  font-size: 18px;
}

.author-intro {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 6px;
  line-height: 1.6;
  word-break: break-word;
}
</style>
