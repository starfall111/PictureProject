<template>
  <div class="tab-content">
    <!-- 筛选区域 -->
    <!-- <FilterRow
      v-model:selectedTag="selectedTag"
      v-model:selectedCategory="selectedCategory"
      v-model:sortOrder="sortOrder"
      :tagList="tagList"
      :categoryList="categoryList"
    /> -->

    <!-- 空状态 -->
    <div v-if="!isLoading && dataList.length === 0" class="empty-state">
      <WorksIcon class="empty-icon" />
      <p class="empty-text">还没有上传过任何作品</p>
    </div>

    <!-- 图片列表 -->
    <PictureList
      v-else
      :dataList="dataList"
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
import { ref, computed, watch } from 'vue'
import { getUserUploadedPicturesCacheUsingPost, queryPictureUserUsingPost } from '@/api/pictureController'
import PictureList from '@/components/PictureList/index.vue'
import FilterRow from '@/layouts/scheme-1-immersive/components/FilterRow.vue'
import { useTabContent, type TabContentFilters } from './useTabContent'

interface Props {
  userId: string | number
  isVisible?: boolean
  categoryList?: API.CategoryBriefVO[]
}

const props = withDefaults(defineProps<Props>(), {
  isVisible: false,
  categoryList: () => []
})

// 筛选状态
const selectedTag = ref<string | undefined>(undefined)
const selectedCategory = ref<number | undefined>(undefined)
const sortOrder = ref('newest')


// 本地筛选状态
const localFilters = computed<TabContentFilters>(() => {
  const filters: TabContentFilters = {}
  if (selectedCategory.value) {
    const category = props.categoryList.find(c => c.id === selectedCategory.value)
    if (category) {
      filters.category = category.name
    }
  }
  if (selectedTag.value) {
    filters.tags = [selectedTag.value]
  }
  if (sortOrder.value === 'popular') {
    filters.sortBy = 'likeCount'
  } else if (sortOrder.value === 'oldest') {
    filters.sortBy = 'createTime_asc'
  }
  return filters
})

// 使用通用Hook
const fetchWorksPictures = async (params: {
  current: number
  pageSize: number
  filters?: TabContentFilters
}) => {
  const queryParams: API.PictureQueryDTO = {
    current: params.current,
    pageSize: params.pageSize,
    userId: props.userId,
    sortField: 'createTime',
    sortOrder: 'descend'
  }

  if (params.filters) {
    if (params.filters.tags && params.filters.tags.length > 0) {
      queryParams.tags = params.filters.tags
    }
    if (params.filters.sortBy) {
      if (params.filters.sortBy === 'likeCount') {
        queryParams.sortField = 'likeCount'
      } else if (params.filters.sortBy === 'createTime_asc') {
        queryParams.sortOrder = 'ascend'
      }
    }
  }

  // 处理分类筛选（需要 categoryId）
  if (selectedCategory.value) {
    queryParams.categoryId = selectedCategory.value
  }

  return await getUserUploadedPicturesCacheUsingPost({ userId: props.userId }, queryParams)
}

const { dataList, hasMore, isLoading, fetchData } = useTabContent({
  userId: props.userId,
  fetchFunction: fetchWorksPictures,
  isVisible: computed(() => props.isVisible ?? false),
  filters: localFilters
})

// 监听筛选变化，重新加载数据
watch([selectedTag, selectedCategory, sortOrder], () => {
  fetchData(true)
})

// 作品图标组件
const WorksIcon = {
  template: `
    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M4 16L8.586 20.586C8.961 20.961 9.47 21.171 10 21.171H19C20.105 21.171 21 20.276 21 19.171V7.171C21 6.066 20.105 5.171 19 5.171H10C9.47 5.171 8.961 5.381 8.586 5.756L4 10.342V16Z"
            stroke="currentColor" stroke-width="2" stroke-opacity="0.3"/>
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
