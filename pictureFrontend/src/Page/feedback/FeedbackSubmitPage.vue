<template>
  <div class="feedback-submit-page">
    <div class="page-container">
      <div class="page-header">
        <a-button type="text" @click="router.back()">
          <template #icon><arrow-left-outlined /></template>
          返回
        </a-button>
        <h2>提交反馈</h2>
      </div>

      <a-card>
        <a-form layout="vertical">
          <!-- 反馈类型 -->
          <a-form-item label="反馈类型" required>
            <FeedbackTypeSelector v-model="form.type" />
          </a-form-item>

          <!-- 标题 -->
          <a-form-item label="标题" required>
            <a-input
              v-model:value="form.title"
              :maxlength="100"
              show-count
              placeholder="请简要描述您的问题或建议"
            />
          </a-form-item>

          <!-- 详细描述 -->
          <a-form-item label="详细描述" required>
            <a-textarea
              v-model:value="form.content"
              :rows="5"
              :maxlength="2000"
              show-count
              placeholder="请详细描述您遇到的问题或建议..."
            />
          </a-form-item>

          <!-- 附件上传 -->
          <a-form-item label="附件（可选）">
            <FeedbackAttachmentUpload v-model="form.attachmentIds" />
          </a-form-item>

          <!-- 关联图片 -->
          <a-form-item label="关联图片（可选）">
            <a-input-number
              v-model:value="form.relatedPictureId"
              placeholder="输入图片 ID"
              style="width: 200px"
            />
          </a-form-item>

          <!-- 关联用户 -->
          <a-form-item label="关联用户（可选）">
            <a-input-number
              v-model:value="form.relatedUserId"
              placeholder="输入用户 ID"
              style="width: 200px"
            />
          </a-form-item>

          <!-- 匿名开关 -->
          <a-form-item>
            <a-switch v-model:checked="form.isAnonymous" />
            <span style="margin-left: 8px">匿名提交</span>
          </a-form-item>

          <!-- 提交 -->
          <a-form-item>
            <a-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="!canSubmit"
              @click="handleSubmit"
              block
            >
              提交反馈
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <div class="page-footer-tip">
        我们会在 1-3 个工作日内处理您的反馈
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import FeedbackTypeSelector from '@/components/feedback/FeedbackTypeSelector.vue'
import FeedbackAttachmentUpload from '@/components/feedback/FeedbackAttachmentUpload.vue'
import { feedbackControllerSubmitFeedback } from '@/api/feedbackController'

const router = useRouter()

const form = ref({
  type: '',
  title: '',
  content: '',
  attachmentIds: [] as number[],
  relatedPictureId: undefined as number | undefined,
  relatedUserId: undefined as number | undefined,
  isAnonymous: false,
})

const submitting = ref(false)

const canSubmit = computed(() => {
  return form.value.type && form.value.title.trim() && form.value.content.trim()
})

const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const res = await feedbackControllerSubmitFeedback({
      type: form.value.type,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      attachmentIds: form.value.attachmentIds,
      relatedPictureId: form.value.relatedPictureId,
      relatedUserId: form.value.relatedUserId,
      isAnonymous: form.value.isAnonymous,
    })
    if (res.data?.code === 0 && res.data?.data) {
      message.success('反馈提交成功')
      router.push(`/feedback/${res.data.data}`)
    } else {
      message.error(res.data?.message ?? '提交失败')
    }
  } catch {
    message.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-submit-page {
  padding: 24px;
}
.page-container {
  max-width: 800px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
}
.page-footer-tip {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin-top: 16px;
}
@media (max-width: 576px) {
  .feedback-submit-page {
    padding: 12px;
  }
}
</style>
