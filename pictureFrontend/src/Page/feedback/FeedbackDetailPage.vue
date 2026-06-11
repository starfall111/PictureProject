<template>
  <div class="feedback-detail-page">
    <div class="page-container">
      <a-spin :spinning="loading">
        <template v-if="detail">
          <!-- 顶部信息卡片 -->
          <a-card class="info-card">
            <div class="info-header">
              <a-button type="text" @click="router.push('/feedback/list')">
                <template #icon><arrow-left-outlined /></template>
                返回列表
              </a-button>
            </div>
            <h2>{{ detail.title }}</h2>
            <div class="info-tags">
              <a-tag :color="FEEDBACK_STATUS_COLOR[detail.status ?? '']">
                {{ FEEDBACK_STATUS_MAP[detail.status ?? ''] ?? detail.status }}
              </a-tag>
              <a-tag>{{ FEEDBACK_TYPE_MAP[detail.type ?? ''] ?? detail.type }}</a-tag>
              <a-tag
                v-if="detail.priority"
                :color="FEEDBACK_PRIORITY_COLOR[detail.priority] ?? 'default'"
              >
                {{ detail.priority }}
              </a-tag>
              <a-tag v-if="detail.isAnonymous" color="default">匿名</a-tag>
            </div>
            <div class="info-meta">
              <span v-if="!detail.isAnonymous">{{ detail.userName }}</span>
              <span v-else>匿名用户</span>
              <span>提交于 {{ formatTime(detail.createTime) }}</span>
            </div>
            <a-divider />
            <div class="info-content">{{ detail.content }}</div>
          </a-card>

          <!-- 附件预览 -->
          <a-card v-if="detail.attachments?.length" title="附件" style="margin-top: 16px">
            <a-image-preview-group>
              <a-space>
                <a-image
                  v-for="att in detail.attachments"
                  :key="att.id"
                  :src="att.fileUrl"
                  :width="120"
                  :height="120"
                  style="border-radius: 8px; object-fit: cover"
                />
              </a-space>
            </a-image-preview-group>
          </a-card>

          <!-- 状态变更时间线 -->
          <a-card v-if="detail.statusLogs?.length" title="状态变更" style="margin-top: 16px">
            <FeedbackTimeline :logs="detail.statusLogs" />
          </a-card>

          <!-- 回复区 -->
          <a-card title="沟通记录" style="margin-top: 16px">
            <FeedbackReplyList :replies="detail.replies ?? []" :admin="isAdmin" />
            <template v-if="isCurrentUser">
              <a-divider v-if="detail.replies?.length" />
              <FeedbackReplyInput :loading="replying" @submit="handleReply" />
            </template>
          </a-card>

          <!-- 操作按钮（仅用户本人可见） -->
          <div class="action-bar" v-if="showActions && isCurrentUser">
            <a-button
              v-if="canReopen"
              @click="handleReopen"
              :loading="actionLoading"
            >
              重新打开
            </a-button>
            <a-button
              v-if="canConfirm"
              type="primary"
              @click="handleConfirm"
              :loading="actionLoading"
            >
              确认解决
            </a-button>
            <a-popconfirm
              v-if="canWithdraw"
              title="确定要撤回此反馈吗？此操作不可恢复"
              @confirm="handleWithdraw"
              ok-text="确定"
              cancel-text="取消"
            >
              <a-button danger :loading="actionLoading">撤回反馈</a-button>
            </a-popconfirm>
          </div>
        </template>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import FeedbackTimeline from '@/components/feedback/FeedbackTimeline.vue'
import FeedbackReplyList from '@/components/feedback/FeedbackReplyList.vue'
import FeedbackReplyInput from '@/components/feedback/FeedbackReplyInput.vue'
import { userLoginUserStore } from '@/stores/user'
import {
  feedbackControllerGetFeedbackDetail,
  feedbackControllerReplyFeedback,
  feedbackControllerReopenFeedback,
  feedbackControllerConfirmFeedback,
  feedbackControllerWithdrawFeedback,
} from '@/api/feedbackController'
import {
  FEEDBACK_STATUS_MAP,
  FEEDBACK_STATUS_COLOR,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_PRIORITY_COLOR,
} from '@/constants/feedback'
import { formatTime } from '@/utils/formatTime'
// import type { API } from '@/api/typings'

const router = useRouter()
const route = useRoute()
const feedbackId = route.params.id
const loginUserStore = userLoginUserStore()

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const isCurrentUser = computed(() => loginUserStore.loginUser.id === detail.value?.userId)

const loading = ref(false)
const replying = ref(false)
const actionLoading = ref(false)
const detail = ref<API.FeedbackVO | null>(null)

const showActions = computed(() => canReopen.value || canConfirm.value || canWithdraw.value)
const canReopen = computed(() => {
  const d = detail.value
  if (!d) return false
  return ['RESOLVED', 'CLOSED'].includes(d.status ?? '') && (d.reopenCount ?? 0) < 2
})
const canConfirm = computed(() => detail.value?.status === 'RESOLVED')
const canWithdraw = computed(() => detail.value?.status === 'PENDING')

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await feedbackControllerGetFeedbackDetail({ id: feedbackId })
    if (res.data?.code === 0 && res.data?.data) {
      detail.value = res.data.data
    } else {
      message.error('反馈不存在或已被删除')
      router.push('/feedback/list')
    }
  } finally {
    loading.value = false
  }
}

const handleReply = async (content: string) => {
  replying.value = true
  try {
    await feedbackControllerReplyFeedback({ id: feedbackId }, { content })
    message.success('回复成功')
    await fetchDetail()
  } catch {
    message.error('回复失败')
  } finally {
    replying.value = false
  }
}

const handleReopen = async () => {
  actionLoading.value = true
  try {
    const res = await feedbackControllerReopenFeedback({ id: feedbackId })
    if (res.data?.code === 0) {
      message.success('已重新打开')
      await fetchDetail()
    } else {
      message.error(res.data?.message ?? '操作失败')
    }
  } catch (e: any) {
    const errMsg = e?.response?.data?.message ?? e?.message ?? '操作失败'
    message.error(errMsg)
  } finally {
    actionLoading.value = false
  }
}

const handleConfirm = async () => {
  actionLoading.value = true
  try {
    const res = await feedbackControllerConfirmFeedback({ id: feedbackId })
    if (res.data?.code === 0) {
      message.success('已确认解决')
      await fetchDetail()
    } else {
      message.error(res.data?.message ?? '操作失败')
    }
  } catch (e: any) {
    const errMsg = e?.response?.data?.message ?? e?.message ?? '操作失败'
    message.error(errMsg)
  } finally {
    actionLoading.value = false
  }
}

const handleWithdraw = async () => {
  actionLoading.value = true
  try {
    await feedbackControllerWithdrawFeedback({ id: feedbackId })
    message.success('已撤回')
    router.push('/feedback/list')
  } catch {
    message.error('撤回失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.feedback-detail-page {
  padding: 24px;
}
.page-container {
  max-width: 800px;
  margin: 0 auto;
}
.info-header {
  margin-bottom: 8px;
}
.info-header h2 {
  margin: 0 0 12px;
}
.info-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.info-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}
.info-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-size: 14px;
}
.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  justify-content: center;
}
@media (max-width: 576px) {
  .feedback-detail-page {
    padding: 12px;
  }
}
</style>
