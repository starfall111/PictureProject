<template>
  <div id="userProfilePage">
    <ProfileBanner
      :userInfo="userInfo"
      :uploadCount="userInfo.uploadCount ?? 0"
      :likeCount="userInfo.totalLikes ?? 0"
      :favoriteCount="userInfo.totalFavorites ?? 0"
      :viewCount="userInfo.totalViews ?? 0"
      :shareCount="userInfo.totalShares ?? 0"
      :downloadCount="userInfo.totalDownloads ?? 0"
      :isCurrentUser="isCurrentUser"
    />

    <!-- Tab切换器 -->
    <ProfileTabSwitcher
      :activeTab="activeTab"
      :worksCount="worksCount"
      :likesCount="likesCount"
      :favoritesCount="favoritesCount"
      :pendingCount="pendingCount"
      :isCurrentUser="isCurrentUser"
      @update:activeTab="handleTabChange"
    />

    <!-- Tab内容区域 -->
    <WorksTabContent
      v-show="activeTab === 'works'"
      :userId="props.id"
      :isVisible="activeTab === 'works'"
      :categoryList="categoryList"
    />

    <LikesTabContent
      v-show="activeTab === 'likes'"
      :userId="props.id"
      :isVisible="activeTab === 'likes'"
    />

    <FavoritesTabContent
      v-show="activeTab === 'favorites'"
      :userId="props.id"
      :isVisible="activeTab === 'favorites'"
    />

    <PendingTabContent
      v-if="isCurrentUser"
      v-show="activeTab === 'pending'"
      :userId="props.id"
      :isVisible="activeTab === 'pending'"
      :userPhone="loginUserStore.loginUser.userPhone ?? ''"
      @bindSuccess="handleBindSuccess"
    />

    <ShareModal v-model:open="shareModalOpen" :picture="sharePicture" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getUserProfileUsingGet } from '@/api/userController'
import { queryPendingPicturesUsingPost } from '@/api/pictureController'
import { userLoginUserStore } from '@/stores/user'
import ProfileBanner from '@/layouts/scheme-1-immersive/components/ProfileBanner.vue'
import ProfileTabSwitcher from '@/layouts/scheme-1-immersive/components/ProfileTabSwitcher.vue'
import WorksTabContent from '@/components/UserProfile/Tabs/WorksTabContent.vue'
import LikesTabContent from '@/components/UserProfile/Tabs/LikesTabContent.vue'
import FavoritesTabContent from '@/components/UserProfile/Tabs/FavoritesTabContent.vue'
import PendingTabContent from '@/components/UserProfile/Tabs/PendingTabContent.vue'
import ShareModal from '@/components/ShareModal.vue'
import type { ProfileTab } from '@/layouts/scheme-1-immersive/components/ProfileTabSwitcher.vue'

const props = defineProps<{
  id: string | number
}>()

const loginUserStore = userLoginUserStore()

// 用户信息
const userInfo = ref<API.UserProfileVO>({})
const isCurrentUser = computed(() => {
  return String(loginUserStore.loginUser.id) === String(props.id)
})

// Tab状态
const activeTab = ref<ProfileTab>('works')

// Tab计数
const worksCount = computed(() => userInfo.value.uploadCount ?? 0)
const likesCount = computed(() => userInfo.value.userLikeCount ?? 0)
const favoritesCount = computed(() => userInfo.value.userFavoriteCount ?? 0)
const pendingCount = ref(0)

// 获取待审核图片数量（仅当前用户）
const fetchPendingCount = async () => {
  if (!isCurrentUser.value) return
  try {
    const res = await queryPendingPicturesUsingPost({ current: 1, pageSize: 1 })
    if (res.data.code === 0 && res.data.data) {
      pendingCount.value = res.data.data.total ?? 0
    }
  } catch {
    // 静默失败，不影响页面
  }
}

// 获取用户资料（包含聚合统计）
const fetchUserInfo = async () => {
  try {
    const res = await getUserProfileUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      userInfo.value = res.data.data
    } else {
      message.error('获取用户信息失败')
    }
  } catch {
    message.error('获取用户信息失败')
  }
}

// Tab切换处理
const handleTabChange = (tab: ProfileTab) => {
  activeTab.value = tab
}

// 手机号绑定成功后刷新用户信息
const handleBindSuccess = async () => {
  await loginUserStore.getLoginUser(true)
}

// 分类列表
const categoryList = computed(() => userInfo.value.categories ?? [])

// 分享弹窗
const shareModalOpen = ref(false)
const sharePicture = ref<API.PictureVO>()

onMounted(async () => {
  await fetchUserInfo()
  fetchPendingCount()
})
</script>

<style scoped>
#userProfilePage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 24px;
}
</style>
