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
          <span class="stat-value">{{ uploadCount }}</span>
          <span class="stat-label">上传</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ likeCount }}</span>
          <span class="stat-label">获赞</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ favoriteCount }}</span>
          <span class="stat-label">收藏</span>
        </div>
      </div>
      <a-button
        v-if="isCurrentUser"
        type="primary"
        ghost
        @click="$router.push('/user/center')"
      >
        编辑资料
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  userInfo: API.UserVO
  uploadCount?: number
  likeCount?: number
  favoriteCount?: number
  isCurrentUser?: boolean
}

withDefaults(defineProps<Props>(), {
  uploadCount: 0,
  likeCount: 0,
  favoriteCount: 0,
  isCurrentUser: false,
})
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
  display: flex;
  gap: 24px;
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
}
</style>
