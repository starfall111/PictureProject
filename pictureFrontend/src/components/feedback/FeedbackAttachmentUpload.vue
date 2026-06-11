<template>
  <a-upload
    v-model:file-list="fileList"
    :custom-request="handleUpload"
    :before-upload="beforeUpload"
    list-type="picture-card"
    :max-count="5"
    accept=".png,.jpg,.jpeg,.gif"
    @remove="handleRemove"
  >
    <div v-if="fileList.length < 5">
      <plus-outlined />
      <div style="margin-top: 8px">上传附件</div>
    </div>
  </a-upload>
  <div class="upload-tip">最多 5 张，支持 PNG/JPG/JPEG/GIF，单张不超过 5MB</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { feedbackControllerUploadAttachment } from '@/api/feedbackController'

interface Props {
  modelValue?: number[]
}
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', ids: number[]): void
}>()

const fileList = ref<UploadFile[]>([])
const attachmentIds = ref<number[]>(props.modelValue ?? [])

watch(
  () => props.modelValue,
  (val) => {
    if (val) attachmentIds.value = val
  },
)

const beforeUpload = (file: File) => {
  const isValidType = ['image/png', 'image/jpeg', 'image/gif'].includes(file.type)
  if (!isValidType) {
    message.error('仅支持 PNG/JPG/JPEG/GIF 格式')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUpload = async (options: any) => {
  const { file, onSuccess, onError } = options
  try {
    const res = await feedbackControllerUploadAttachment({ file })
    if (res.data?.code === 0 && res.data?.data) {
      const id = res.data.data.id!
      attachmentIds.value.push(id)
      emit('update:modelValue', attachmentIds.value)
      onSuccess(res, file)
    } else {
      message.error('上传失败')
      onError(new Error('上传失败'))
    }
  } catch (e: any) {
    message.error('上传失败')
    onError(e)
  }
}

const handleRemove = (file: UploadFile) => {
  const idx = fileList.value.indexOf(file)
  if (idx > -1 && attachmentIds.value[idx] !== undefined) {
    attachmentIds.value.splice(idx, 1)
    emit('update:modelValue', attachmentIds.value)
  }
}
</script>

<style scoped>
.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
