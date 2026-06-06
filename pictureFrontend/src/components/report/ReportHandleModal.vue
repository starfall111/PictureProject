<template>
  <a-modal
    :open="open"
    title="处理举报"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
    ok-text="确认处理"
    cancel-text="取消"
    width="520px"
  >
    <div class="handle-form">
      <a-form layout="vertical">
        <a-form-item label="处理结果" required>
          <a-radio-group v-model:value="handleResult">
            <a-radio
              v-for="opt in REPORT_HANDLE_RESULT_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          v-if="needsBanDuration"
          label="封禁天数"
          required
        >
          <a-input-number
            v-model:value="banDuration"
            :min="1"
            :max="3650"
            placeholder="请输入封禁天数"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="处理备注">
          <a-textarea
            v-model:value="handleReason"
            :rows="3"
            :maxlength="500"
            show-count
            placeholder="请输入处理原因..."
          />
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { REPORT_HANDLE_RESULT_OPTIONS } from '@/constants/report'
import { adminReportControllerHandleReport } from '@/api/adminReportController'

interface Props {
  open: boolean
  reportId?: number
  targetType?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'success'): void
}>()

const handleResult = ref<string>('')
const handleReason = ref('')
const banDuration = ref<number>(7)
const loading = ref(false)

const needsBanDuration = computed(() =>
  ['BAN_TEMP_3', 'BAN_TEMP_7', 'BAN_TEMP_30'].includes(handleResult.value),
)

watch(
  () => props.open,
  (val) => {
    if (val) {
      handleResult.value = ''
      handleReason.value = ''
      banDuration.value = 7
    }
  },
)

watch(handleResult, (val) => {
  const banMap: Record<string, number> = { BAN_TEMP_3: 3, BAN_TEMP_7: 7, BAN_TEMP_30: 30 }
  if (banMap[val]) {
    banDuration.value = banMap[val]
  }
})

const handleOk = async () => {
  if (!handleResult.value) {
    message.warning('请选择处理结果')
    return
  }
  loading.value = true
  try {
    await adminReportControllerHandleReport({
      reportId: props.reportId,
      handleResult: handleResult.value,
      handleReason: handleReason.value,
      banDuration: needsBanDuration.value ? banDuration.value : undefined,
    })
    message.success('处理成功')
    emit('update:open', false)
    emit('success')
  } catch {
    message.error('处理失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style scoped>
.handle-form {
  padding: 8px 0;
}
</style>
