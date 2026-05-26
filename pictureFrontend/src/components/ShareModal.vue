<template>
  <a-modal
    v-model:open="visible"
    title="分享图片"
    :width="400"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="share-modal">
      <!-- 图片缩略图 -->
      <div class="share-preview">
        <img :src="picture?.thumbnailUrl ?? picture?.url" :alt="picture?.name" class="share-preview-img" />
        <div class="share-preview-name">{{ picture?.name ?? '未命名' }}</div>
      </div>
      <!-- 复制链接 -->
      <div class="share-link">
        <a-input-group compact>
          <a-input
            :value="shareLink"
            read-only
            style="width: calc(100% - 80px)"
          />
          <a-button type="primary" @click="copyLink">复制</a-button>
        </a-input-group>
      </div>
      <!-- 社交平台 -->
      <div class="share-platforms">
        <div class="share-platform-item" @click="shareToWeibo">
          <div class="platform-icon weibo">微</div>
          <span>微博</span>
        </div>
        <div class="share-platform-item" @click="shareToQQ">
          <div class="platform-icon qq">Q</div>
          <span>QQ</span>
        </div>
        <div class="share-platform-item" @click="copyLink">
          <div class="platform-icon link">链</div>
          <span>复制链接</span>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'

interface Props {
  open: boolean
  picture?: API.PictureVO
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
}>()

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const shareLink = computed(() => {
  if (!props.picture?.id) return ''
  return `${window.location.origin}/picture/${props.picture.id}`
})

const handleClose = () => {
  emit('update:open', false)
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    message.success('链接已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

const shareToWeibo = () => {
  const url = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(shareLink.value)}&title=${encodeURIComponent(props.picture?.name ?? '分享图片')}`
  window.open(url, '_blank', 'width=600,height=500')
}

const shareToQQ = () => {
  const url = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(shareLink.value)}&title=${encodeURIComponent(props.picture?.name ?? '分享图片')}`
  window.open(url, '_blank', 'width=600,height=500')
}
</script>

<style scoped>
.share-modal {
  padding: 8px 0;
}

.share-preview {
  text-align: center;
  margin-bottom: 16px;
}

.share-preview-img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
  margin-bottom: 8px;
}

.share-preview-name {
  font-size: 14px;
  color: #333;
}

.share-link {
  margin-bottom: 16px;
}

.share-platforms {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 12px 0;
}

.share-platform-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: transform 0.2s;
}

.share-platform-item:hover {
  transform: translateY(-2px);
}

.platform-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
}

.platform-icon.weibo {
  background: #e6162d;
}

.platform-icon.qq {
  background: #12b7f5;
}

.platform-icon.link {
  background: #52c41a;
}
</style>
