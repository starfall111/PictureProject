<template>
  <div class="filter-row">
    <div class="filter-tags">
      <a-checkable-tag
        :checked="!selectedTag && !selectedCategory"
        @change="handleClearAll"
        class="filter-tag"
      >
        全部
      </a-checkable-tag>
      <a-checkable-tag
        v-for="tag in tagList"
        :key="tag"
        :checked="selectedTag === tag"
        @change="handleTagChange(tag)"
        class="filter-tag"
      >
        {{ tag }}
      </a-checkable-tag>
      <a-divider v-if="categoryList.length > 0" type="vertical" />
      <a-checkable-tag
        v-for="category in categoryList"
        :key="category.id"
        :checked="selectedCategory === category.id"
        @change="handleCategoryChange(category.id)"
        class="filter-tag"
      >
        {{ category.name }}
      </a-checkable-tag>
    </div>
    <div class="filter-sort">
      <a-select
        :value="sortOrder"
        @change="$emit('update:sortOrder', $event)"
        size="small"
        style="width: 120px"
      >
        <a-select-option value="newest">最新上传</a-select-option>
        <a-select-option value="popular">最多点赞</a-select-option>
        <a-select-option value="oldest">最早上传</a-select-option>
      </a-select>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  tagList: string[]
  categoryList?: API.CategoryBriefVO[]
  selectedTag?: string
  sortOrder?: string
}

const props = withDefaults(defineProps<Props>(), {
  tagList: () => [],
  categoryList: () => [],
  sortOrder: 'newest',
})

const emit = defineEmits<{
  (e: 'update:selectedTag', value: string | undefined): void
  (e: 'update:sortOrder', value: string): void
}>()

// 新增 v-model 绑定
const selectedCategory = defineModel<number | undefined>('selectedCategory')

const handleClearAll = () => {
  emit('update:selectedTag', undefined)
  selectedCategory.value = undefined
}

const handleTagChange = (tag: string) => {
  emit('update:selectedTag', props.selectedTag === tag ? undefined : tag)
  selectedCategory.value = undefined
}

const handleCategoryChange = (categoryId: number) => {
  const current = selectedCategory.value
  selectedCategory.value = current === categoryId ? undefined : categoryId
  emit('update:selectedTag', undefined)
}
</script>

<style scoped>
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  margin-bottom: 8px;
}

.filter-tags {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  flex: 1;
  min-width: 0;
  padding-right: 16px;
  scrollbar-width: none;
}

.filter-tags::-webkit-scrollbar {
  display: none;
}

.filter-tag {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: var(--radius-pill, 9999px);
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.filter-tag[data-checked="true"],
.filter-tag.ant-tag-checkable-checked {
  background: var(--accent, #33A1C9) !important;
  color: #fff !important;
  border-color: var(--accent, #33A1C9) !important;
}

.filter-sort {
  flex-shrink: 0;
}
</style>
