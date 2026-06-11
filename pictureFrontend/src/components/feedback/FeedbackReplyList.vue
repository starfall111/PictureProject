<template>
  <div class="reply-list">
    <a-empty v-if="!replies?.length" description="暂无回复，请耐心等待">
      <template #image>
        <message-outlined style="font-size: 48px; color: #bfbfbf" />
      </template>
    </a-empty>
    <div v-for="reply in replies" :key="reply.id" class="reply-item" :class="getReplyClass(reply)">
      <!-- 内部备注（仅管理端） -->
      <div v-if="reply.replyType === 'INTERNAL_NOTE' && admin" class="reply-bubble internal-note">
        <div class="reply-header">
          <a-tag color="orange">内部备注</a-tag>
          <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
        </div>
        <div class="reply-content">{{ reply.content }}</div>
      </div>
      <!-- 正常回复 -->
      <div v-else-if="reply.replyType !== 'INTERNAL_NOTE'" class="reply-bubble" :class="isUserReply(reply) ? 'user-bubble' : 'admin-bubble'">
        <div class="reply-header">
          <a-avatar :size="28" :src="reply.userAvatar">
            {{ reply.userName?.charAt(0) }}
          </a-avatar>
          <span class="reply-user">{{ reply.userName }}</span>
          <a-tag v-if="reply.replyType === 'ADMIN_REPLY'" color="blue" size="small">管理员</a-tag>
          <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
        </div>
        <div class="reply-content">{{ reply.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MessageOutlined } from '@ant-design/icons-vue'
import { formatTime } from '@/utils/formatTime'

interface Props {
  replies?: API.FeedbackReplyVO[]
  /** 管理端模式：显示内部备注 */
  admin?: boolean
}

withDefaults(defineProps<Props>(), {
  replies: () => [],
  admin: false,
})

const isUserReply = (reply: API.FeedbackReplyVO) => reply.replyType === 'USER_REPLY'

const getReplyClass = (reply: API.FeedbackReplyVO) => {
  if (reply.replyType === 'INTERNAL_NOTE') return 'note-align'
  return isUserReply(reply) ? 'left-align' : 'right-align'
}
</script>

<style scoped>
.reply-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.reply-item.left-align {
  align-self: flex-start;
  max-width: 80%;
}
.reply-item.right-align {
  align-self: flex-end;
  max-width: 80%;
}
.reply-item.note-align {
  align-self: stretch;
}
.reply-bubble {
  padding: 12px 16px;
  border-radius: 12px;
}
.user-bubble {
  background: #f5f5f5;
  border-bottom-left-radius: 4px;
}
.admin-bubble {
  background: #e6f7ff;
  border-bottom-right-radius: 4px;
}
.internal-note {
  background: #fffbe6;
  border: 1px dashed #faad14;
  border-radius: 8px;
}
.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.reply-user {
  font-weight: 500;
  font-size: 13px;
}
.reply-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}
.reply-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
