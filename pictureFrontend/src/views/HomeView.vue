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
    <!-- 图片列表 -->
    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" />
    <a-pagination style="text-align: right" v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize" :total="total" @change="onPageChange" />

  </div>


</template>


<script setup lang="ts">
import { listCategoryUsingGet } from '@/api/categoryController'
import { queryPictureUserUsingPost } from '@/api/pictureController'
import { listTagUsingGet } from '@/api/tagController'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PictureList  from '@/components/PictureList.vue'

// 数据  
const dataList = ref([])
const total = ref(0)
const loading = ref(true)

// 搜索条件  
const searchParams = reactive<API.PictureQueryDTO>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const onPageChange = (page, pageSize) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

const doSearch = () => {
  // 重置搜索条件  
  searchParams.current = 1
  fetchData()
}

const categoryList = ref<{ value: number; label: string }[]>([])
const selectedCategory = ref<number>(0)
const categoryExpanded = ref(false)
const categorySearchText = ref('')
const tagList = ref<{ name: string; count: number }[]>([])
const selectedTagList = ref<string[]>([])
const tagExpanded = ref(false)
const tagSearchText = ref('')

// 分类栏：收起态显示前 10 个，展开态显示全部（支持搜索过滤）
const visibleCategoryList = computed(() => {
  if (!categoryExpanded.value) {
    return categoryList.value.slice(0, 10)
  }
  if (categorySearchText.value) {
    const keyword = categorySearchText.value.toLowerCase()
    return categoryList.value.filter(c => c.label.toLowerCase().includes(keyword))
  }
  return categoryList.value
})

// 分类数超过 10 个才显示"更多"按钮
const showCategoryExpandBtn = computed(() => categoryList.value.length > 10)

// 收起态只显示前 8 个热门标签，展开态显示全部（支持搜索过滤）
const visibleTagList = computed(() => {
  if (!tagExpanded.value) {
    return tagList.value.slice(0, 8)
  }
  if (tagSearchText.value) {
    const keyword = tagSearchText.value.toLowerCase()
    return tagList.value.filter(tag => tag.name.toLowerCase().includes(keyword))
  }
  return tagList.value
})

// 标签总数超过 8 个才显示展开按钮
const showExpandBtn = computed(() => tagList.value.length > 8)

// 切换标签选中状态
const toggleTag = (tagName: string) => {
  const index = selectedTagList.value.indexOf(tagName)
  if (index > -1) {
    selectedTagList.value.splice(index, 1)
  } else {
    selectedTagList.value.push(tagName)
  }
  doSearch()
}

// 移除单个已选标签
const removeTag = (tagName: string) => {
  const index = selectedTagList.value.indexOf(tagName)
  if (index > -1) {
    selectedTagList.value.splice(index, 1)
  }
  doSearch()
}

// 清除所有已选标签
const clearAllTags = () => {
  selectedTagList.value = []
  doSearch()
}

// 获取标签和分类选项  
const getTagCategoryOptions = async () => {
  const res_tag = await listTagUsingGet()
  const res_category = await listCategoryUsingGet()
  if (res_category.data.code === 0 && res_category.data.data) {
    // 转换成下拉选项组件接受的格式  
    categoryList.value = (res_category.data.data ?? []).map((data: any) => {
      return {
        value: data.id,
        label: data.name,
      }
    })
  } else {
    message.error('加载选项失败，' + res_category.data.message)
  }
  if (res_tag.data.code === 0 && res_tag.data.data) {
    // 按 count 降序排列（热度排序）
    tagList.value = (res_tag.data.data ?? [])
      .map((tag: any) => ({ name: tag.name, count: tag.count ?? 0 }))
      .sort((a, b) => b.count - a.count)
  } else {
    message.error('加载选项失败，' + res_tag.data.message)
  }
}

const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [...selectedTagList.value],
  }
  if (selectedCategory.value !== 0) {
    params.categoryId = selectedCategory.value
  }
  const res = await queryPictureUserUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

const router = useRouter()
// 跳转至图片详情  
const doClickPicture = (picture) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}


onMounted(() => {
  getTagCategoryOptions()
})


// 页面加载时请求一次  
onMounted(() => {
  fetchData()
})


</script>

<style>
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

/* 收起态：标签和展开按钮同一行 */
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
  /* 不设 flex:1，避免把按钮挤出 */
  max-width: calc(100% - 80px);
}

/* 展开态 header */
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

/* 展开态标签区域 */
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
