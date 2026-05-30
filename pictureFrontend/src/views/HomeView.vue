<template>
  <div id="home">

    <!-- 搜索框 -->
    <div v-if="!isImmersive" class="search-bar">
      <a-input-search placeholder="从海量图片中搜索" v-model:value="searchParams.searchText" enter-button="搜索" size="large"
        @search="doSearch" />
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">

      <!-- 排序按钮组 -->
      <div class="sort-bar">
        <a-radio-group v-model:value="currentSort" button-style="solid" size="middle" @change="onSortChange">
          <a-radio-button value="">
            <FireOutlined /> 推荐
          </a-radio-button>
          <a-radio-button value="createTime">
            <ClockCircleOutlined /> 最新
          </a-radio-button>
          <a-radio-button value="thumbCount">
            <StarOutlined /> 精选
          </a-radio-button>
        </a-radio-group>
      </div>

      <!-- 分类直接展示 -->
      <div class="category-bar">
        <div class="category-inline">
          <a-checkable-tag :checked="selectedCategory === 0" @change="selectedCategory = 0; doSearch()"
            class="category-item">
            全部
          </a-checkable-tag>
          <a-checkable-tag v-for="category in visibleCategoryListLimited" :key="category.value"
            :checked="selectedCategory === category.value"
            @change="selectedCategory = category.value; doSearch()"
            class="category-item">
            {{ category.label }}
          </a-checkable-tag>
          <a-popover v-model:open="categoryPopoverOpen" trigger="click" placement="bottomLeft"
            overlay-class-name="category-popover-overlay">
            <template #content>
              <div class="category-popover-content">
                <a-input-search v-model:value="categorySearchText" placeholder="搜索分类" size="small" allow-clear
                  style="margin-bottom: 12px; width: 100%" />
                <div class="category-popover-list">
                  <a-checkable-tag :checked="selectedCategory === 0"
                    @change="selectedCategory = 0; doSearch(); categoryPopoverOpen = false"
                    class="category-popover-item">
                    全部
                  </a-checkable-tag>
                  <a-checkable-tag v-for="category in filteredCategoryList" :key="category.value"
                    :checked="selectedCategory === category.value"
                    @change="selectedCategory = category.value; doSearch(); categoryPopoverOpen = false"
                    class="category-popover-item">
                    {{ category.label }}
                  </a-checkable-tag>
                </div>
              </div>
            </template>
            <a-button v-if="categoryList.length > CATEGORY_VISIBLE_COUNT" type="link" size="small" class="more-category-btn">
              全部分类 <RightOutlined />
            </a-button>
          </a-popover>
        </div>
      </div>

      <!-- 已选条件汇总 -->
      <div class="active-filters" v-if="selectedTagList.length > 0 || selectedCategory !== 0">
        <a-tag v-if="selectedCategory !== 0" closable @close="selectedCategory = 0; doSearch()" color="blue">
          {{ currentCategoryLabel }}
        </a-tag>
        <a-tag v-for="tag in selectedTagList" :key="tag" closable @close="removeTag(tag)" color="blue">
          {{ tag }}
        </a-tag>
        <a-button type="link" size="small" @click="clearAllFilters" class="clear-all-btn">清除全部</a-button>
      </div>

      <!-- 布局切换 -->
      <div v-if="!isImmersive" class="layout-bar">
        <a-radio-group v-model:value="currentLayoutMode" button-style="solid" size="small" @change="onLayoutChange">
          <a-radio-button value="waterfall">
            <AppstoreOutlined /> 瀑布流
          </a-radio-button>
          <a-radio-button value="grid">
            <TableOutlined /> 网格
          </a-radio-button>
        </a-radio-group>
      </div>
    </div>

    <!-- 图片列表 -->
    <PictureList
      :dataList="allPictures"
      :loading="isLoading && allPictures.length === 0"
      :layoutMode="currentLayoutMode"
      :showSocial="true"
      :hasMore="hasMore"
      :isLoadingMore="isLoading"
      @loadMore="onLoadMore"
    />

    <!-- 分享弹窗 -->
    <ShareModal v-model:open="shareModalOpen" :picture="sharePicture" />
  </div>
