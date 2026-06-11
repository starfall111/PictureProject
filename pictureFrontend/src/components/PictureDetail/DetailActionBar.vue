<template>
  <div class="action-bar">
    <div class="action-left">
      <!-- 点赞 -->
      <span class="action-btn-large" :class="{ active: isLiked }" @click.stop="$emit('toggle-like')">
        <HeartFilled v-if="isLiked" class="action-icon liked" />
        <HeartOutlined v-else class="action-icon" />
        <span class="action-count">{{ formatCount(socialInfo?.likeCount) }}</span>
      </span>
      <!-- 收藏 -->
      <span class="action-btn-large" :class="{ active: isFavorited }" @click.stop="$emit('toggle-favorite')">
        <StarFilled v-if="isFavorited" class="action-icon favorited" />
        <StarOutlined v-else class="action-icon" />
        <span class="action-count">{{ formatCount(socialInfo?.favoriteCount) }}</span>
      </span>
      <!-- 分享 -->
      <span class="action-btn-large" @click.stop="$emit('share')">
        <ShareAltOutlined class="action-icon" />
        <span class="action-count">{{ formatCount(socialInfo?.shareCount) }}</span>
      </span>
      <!-- 浏览量（只读展示） -->
      <span class="action-btn-large action-btn-readonly">
        <EyeOutlined class="action-icon" />
        <span class="action-count">{{ formatCount(socialInfo?.viewCount) }}</span>
      </span>
      <!-- 下载量（只读展示） -->
      <span class="action-btn-large action-btn-readonly">
        <DownloadOutlined class="action-icon" />
        <span class="action-count">{{ formatCount(socialInfo?.downloadCount) }}</span>
      </span>
    </div>
    <div class="action-right">
      <a-button type="primary" size="large" @click="$emit('download')">
        <template #icon><DownloadOutlined /></template>
        免费下载
      </a-button>
      <a-dropdown v-if="canEdit">
        <a-button size="large">
          <template #icon><MoreOutlined /></template>
        </a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="$emit('edit')">
              <EditOutlined /> 编辑
            </a-menu-item>
            <a-menu-item @click="$emit('delete')">
              <DeleteOutlined /> 删除
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
      <a-button v-if="showReport" size="large" danger ghost @click="$emit('report')">
        <template #icon><WarningOutlined /></template>
        举报
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  HeartOutlined,
  HeartFilled,
  StarOutlined,
  StarFilled,
  ShareAltOutlined,
  DownloadOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import { formatCount } from '@/utils/formatCount'

interface Props {
  socialInfo?: API.PictureSocialVO
  isLiked: boolean
  isFavorited: boolean
  canEdit: boolean
  showReport?: boolean
}

defineProps<Props>()

defineEmits<{
  (e: 'toggle-like'): void
  (e: 'toggle-favorite'): void
  (e: 'share'): void
  (e: 'download'): void
  (e: 'edit'): void
  (e: 'delete'): void
  (e: 'report'): void
}>()
</script>

<style scoped>
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.action-btn-large {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 18px;
  color: #595959;
  transition: transform 0.2s ease;
  user-select: none;
}

.action-btn-readonly {
  cursor: default;
}

.action-btn-large:hover {
  transform: translateY(-2px);
}

.action-btn-readonly:hover {
  transform: none;
}

.action-icon {
  font-size: 24px;
  transition: color 0.2s ease;
}

.action-icon.liked {
  color: #ff4d4f;
  animation: heartbeat 0.4s ease;
}

.action-icon.favorited {
  color: #faad14;
  animation: starlight 0.4s ease;
}

.action-count {
  font-size: 18px;
  font-weight: 600;
}

.action-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 767px) {
  .action-bar {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .action-left {
    justify-content: space-around;
  }

  .action-right {
    display: flex;
    gap: 8px;
  }

  .action-right .ant-btn {
    flex: 1;
  }
}
</style>

<style>
@keyframes heartbeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.9); }
  75% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

@keyframes starlight {
  0% { transform: scale(1) rotate(0deg); }
  25% { transform: scale(1.3) rotate(15deg); }
  50% { transform: scale(0.9) rotate(-10deg); }
  75% { transform: scale(1.15) rotate(5deg); }
  100% { transform: scale(1) rotate(0deg); }
}
</style>
