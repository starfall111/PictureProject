<template>
  <div class="feed-card" :class="{ 'is-new': feed.isNew }">
    <!-- 新内容蓝点 -->
    <span v-if="feed.isNew" class="feed-card-new-dot" />

    <!-- 作者栏 -->
    <div class="feed-card-author">
      <a-avatar
        :size="36"
        :src="feed.userAvatar"
        class="feed-card-avatar"
        @click.stop="goUserProfile"
      >
        {{ feed.userName?.charAt(0) ?? '?' }}
      </a-avatar>
      <div class="feed-card-author-info">
        <span class="feed-card-username" @click.stop="goUserProfile">
          {{ feed.userName ?? '匿名用户' }}
        </span>
        <div class="feed-card-meta">
          <a-tag v-if="feed.categoryName" color="blue" class="feed-card-category">
            {{ feed.categoryName }}
          </a-tag>
          <span class="feed-card-time">{{ formatRelativeTime(feed.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 图片区域 -->
    <div class="feed-card-image-wrapper" @click="goPictureDetail">
      <img
        class="feed-card-image"
        :src="feed.url"
        :alt="feed.name"
        @error="onImgError"
      />
    </div>

    <!-- 底部信息栏 -->
    <div class="feed-card-body">
      <h3 v-if="feed.name" class="feed-card-name">{{ feed.name }}</h3>
      <p v-if="feed.introduction" class="feed-card-intro">{{ feed.introduction }}</p>
      <div v-if="feed.tags && feed.tags.length" class="feed-card-tags">
        <a-tag v-for="tag in feed.tags" :key="tag" size="small">{{ tag }}</a-tag>
      </div>
      <div class="feed-card-social">
        <span class="feed-card-stat">
          <EyeOutlined />
          <span>{{ formatCount(feed.socialInfo?.viewCount) }}</span>
        </span>
        <span class="feed-card-stat">
          <HeartOutlined />
          <span>{{ formatCount(feed.socialInfo?.likeCount) }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { EyeOutlined, HeartOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { formatCount } from '@/utils/formatCount'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

interface Props {
  feed: API.FeedVO
}

const props = defineProps<Props>()
const router = useRouter()

const formatRelativeTime = (time?: string) => {
  if (!time) return ''
  const d = dayjs(time)
  const now = dayjs()
  // 超过 7 天显示日期
  if (now.diff(d, 'day') >= 7) {
    return d.format('MM-DD HH:mm')
  }
  return d.fromNow()
}

const onImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src =
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAwIiBoZWlnaHQ9IjQ1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iODAwIiBoZWlnaHQ9IjQ1MCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjY2NjIiBmb250LXNpemU9IjE2Ij7lm77niYc8L3RleHQ+PC9zdmc+'
}

const goUserProfile = () => {
  if (props.feed.userId) {
    router.push(`/user/${props.feed.userId}`)
  }
}

const goPictureDetail = () => {
  if (props.feed.id) {
    router.push(`/picture/${props.feed.id}`)
  }
}
</script>

<style scoped>
.feed-card {
  position: relative;
  background: var(--surface-primary, #FFFFFF);
  border-radius: var(--radius-card, 12px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
  margin-bottom: 20px;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.feed-card:hover {
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.08));
}

/* 新内容圆点 */
.feed-card-new-dot {
  position: absolute;
  top: 16px;
  left: 12px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent, #33A1C9);
  z-index: 2;
}

/* ===== 作者栏 ===== */
.feed-card-author {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 12px;
}

.feed-card-avatar {
  cursor: pointer;
  flex-shrink: 0;
  transition: opacity 0.2s ease;
}

.feed-card-avatar:hover {
  opacity: 0.75;
}

.feed-card-author-info {
  min-width: 0;
  flex: 1;
}

.feed-card-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--fg-primary, #1A1A1A);
  cursor: pointer;
  transition: color 0.2s ease;
}

.feed-card-username:hover {
  color: var(--accent, #33A1C9);
}

.feed-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.feed-card-category {
  line-height: 18px;
  padding: 0 6px;
  font-size: 11px;
}

.feed-card-time {
  font-size: 13px;
  color: var(--fg-muted, #9CA3AF);
}

/* ===== 图片区域 ===== */
.feed-card-image-wrapper {
  background: #fafafa;
  cursor: pointer;
  overflow: hidden;
}

.feed-card-image {
  display: block;
  width: 100%;
  max-height: 520px;
  object-fit: contain;
  background: #fafafa;
  transition: transform 0.3s ease;
}

.feed-card:hover .feed-card-image {
  transform: scale(1.01);
}

/* ===== 底部信息栏 ===== */
.feed-card-body {
  padding: 14px 20px 16px;
}

.feed-card-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--fg-primary, #1A1A1A);
  margin: 0 0 4px;
  line-height: 1.5;
}

.feed-card-intro {
  font-size: 14px;
  color: var(--fg-secondary, #4B5563);
  margin: 0 0 10px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.feed-card-social {
  display: flex;
  align-items: center;
  gap: 16px;
}

.feed-card-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--fg-muted, #9CA3AF);
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .feed-card-author {
    padding: 12px 14px 10px;
  }

  .feed-card-body {
    padding: 10px 14px 14px;
  }

  .feed-card-image {
    max-height: 400px;
  }
}
</style>
