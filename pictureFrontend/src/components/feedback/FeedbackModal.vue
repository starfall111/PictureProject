<template>
  <a-modal
    :open="visible"
    title="提交反馈"
    :confirm-loading="submitting"
    @ok="handleSubmit"
    @cancel="handleClose"
    ok-text="提交"
    cancel-text="取消"
  >
    <a-form layout="vertical" :model="form">
      <a-form-item label="标题" name="title">
        <a-input v-model:value="form.title" placeholder="请输入反馈标题" />
      </a-form-item>
      <a-form-item label="类型" name="type">
        <a-select v-model:value="form.type" :options="FEEDBACK_TYPE_OPTIONS" placeholder="请选择类型" />
      </a-form-item>
      <a-form-item label="内容" name="content">
        <a-textarea
          v-model:value="form.content"
          placeholder="请描述您想要的分类或标签..."
          :rows="4"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { feedbackControllerSubmitFeedback } from '@/api/feedbackController'
import { FEEDBACK_TYPE_OPTIONS } from '@/constants/feedback'

const props = defineProps<{
  visible: boolean
  /** 预填标题 */
  defaultTitle?: string
  /** 预填类型 */
  defaultType?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

const submitting = ref(false)

const form = reactive({
  title: '',
  type: 'FEATURE' as string,
  content: '',
})

// 打开弹窗时预填
watch(() => props.visible, (val) => {
  if (val) {
    form.title = props.defaultTitle ?? ''
    form.type = props.defaultType ?? 'FEATURE'
    form.content = ''
  }
})

const handleSubmit = async () => {
  if (!form.title.trim()) {
    message.warning('请输入标题')
    return
  }
  if (!form.content.trim()) {
    message.warning('请输入内容')
    return
  }
  submitting.value = true
  try {
    const res = await feedbackControllerSubmitFeedback({
      title: form.title,
      type: form.type,
      content: form.content,
    })
    if (res.data.code === 0) {
      message.success('反馈提交成功')
      emit('success')
      handleClose()
    } else {
      message.error('提交失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('提交失败：' + e.message)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>
