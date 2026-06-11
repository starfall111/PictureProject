<template>
  <div class="tab-content">
    <!-- 空状态 -->
    <div v-if="!isLoading && dataList.length === 0" class="empty-state">
      <FavoritesIcon class="empty-icon" />
      <p class="empty-text">还没有收藏过任何图片</p>
    </div>

    <!-- 图片列表 -->
    <PictureList
      v-else
      :dataList="convertToPictureVO(dataList)"
      :loading="isLoading && dataList.length === 0"
      layoutMode="waterfall"
      :showSocial="true"
      :hasMore="hasMore"
      :isLoadingMore="isLoading"
      @loadMore="() => fetchData(false)"
    />
    <!-- 哨兵元素 -->
    <div :id="`scroll-sentinel-${userId}`" class="scroll-sentinel" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { pictureControllerGetUserFavoritedPicturesCache, pictureControllerGetUserFavoritedPictures } from '@/api/pictureController'
import PictureList from '@/components/PictureList/index.vue'
import { useTabContent, type TabContentFilters } from './useTabContent'

interface Props {
  userId: string | number
  isVisible?: boolean
  showFilter?: boolean
  categoryList?: API.CategoryBriefVO[]
  tagList?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  isVisible: false,
  showFilter: false,
  categoryList: () => [],
  tagList: () => []
})

// 本地筛选状态
const localFilters = computed<TabContentFilters>(() => ({}))

// 将 PictureBriefVO 转换为 PictureVO（保持兼容性）
const convertToPictureVO = (list: API.PictureBriefVO[]): API.PictureVO[] => {
  return list.map(item => ({
    ...item,
    socialInfo: {
      likeCount: item.likeCount,
      favoriteCount: item.favoriteCount,
      viewCount: item.viewCount,
      downloadCount: item.downloadCount,
      isLiked: !!item.likeTime,
      isFavorited: !!item.favoriteTime
    }
  } as API.PictureVO))
}

// 使用通用Hook
const fetchFavoritedPictures = async (params: {
  current: number
  pageSize: number
  filters?: TabContentFilters
}) => {
  const queryParams: API.UserPictureQueryDTO = {
    current: params.current,
    pageSize: params.pageSize
  }

  if (params.filters) {
    if (params.filters.category) {
      queryParams.category = params.filters.category
    }
    if (params.filters.tags && params.filters.tags.length > 0) {
      queryParams.tags = params.filters.tags
    }
    if (params.filters.sortBy) {
      queryParams.sortBy = params.filters.sortBy
    }
  }

  return await pictureControllerGetUserFavoritedPicturesCache({ userId: props.userId }, queryParams)
}

const { dataList, hasMore, isLoading, fetchData } = useTabContent({
  userId: props.userId,
  fetchFunction: fetchFavoritedPictures,
  isVisible: computed(() => props.isVisible ?? false),
  filters: localFilters
})

// 收藏图标组件
const FavoritesIcon = {
  template: `
    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M5 4V20H19V4H5Z" stroke="currentColor" stroke-width="2" stroke-opacity="0.3"/>
      <path d="M9 9H15M9 12H15M9 15H12" stroke="currentColor" stroke-width="2" stroke-opacity="0.3"/>
    </svg>
  `
}
</script>

<style scoped>
.tab-content {
  min-height: 300px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: #9CA3AF;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  color: #9CA3AF;
}

.empty-text {
  font-size: 14px;
  color: #9CA3AF;
}

.scroll-sentinel {
  height: 1px;
  width: 100%;
}
</style>