</template>


<script setup lang="ts">
import { listCategoryUsingGet } from '@/api/categoryController'
import { queryPictureUserCacheUsingPost } from '@/api/pictureController'
import { recommendUsingPost } from '@/api/recommendController'
import { listTagUsingGet } from '@/api/tagController'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PictureList from '@/components/PictureList/index.vue'
import ShareModal from '@/components/ShareModal.vue'
import { useLayoutPreferenceStore } from '@/stores/layoutPreference'
import { useLayoutScheme } from '@/composables/useLayoutScheme'
import {
  AppstoreOutlined,
  TableOutlined,
  FireOutlined,
  ClockCircleOutlined,
  StarOutlined,
  FolderOutlined,
  DownOutlined,
  RightOutlined
} from '@ant-design/icons-vue'

const layoutStore = useLayoutPreferenceStore()
const { activeSchemeId } = useLayoutScheme()
const currentLayoutMode = ref<'waterfall' | 'grid'>(layoutStore.layoutMode)

const onLayoutChange = () => {
  layoutStore.setLayoutMode(currentLayoutMode.value)
}

const isImmersive = computed(() => activeSchemeId.value === 'scheme-1-immersive')

// ==================== 无限滚动数据 ====================

const allPictures = ref<API.PictureVO[]>([])
const currentPage = ref(1)
const pageSize = 12
const hasMore = ref(true)
const isLoading = ref(false)
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.PictureQueryDTO>({
  current: 1,
  pageSize,
  sortField: undefined,
  sortOrder: 'descend',
})

/**
 * 获取推荐数据（使用推荐接口）
 * @param reset 是否重置（筛选变化时清空重新加载）
 */
