<template>
  <a-modal
    :open="open"
    title="举报"
    :footer="null"
    :width="500"
    @cancel="handleClose"
    :maskClosable="false"
  >
    <!-- 步骤引导 -->
    <a-steps :current="step" size="small" style="margin-bottom: 24px">
      <a-step title="选择原因" />
      <a-step title="补充描述" />
      <a-step title="完成" />
    </a-steps>

    <!-- Step 1：选择原因 -->
    <div v-if="step === 0">
      <ReportReasonSelector v-model="form.reasonType" />
      <div class="step-actions">
        <a-button :disabled="!form.reasonType" type="primary" @click="step = 1">
          下一步
        </a-button>
      </div>
    </div>

    <!-- Step 2：补充描述 -->
    <div v-if="step === 1">
      <div class="target-preview" v-if="targetPreview">
        <span class="preview-label">被举报对象：</span>
        <a-image
          v-if="targetType === 'PICTURE'"
          :src="targetPreview"
          :width="80"
          :height="60"
          style="border-radius: 6px; object-fit: cover"
        />
        <span v-else>{{ targetPreview }}</span>
      </div>
      <a-textarea
        v-model:value="form.description"
        :rows="4"
        :maxlength="500"
        show-count
        placeholder="请补充描述（选填）..."
        style="margin-top: 12px"
      />
      <a-alert
        message="恶意举报可能导致账号受限"
        type="warning"
        show-icon
        style="margin-top: 12px"
      />
      <div class="step-actions">
        <a-button @click="step = 0">上一步</a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">
          提交举报
        </a-button>
      </div>
    </div>

    <!-- Step 3：完成 -->
    <div v-if="step === 2">
      <a-result status="success" title="举报已提交" sub-title="我们会尽快审核处理">
        <template #extra>
          <a-button type="primary" @click="handleClose">我知道了</a-button>
        </template>
      </a-result>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import ReportReasonSelector from '@/components/report/ReportReasonSelector.vue'
import { reportControllerSubmitReport } from '@/api/reportController'

interface Props {
  open: boolean
  targetType?: string
  targetId?: number
  /** 用于预览：图片传缩略图 URL，用户传用户名 */
  targetPreview?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'success'): void
}>()

const step = ref(0)
const submitting = ref(false)
const form = ref({
  reasonType: '',
  description: '',
})

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await reportControllerSubmitReport({
      targetType: props.targetType,
      targetId: props.targetId,
      reasonType: form.value.reasonType,
      description: form.value.description || undefined,
    })
    if (res.data?.code === 0) {
      step.value = 2
      emit('success')
    } else {
      message.error(res.data?.message ?? '举报失败')
    }
  } catch {
    message.error('举报失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  step.value = 0
  form.value = { reasonType: '', description: '' }
  emit('update:open', false)
}
</script>

<style scoped>
.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
.target-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-label {
  font-size: 13px;
  color: #666;
}
</style>
