<template>
  <div class="profile-banner">
    <div class="profile-banner-left">
      <a-avatar :src="userInfo.userAvatar" :size="64">
        {{ userInfo.userName?.charAt(0) ?? '?' }}
      </a-avatar>
      <div class="profile-banner-info">
        <h2 class="profile-banner-name">{{ userInfo.userName ?? '匿名用户' }}</h2>
        <p v-if="userInfo.userProfile" class="profile-banner-bio">{{ userInfo.userProfile }}</p>
      </div>
    </div>
    <div class="profile-banner-right">
      <div class="profile-banner-stats">
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(uploadCount) }}</span>
          <span class="stat-label">上传</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(likeCount) }}</span>
          <span class="stat-label">获赞</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(favoriteCount) }}</span>
          <span class="stat-label">收藏</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(viewCount) }}</span>
          <span class="stat-label">浏览</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(shareCount) }}</span>
          <span class="stat-label">分享</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatCount(downloadCount) }}</span>
          <span class="stat-label">下载</span>
        </div>
        <!-- 新增：关注数和粉丝数 -->
        <div
          class="stat-item clickable"
          @click="handleShowFollowing"
        >
          <span class="stat-value">{{ formatCount(followerCount) }}</span>
          <span class="stat-label">关注</span>
        </div>
        <div
          class="stat-item clickable"
          @click="handleShowFollowers"
        >
          <span class="stat-value">{{ formatCount(followCount) }}</span>
          <span class="stat-label">粉丝</span>
        </div>
      </div>
      <!-- 自己的主页：编辑资料按钮 -->
      <a-button
        v-if="isCurrentUser"
        type="primary"
        ghost
        @click="handleEditProfile"
      >
        编辑资料
      </a-button>
      <!-- 别人的主页：关注按钮 -->
      <FollowButton
        v-else
        :target-user-id="userInfo.id"
        :is-following="isFollowed"
        @follow-change="handleFollowChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatCount } from '@/utils/formatCount'
import { useRouter } from 'vue-router'
import FollowButton from '@/components/UserProfile/FollowButton.vue'

const router = useRouter()

interface Props {
  userInfo: API.UserVO | API.UserProfileVO
  uploadCount?: number
  likeCount?: number
  favoriteCount?: number
  viewCount?: number
  shareCount?: number
  downloadCount?: number
  followCount?: number
  followerCount?: number
  isFollowed?: boolean
  isCurrentUser?: boolean
}

withDefaults(defineProps<Props>(), {
  uploadCount: 0,
  likeCount: 0,
  favoriteCount: 0,
  viewCount: 0,
  shareCount: 0,
  downloadCount: 0,
  followCount: 0,
  followerCount: 0,
  isFollowed: false,
  isCurrentUser: false,
})

const emit = defineEmits<{
  (e: 'showFollowing'): void
  (e: 'showFollowers'): void
  (e: 'followChange', userId: number, isFollowing: boolean): void
}>()

const handleEditProfile = () => {
  router.push('/user/center')
}

const handleShowFollowing = () => {
  emit('showFollowing')
}

const handleShowFollowers = () => {
  emit('showFollowers')
}

const handleFollowChange = (userId: number, isFollowing: boolean) => {
  emit('followChange', userId, isFollowing)
}
</script>

<style scoped>
.profile-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: var(--banner-height, 110px);
  padding: 0 24px;
  background: var(--surface-primary, #FFFFFF);
  border-radius: var(--radius-card, 12px);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
}

.profile-banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-banner-info {
  min-width: 0;
}

.profile-banner-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
  margin: 0 0 4px;
}

.profile-banner-bio {
  font-size: 14px;
  color: var(--fg-secondary, #4B5563);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.profile-banner-right {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.profile-banner-stats {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
}

.stat-label {
  font-size: 12px;
  color: var(--fg-muted, #9CA3AF);
}

@media (max-width: 640px) {
  .profile-banner {
    flex-direction: column;
    height: auto;
    padding: 20px 16px;
    gap: 16px;
  }

  .profile-banner-right {
    width: 100%;
    justify-content: space-between;
  }

  .profile-banner-stats {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .profile-banner-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-item.clickable {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item.clickable:hover {
  transform: scale(1.05);
}

.stat-item.clickable:hover .stat-value {
  color: var(--accent, #635BFF);
}
</style>
