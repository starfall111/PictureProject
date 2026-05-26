<template>
  <div id="home">

    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search placeholder="从海量图片中搜索" v-model:value="searchParams.searchText" enter-button="搜索" size="large"
        @search="doSearch" />
    </div>

    <!-- 分类栏 -->
    <div class="category-bar">
      <!-- 收起态 -->
      <div v-if="!categoryExpanded" class="category-bar-inline">
        <span class="category-label">分类：</span>
        <div class="category-bar-content">
          <a-checkable-tag :checked="selectedCategory === 0" @change="selectedCategory = 0; doSearch()"
            class="category-item">
            全部
          </a-checkable-tag>
          <a-checkable-tag v-for="category in visibleCategoryList" :key="category.value"
            :checked="selectedCategory === category.value" @change="selectedCategory = category.value; doSearch()"
            class="category-item">
            {{ category.label }}
          </a-checkable-tag>
        </div>
        <a-button v-if="showCategoryExpandBtn" type="link" size="small" @click="categoryExpanded = true">
          更多
        </a-button>
      </div>
      <!-- 展开态 -->
      <template v-else>
        <div class="category-bar-header">
          <span class="category-label">分类：</span>
          <a-input-search v-model:value="categorySearchText" placeholder="搜索分类" size="small"
            style="width: 160px; margin-right: 8px" allow-clear />
          <a-button type="link" size="small" @click="categoryExpanded = false; categorySearchText = ''">
            收起
          </a-button>
        </div>
        <div class="category-bar-content expanded">
          <a-checkable-tag :checked="selectedCategory === 0" @change="selectedCategory = 0; doSearch()"
            class="category-item">
            全部
          </a-checkable-tag>
          <a-checkable-tag v-for="category in visibleCategoryList" :key="category.value"
            :checked="selectedCategory === category.value" @change="selectedCategory = category.value; doSearch()"
            class="category-item">
            {{ category.label }}
          </a-checkable-tag>
        </div>
      </template>
    </div>
    <!-- 已选筛选条件 -->
    <div class="selected-filter-bar" v-if="selectedTagList.length > 0">
      <span class="filter-label">已选：</span>
      <a-tag v-for="tag in selectedTagList" :key="tag" closable @close="removeTag(tag)" color="blue">
        {{ tag }}
      </a-tag>
      <a-button type="link" size="small" @click="clearAllTags">清除全部</a-button>
    </div>

    <!-- 标签选择 -->
    <div class="tag-bar">
      <!-- 收起态：标签和展开按钮同一行 -->
      <div v-if="!tagExpanded" class="tag-bar-inline">
        <span class="tag-label">标签：</span>
        <div class="tag-bar-content">
          <a-checkable-tag v-for="tag in visibleTagList" :key="tag.name" :checked="selectedTagList.includes(tag.name)"
            @change="toggleTag(tag.name)" class="tag-item">
            {{ tag.name }}
          </a-checkable-tag>
        </div>
        <a-button v-if="showExpandBtn" type="link" size="small" @click="tagExpanded = true">
          展开
        </a-button>
      </div>
      <!-- 展开态：header + content 上下结构 -->
      <template v-else>
        <div class="tag-bar-header">
          <span class="tag-label">标签：</span>
          <a-input-search v-model:value="tagSearchText" placeholder="搜索标签" size="small"
            style="width: 160px; margin-right: 8px" allow-clear />
          <a-button type="link" size="small" @click="tagExpanded = false; tagSearchText = ''">收起</a-button>
        </div>
        <div class="tag-bar-content expanded">
          <a-checkable-tag v-for="tag in visibleTagList" :key="tag.name" :checked="selectedTagList.includes(tag.name)"
            @change="toggleTag(tag.name)" class="tag-item">
            {{ tag.name }}
          </a-checkable-tag>
        </div>
      </template>
    </div>

    <!-- 布局切换 -->
    <div class="layout-bar">
      <a-radio-group v-model:value="currentLayoutMode" button-style="solid" size="small" @change="onLayoutChange">
        <a-radio-button value="waterfall">
          <AppstoreOutlined /> 瀑布流
        </a-radio-button>
        <a-radio-button value="grid">
          <TableOutlined /> 网格
        </a-radio-button>
      </a-radio-group>
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
import { queryPictureUserUsingPost } from '@/api/pictureController'
import { listTagUsingGet } from '@/api/tagController'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PictureList from '@/components/PictureList/index.vue'
import ShareModal from '@/components/ShareModal.vue'
import { useLayoutPreferenceStore } from '@/stores/layoutPreference'
import { AppstoreOutlined, TableOutlined } from '@ant-design/icons-vue'

