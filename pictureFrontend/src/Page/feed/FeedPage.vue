<template>
  <div class="feed-page">
    <FeedHeader
      :unreadCount="unreadCount"
      @read="handleMarkedRead"
    />

    <!-- 加载中骨架屏 -->
    <div v-if="loading && feedList.length === 0" class="feed-skeleton-list">
      <div v-for="i in 3" :key="i" class="feed-skeleton">
        <div class="feed-skeleton-author">
          <a-skeleton-avatar :size="36" active />
          <div class="feed-skeleton-author-text">
            <a-skeleton-input active size="small" style="width: 120px" />
            <a-skeleton-input active size="small" style="width: 80px; margin-top: 4px" />
          </div>
        </div>
        <a-skeleton-image active class="feed-skeleton-image" />
        <div style="padding: 14px 20px 16px">
          <a-skeleton active :paragraph="{ rows: 2 }" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <a-empty
      v-else-if="!loading && feedList.length === 0"
      description="暂无动态，去关注一些创作者吧"
    />

    <!-- 动态列表 -->
    <template v-else>
      <FeedCard
        v-for="item in feedList"
        :key="item.id"
        :feed="item"
      />

      <!-- 加载更多 -->
      <div class="feed-load-more">
        <a-spin v-if="loadingMore" />
        <a-button
          v-else-if="hasMore"
          type="text"
          block
          @click="loadMore"
        >
          加载更多
        </a-button>
        <span v-else class="feed-no-more">没有更多了</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getTimelineUsingGet, getUnreadCountUsingGet } from '@/api/feedController'
import FeedHeader from '@/components/Feed/FeedHeader.vue'
import FeedCard from '@/components/Feed/FeedCard.vue'

const feedList = ref<API.FeedVO[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const current = ref(1)
const pageSize = 10
const total = ref(0)
const unreadCount = ref(0)

const hasMore = ref(true)

// 获取动态列表
const fetchFeed = async (page: number, append = false) => {
  try {
    const res = await getTimelineUsingGet({ current: page, pageSize })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      if (append) {
        feedList.value = [...feedList.value, ...(data.records ?? [])]
      } else {
        feedList.value = data.records ?? []
      }
      total.value = data.total ?? 0
      current.value = Number(data.current ?? page)
      hasMore.value = feedList.value.length < total.value
    }
  } catch {
    // 静默处理
  }
}

// 获取未读数（独立接口）
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCountUsingGet()
    if (res.data.code === 0 && res.data.data) {
      unreadCount.value = res.data.data.unreadCount ?? 0
    }
  } catch {
    // 静默处理
  }
}

// 加载更多
const loadMore = async () => {
  loadingMore.value = true
  await fetchFeed(current.value + 1, true)
  loadingMore.value = false
}

// 全部已读回调
const handleMarkedRead = () => {
  unreadCount.value = 0
  feedList.value.forEach(item => {
    item.isNew = false
  })
}

// 滚动加载
const handleScroll = () => {
  if (loading.value || loadingMore.value || !hasMore.value) return
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const docHeight = document.documentElement.scrollHeight
  if (scrollTop + windowHeight >= docHeight - 200) {
    loadMore()
  }
}

onMounted(async () => {
  loading.value = true
  await fetchFeed(1)
  loading.value = false
  window.addEventListener('scroll', handleScroll, { passive: true })
  await fetchUnreadCount();
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.feed-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 24px 24px;
}

/* 骨架屏 */
.feed-skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feed-skeleton {
  background: var(--surface-primary, #FFFFFF);
  border-radius: var(--radius-card, 12px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
  overflow: hidden;
}

.feed-skeleton-author {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 12px;
}

.feed-skeleton-author-text {
  display: flex;
  flex-direction: column;
}

.feed-skeleton-image {
  width: 100%;
}

.feed-skeleton-image :deep(.ant-skeleton-image) {
  width: 100%;
  height: 300px;
}

/* 加载更多 */
.feed-load-more {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

.feed-no-more {
  font-size: 13px;
  color: var(--fg-muted, #9CA3AF);
}

/* 响应式 */
@media (max-width: 480px) {
  .feed-page {
    padding: 0 12px 16px;
  }
}
</style>