const fetchRecommendData = async (reset = false) => {
  if (isLoading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    currentPage.value = 1
    allPictures.value = []
    hasMore.value = true
  }

  isLoading.value = true

  const params: API.RecommendQueryDTO = {
    current: currentPage.value,
    pageSize,
    categoryId: selectedCategory.value !== 0 ? selectedCategory.value : undefined,
  }

  try {
    const res = await recommendUsingPost(params)
    if (res.data.data) {
      const records = res.data.data.pictures ?? []
      hasMore.value = res.data.data.hasMore ?? false

      if (reset) {
        allPictures.value = records
      } else {
        allPictures.value = [...allPictures.value, ...records]
      }

      total.value = allPictures.value.length + (hasMore.value ? 1 : 0)
      currentPage.value++
    } else {
      message.error('获取推荐数据失败，' + res.data.message)
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

/**
 * 获取数据
 * @param reset 是否重置（筛选变化时清空重新加载）
 */
const fetchData = async (reset = false) => {
  // 推荐模式走独立推荐接口
  if (currentSort.value === '') {
    return fetchRecommendData(reset)
  }

  if (isLoading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    currentPage.value = 1
    allPictures.value = []
    hasMore.value = true
  }

  isLoading.value = true

  const params: API.PictureQueryDTO = {
    current: currentPage.value,
    pageSize,
    sortField: searchParams.sortField,
    sortOrder: searchParams.sortOrder,
    searchText: searchParams.searchText,
    tags: [...selectedTagList.value],
  }
  if (selectedCategory.value !== 0) {
    params.categoryId = selectedCategory.value
  }

  try {
    const res = await queryPictureUserCacheUsingPost(params)
    if (res.data.data) {
      const records = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0

      if (reset) {
        allPictures.value = records
      } else {
        allPictures.value = [...allPictures.value, ...records]
      }

      hasMore.value = allPictures.value.length < total.value
      currentPage.value++
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 筛选变化 → 重置加载
const doSearch = () => {
  fetchData(true)
  nextTick(() => {
    setupObserver()
  })
}

// 触底加载更多
const onLoadMore = () => {
  fetchData(false)
}

// IntersectionObserver 自动加载
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const setupObserver = () => {

  if (observer) {
    observer.disconnect()
    observer = null
  }

  // 在下一帧确保 DOM 已渲染
  setTimeout(() => {
    const sentinel = document.getElementById('scroll-sentinel')
    if (!sentinel) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore.value && !isLoading.value) {
          onLoadMore()
        }
      },
      { rootMargin: '200px' }
    )
    observer.observe(sentinel)
  }, 100)
}

// 首次加载后判断是否需要自动填充
const ensureFullPage = async () => {
  if (hasMore.value && allPictures.value.length < pageSize) {
    await fetchData(false)
  }
}

// ==================== 分享弹窗 ====================
const shareModalOpen = ref(false)
const sharePicture = ref<API.PictureVO>()

// ==================== 标签和分类 ====================
const categoryList = ref<{ value: number; label: string }[]>([])
const tagList = ref<{ name: string; count: number }[]>([])
const selectedCategory = ref(0)
const selectedTagList = ref<string[]>([])
const categorySearchText = ref('')
const tagSearchText = ref('')

// 气泡卡片控制
const categoryPopoverOpen = ref(false)
const tagPopoverOpen = ref(false)

// 排序
const currentSort = ref('')

const onSortChange = () => {
  searchParams.sortField = currentSort.value || undefined
  searchParams.sortOrder = 'descend'
  doSearch()
}

// 分类可见数量
const CATEGORY_VISIBLE_COUNT = 20

const visibleCategoryListLimited = computed(() => {
  return categoryList.value.slice(0, CATEGORY_VISIBLE_COUNT)
})

const filteredCategoryList = computed(() => {
  const keyword = categorySearchText.value.trim().toLowerCase()
  if (!keyword) return categoryList.value
  return categoryList.value.filter(c => c.label.toLowerCase().includes(keyword))
})

const filteredTagList = computed(() => {
  const keyword = tagSearchText.value.trim().toLowerCase()
  if (!keyword) return tagList.value
  return tagList.value.filter(t => t.name.toLowerCase().includes(keyword))
})

const currentCategoryLabel = computed(() => {
  if (selectedCategory.value === 0) return '全部分类'
  const found = categoryList.value.find(c => c.value === selectedCategory.value)
  return found?.label ?? '全部分类'
})

const toggleTag = (tagName: string) => {
  const index = selectedTagList.value.indexOf(tagName)
  if (index > -1) {
    selectedTagList.value.splice(index, 1)
  } else {
    selectedTagList.value.push(tagName)
  }
  doSearch()
}

const removeTag = (tagName: string) => {
  const index = selectedTagList.value.indexOf(tagName)
  if (index > -1) {
    selectedTagList.value.splice(index, 1)
  }
  doSearch()
}

const clearAllFilters = () => {
  selectedCategory.value = 0
  selectedTagList.value = []
  doSearch()
}

const getTagCategoryOptions = async () => {
  const res_tag = await listTagUsingGet()
  const res_category = await listCategoryUsingGet()
  if (res_category.data.code === 0 && res_category.data.data) {
    categoryList.value = (res_category.data.data ?? []).map((data: any) => ({
      value: data.id,
      label: data.name,
    }))
  } else {
    message.error('加载选项失败，' + res_category.data.message)
  }
  if (res_tag.data.code === 0 && res_tag.data.data) {
    tagList.value = (res_tag.data.data ?? [])
      .map((tag: any) => ({ name: tag.name, count: tag.count ?? 0 }))
      .sort((a, b) => b.count - a.count)
  } else {
    message.error('加载选项失败，' + res_tag.data.message)
  }
}

const router = useRouter()

// ==================== 初始化 ====================

onMounted(async () => {
  await getTagCategoryOptions()
  await fetchData(true)
  setupObserver()
  ensureFullPage()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style>

#home {
  /* margin: 0 32px; */
}

/* ===== 搜索框 ===== */
#home .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
  font-size: 16px;
}

/* ===== 筛选区域 ===== */
#home .filter-section {
  margin-bottom: 20px;
}

/* ===== 排序按钮组 ===== */
#home .sort-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

#home .sort-bar .ant-radio-button-wrapper {
  border-radius: 20px;
  border: none;
  padding: 0 22px;
  height: 36px;
  line-height: 36px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

#home .sort-bar .ant-radio-button-wrapper::before {
  display: none;
}

#home .sort-bar .ant-radio-button-wrapper:first-child {
  border-radius: 20px 0 0 20px;
}

