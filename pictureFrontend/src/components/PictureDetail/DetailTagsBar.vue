<template>
  <div v-if="tags && tags.length" class="tags-bar">
    <a-tag v-for="tag in tags" :key="tag" class="tag-pill">
      {{ tag }}
    </a-tag>
  </div>
</template>

<script setup lang="ts">
interface Props {
  tags?: string[]
}

withDefaults(defineProps<Props>(), {
  tags: () => [],
})
</script>

<style scoped>
.tags-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.tag-pill {
  border-radius: 16px;
  padding: 2px 12px;
  background: #f5f5f5;
  border-color: #e8e8e8;
  color: #595959;
  font-size: 14px;
  cursor: default;
}

.tag-pill:hover {
  background: #eaeaea;
}
</style>
