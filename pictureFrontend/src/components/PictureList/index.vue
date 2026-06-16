<template>
  <div class="picture-list">
    <PictureWaterfallLayout
      v-if="layoutMode === 'waterfall' && showSocial"
      :dataList="dataList"
    />
    <PictureGridLayout
      v-else
      :dataList="dataList"
      :loading="loading"
      :showOp="showOp"
      :onReload="onReload"
    />
    <!-- 空状态：非加载中且数据为空时展示 -->
    <a-empty
      v-if="!loading && dataList.length === 0"
      :description="emptyText"
      class="picture-empty"
    />
    <!-- 加载状态 -->
    <div v-if="showSocial" class="infinite-scroll-status">
      <a-spin v-if="isLoadingMore" tip="加载中..." class="load-spinner" />
      <div v-else-if="!hasMore && dataList.length > 0" class="no-more">
        —— 没有更多了 ——
      </div>
      <!-- 哨兵元素，用于 IntersectionObserver 触底检测 -->
      <div id="scroll-sentinel" class="scroll-sentinel" />
    </div>
  </div>
</template>

<script setup lang="ts">
import PictureGridLayout from './PictureGridLayout.vue'
import PictureWaterfallLayout from './PictureWaterfallLayout.vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  layoutMode?: 'waterfall' | 'grid'
  showSocial?: boolean
  hasMore?: boolean
  isLoadingMore?: boolean
  /** 空数据提示文案 */
  emptyText?: string
}

withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  layoutMode: 'grid',
  showSocial: false,
  hasMore: true,
  isLoadingMore: false,
  emptyText: '暂无图片',
})

defineEmits<{
  (e: 'loadMore'): void
}>()
</script>

<style scoped>
.infinite-scroll-status {
  text-align: center;
  padding: 20px 0;
}

.load-spinner {
  padding: 16px;
}

.no-more {
  color: #999;
  font-size: 13px;
  padding: 8px 0;
}

.scroll-sentinel {
  height: 1px;
  width: 100%;
}

.picture-empty {
  padding: 60px 0;
}
</style>
