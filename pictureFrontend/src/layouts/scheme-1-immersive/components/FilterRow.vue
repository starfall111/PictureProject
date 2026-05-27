<template>
  <div class="filter-row">
    <div class="filter-tags">
      <a-checkable-tag
        :checked="!selectedTag"
        @change="$emit('update:selectedTag', undefined)"
        class="filter-tag"
      >
        全部
      </a-checkable-tag>
      <a-checkable-tag
        v-for="tag in tagList"
        :key="tag"
        :checked="selectedTag === tag"
        @change="$emit('update:selectedTag', tag)"
        class="filter-tag"
      >
        {{ tag }}
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
  selectedTag?: string
  sortOrder?: string
}

withDefaults(defineProps<Props>(), {
  tagList: () => [],
  sortOrder: 'newest',
})

defineEmits<{
  (e: 'update:selectedTag', value: string | undefined): void
  (e: 'update:sortOrder', value: string): void
}>()
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
