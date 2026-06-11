<template>
  <a-modal
    :open="open"
    title="解封用户"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
    ok-text="确认解封"
    cancel-text="取消"
    width="440px"
  >
    <a-form layout="vertical">
      <a-form-item label="解封原因" required>
        <a-textarea
          v-model:value="unbanReason"
          :rows="4"
          :maxlength="500"
          show-count
          placeholder="请输入解封原因（必填）"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { adminBanControllerUnbanUser } from '@/api/adminBanController'

interface Props {
  open: boolean
  userId?: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'success'): void
}>()

const unbanReason = ref('')
const loading = ref(false)

watch(
  () => props.open,
  (val) => {
    if (val) unbanReason.value = ''
  },
)

const handleOk = async () => {
  if (!unbanReason.value.trim()) {
    message.warning('请填写解封原因')
    return
  }
  loading.value = true
  try {
    await adminBanControllerUnbanUser({
      userId: props.userId,
      unbanReason: unbanReason.value.trim(),
    })
    message.success('解封成功')
    emit('update:open', false)
    emit('success')
  } catch {
    message.error('解封失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>