const layoutStore = useLayoutPreferenceStore()
const currentLayoutMode = ref<'waterfall' | 'grid'>(layoutStore.layoutMode)

const onLayoutChange = () => {
  layoutStore.setLayoutMode(currentLayoutMode.value)
}

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
  sortField: 'createTime',
  sortOrder: 'descend',
})

/**
 * 获取数据
 * @param reset 是否重置（筛选变化时清空重新加载）
 */
const fetchData = async (reset = false) => {
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
    const res = await queryPictureUserUsingPost(params)
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
  // 如果首屏数据不足以填满视口，继续加载
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
const categoryExpanded = ref(false)
const tagExpanded = ref(false)
const categorySearchText = ref('')
const tagSearchText = ref('')

const VISIBLE_COUNT = 10

const visibleCategoryList = computed(() => {
  const keyword = categorySearchText.value.trim().toLowerCase()
  if (!keyword) return categoryList.value.slice(0, VISIBLE_COUNT)
  return categoryList.value.filter(c => c.label.toLowerCase().includes(keyword))
})

const showCategoryExpandBtn = computed(() => {
  const keyword = categorySearchText.value.trim().toLowerCase()
  const list = keyword
    ? categoryList.value.filter(c => c.label.toLowerCase().includes(keyword))
    : categoryList.value
  return list.length > VISIBLE_COUNT
})

const visibleTagList = computed(() => {
  const keyword = tagSearchText.value.trim().toLowerCase()
  const filtered = keyword
    ? tagList.value.filter(t => t.name.toLowerCase().includes(keyword))
    : tagList.value
  return filtered
})

const showExpandBtn = computed(() => tagList.value.length > 8)

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

const clearAllTags = () => {
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

// watch(currentLayoutMode, (newMode) => {
//   nextTick(() => {
//     setupObserver()
//   })
// })
</script>

<style>
/* ===== 布局切换 ===== */
#home .layout-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

/* ===== 分类栏 ===== */
#home .category-bar {
  margin-bottom: 16px;
}

#home .category-bar-inline {
  display: flex;
  align-items: center;
  gap: 4px;
}

#home .category-bar-inline .category-bar-content {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
  gap: 6px;
  max-width: calc(100% - 80px);
}

#home .category-label {
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
  font-weight: 500;
  margin-right: 4px;
}

#home .category-bar-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

#home .category-bar-content.expanded {
  max-height: 120px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

#home .category-bar-content.expanded::-webkit-scrollbar {
  width: 4px;
}

#home .category-bar-content.expanded::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

#home .category-item {
  font-size: 18px !important;
  padding: 4px 12px !important;
  cursor: pointer;
  transition: all 0.2s ease;
}

/* ===== 标签栏 ===== */

#home .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
  font-size: 16px;
}

#home .tag-bar {
  margin-bottom: 16px;
}

#home .tag-bar-inline {
  display: flex;
  align-items: center;
  gap: 4px;
}

#home .tag-bar-inline .tag-bar-content {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
  gap: 4px;
  max-width: calc(100% - 80px);
}

#home .tag-bar-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

#home .tag-label {
  margin-right: 8px;
  font-size: 14px;
  flex-shrink: 0;
}

#home .tag-bar-content.expanded {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

#home .tag-bar-content.expanded::-webkit-scrollbar {
  width: 4px;
}

#home .tag-bar-content.expanded::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

#home .tag-item {
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

#home .selected-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f6f8fa;
  border-radius: 6px;
}

#home .filter-label {
  color: #666;
  font-size: 14px;
  margin-right: 4px;
}
</style>
