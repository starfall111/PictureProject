<template>
  <div class="picture-search-form">
    <!-- 高频筛选区（始终可见） -->
    <div class="search-main">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="搜索" name="searchText" class="search-keyword">
          <a-input
            v-model:value="searchParams.searchText"
            placeholder="搜索名称、简介..."
            allow-clear
            @pressEnter="doSearch"
          />
        </a-form-item>
        <a-form-item label="分类" name="category">
          <a-auto-complete
            v-model:value="searchParams.categoryId"
            style="min-width: 160px"
            :options="categoryOptions"
            placeholder="请输入分类"
            allowClear
          />
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-select
            v-model:value="searchParams.tags"
            style="min-width: 160px"
            :options="tagOptions"
            mode="tags"
            placeholder="请输入标签"
            allowClear
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button html-type="reset" @click="doClear">重置</a-button>
            <a-popover
              v-model:open="advancedOpen"
              trigger="click"
              placement="bottomRight"
            >
              <template #content>
                <div class="advanced-popover">
                  <a-form layout="vertical" :model="searchParams" @finish="doSearch">
                    <a-row :gutter="12">
                      <a-col :span="24">
                        <a-form-item label="日期范围">
                          <a-range-picker
                            style="width: 100%"
                            show-time
                            v-model:value="dateRange"
                            :placeholder="['编辑开始日期', '编辑结束时间']"
                            format="YYYY/MM/DD HH:mm:ss"
                            :presets="rangePresets"
                            @change="onRangeChange"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="名称">
                          <a-input v-model:value="searchParams.name" placeholder="精确匹配名称" allow-clear />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="简介">
                          <a-input v-model:value="searchParams.introduction" placeholder="精确匹配简介" allow-clear />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="宽度">
                          <a-input-number v-model:value="searchParams.picWidth" :min="1" placeholder="px" style="width: 100%" />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="高度">
                          <a-input-number v-model:value="searchParams.picHeight" :min="1" placeholder="px" style="width: 100%" />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="格式">
                          <a-select
                            v-model:value="searchParams.picFormat"
                            placeholder="格式"
                            allow-clear
                          >
                            <a-select-option value="jpg">JPG</a-select-option>
                            <a-select-option value="jpeg">JPEG</a-select-option>
                            <a-select-option value="png">PNG</a-select-option>
                            <a-select-option value="gif">GIF</a-select-option>
                            <a-select-option value="webp">WebP</a-select-option>
                            <a-select-option value="svg">SVG</a-select-option>
                          </a-select>
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </a-form>
                </div>
              </template>
              <div
                class="more-filter-btn"
                :class="{
                  'more-filter-btn--active': activeAdvancedCount > 0,
                  'more-filter-btn--open': advancedOpen,
                }"
              >
                <FilterOutlined />
                <span>更多筛选</span>
                <span v-if="activeAdvancedCount > 0" class="more-filter-badge">{{ activeAdvancedCount }}</span>
                <DownOutlined :class="{ 'more-filter-arrow--open': advancedOpen }" class="more-filter-arrow" />
              </div>
            </a-popover>
          </a-space>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { DownOutlined, FilterOutlined } from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { message } from 'ant-design-vue';
import { listTagUsingGet } from '@/api/tagController';
import { listCategoryUsingGet } from '@/api/categoryController';

interface Props {
  onSearch?: (searchParams: API.PictureQueryDTO) => void
}

const props = defineProps<Props>()

// 搜索条件
const searchParams = reactive<API.PictureQueryDTO>({})

// 高级筛选气泡卡片开关
const advancedOpen = ref(false)

// 计算高级筛选区激活的字段数量
const activeAdvancedCount = computed(() => {
  let count = 0
  if (searchParams.startEditTime || searchParams.endEditTime) count++
  if (searchParams.name) count++
  if (searchParams.introduction) count++
  if (searchParams.picWidth) count++
  if (searchParams.picHeight) count++
  if (searchParams.picFormat) count++
  return count
})

// 获取数据
const doSearch = () => {
  props.onSearch?.(searchParams)
}

const dateRange = ref<[]>([])

/**
 * 日期范围更改时触发
 * @param dates
 * @param dateStrings
 */
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates.length < 2) {
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
  } else {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  }
}

const rangePresets = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])


const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

// 获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res_tag = await listTagUsingGet()
  const res_category = await listCategoryUsingGet()
  if (res_category.data.code === 0 && res_category.data.data) {
    categoryOptions.value = (res_category.data.data ?? []).map((data: any) => {
      return {
        value: data.id,
        label: data.name,
      }
    })
  } else {
    message.error('加载选项失败，' + res_category.data.message)
  }
  if (res_tag.data.code === 0 && res_tag.data.data) {
    tagOptions.value = (res_tag.data.data ?? []).map((data: any) => {
      return {
        value: data.name,
        label: data.name,
      }
    })
  } else {
    message.error('加载选项失败，' + res_tag.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

// 清理
const doClear = () => {
  Object.keys(searchParams).forEach((key) => {
    searchParams[key] = undefined
  })
  dateRange.value = []
  props.onSearch?.(searchParams)
}

</script>

<style scoped>
.picture-search-form {
  width: 100%;
}

.search-main :deep(.ant-form-item) {
  margin-top: 16px;
}

.search-keyword :deep(.ant-input) {
  width: 240px;
}

/* 气泡卡片内布局 */
.advanced-popover {
  width: 420px;
}

.advanced-popover :deep(.ant-form-item) {
  margin-bottom: 12px;
}

/* 更多筛选按钮 — 气泡卡片样式 */
.more-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.more-filter-btn:hover {
  border-color: var(--ant-color-primary, #1677ff);
  color: var(--ant-color-primary, #1677ff);
}

.more-filter-btn--open {
  border-color: var(--ant-color-primary, #1677ff);
  color: var(--ant-color-primary, #1677ff);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
}

.more-filter-btn--active {
  border-color: var(--ant-color-primary, #1677ff);
  color: var(--ant-color-primary, #1677ff);
  background: #e6f4ff;
}

.more-filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--ant-color-primary, #1677ff);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.more-filter-arrow {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.more-filter-arrow--open {
  transform: rotate(180deg);
}
</style>