#home .sort-bar .ant-radio-button-wrapper:last-child {
  border-radius: 0 20px 20px 0;
}

#home .sort-bar .ant-radio-button-wrapper-checked {
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

/* ===== 标签云 ===== */
#home .tag-cloud-bar {
  margin-bottom: 12px;
}

#home .tag-cloud-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 76px;
  overflow: hidden;
  align-items: center;
}

#home .tag-cloud-item {
  font-size: 14px !important;
  padding: 4px 14px !important;
  border-radius: 16px !important;
  cursor: pointer;
  transition: all 0.2s;
  background: #f3f4f6;
  color: #4b5563;
  border: none !important;
}

#home .tag-cloud-item:hover {
  background: #e8f0fe;
  transform: translateY(-1px);
}

#home .tag-cloud-item.ant-tag-checkable-checked {
  background: #1677ff !important;
  color: #fff !important;
}

#home .tag-count {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 4px;
}

#home .tag-cloud-item.ant-tag-checkable-checked .tag-count {
  color: rgba(255, 255, 255, 0.7);
}

#home .more-tags-btn {
  color: #1677ff;
  font-size: 13px;
  height: 28px;
  line-height: 28px;
  padding: 0 8px;
  flex-shrink: 0;
}

/* 标签气泡卡片内容 */
.tag-popover-overlay .ant-popover-inner {
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.tag-popover-content {
  width: 400px;
  max-height: 360px;
}

.tag-popover-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding-right: 4px;
}

.tag-popover-list::-webkit-scrollbar {
  width: 4px;
}

.tag-popover-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.tag-popover-item {
  font-size: 14px !important;
  padding: 4px 14px !important;
  border-radius: 16px !important;
  background: #f3f4f6;
  color: #4b5563;
  border: none !important;
}

.tag-popover-item.ant-tag-checkable-checked {
  background: #1677ff !important;
  color: #fff !important;
}

/* ===== 分类直接展示 ===== */
#home .category-bar {
  margin-bottom: 12px;
}

#home .category-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 76px;
  overflow: hidden;
  align-items: center;
}

#home .category-item {
  font-size: 14px !important;
  padding: 4px 14px !important;
  border-radius: 16px !important;
  cursor: pointer;
  transition: all 0.2s;
  background: #f3f4f6;
  color: #4b5563;
  border: none !important;
}

#home .category-item:hover {
  background: #e8f0fe;
  transform: translateY(-1px);
}

#home .category-item.ant-tag-checkable-checked {
  background: #1677ff !important;
  color: #fff !important;
}

#home .more-category-btn {
  color: #1677ff;
  font-size: 13px;
  height: 28px;
  line-height: 28px;
  padding: 0 8px;
  flex-shrink: 0;
}

/* 分类气泡卡片内容（全部分类弹窗） */
.category-popover-overlay .ant-popover-inner {
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.category-popover-content {
  width: 320px;
  max-height: 360px;
}

.category-popover-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 4px;
}

.category-popover-list::-webkit-scrollbar {
  width: 4px;
}

.category-popover-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.category-popover-item {
  font-size: 14px !important;
  padding: 4px 14px !important;
  border-radius: 16px !important;
  background: #f3f4f6;
  color: #4b5563;
  border: none !important;
}

.category-popover-item.ant-tag-checkable-checked {
  background: #1677ff !important;
  color: #fff !important;
}

/* ===== 已选条件汇总 ===== */
#home .active-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  padding: 8px 14px;
  background: #f6f8fa;
  border-radius: 8px;
}

#home .clear-all-btn {
  color: #999;
  font-size: 13px;
}

#home .clear-all-btn:hover {
  color: #ff4d4f;
}

/* ===== 布局切换 ===== */
#home .layout-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
