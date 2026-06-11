<template>
  <div class="reply-input">
    <a-textarea
      v-model:value="content"
      :rows="3"
      :maxlength="500"
      show-count
      placeholder="请输入回复内容..."
      @pressEnter="handleSubmit"
    />
    <div class="reply-actions">
      <a-button type="primary" :loading="loading" :disabled="!content.trim()" @click="handleSubmit">
        <template #icon><send-outlined /></template>
        发送
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { SendOutlined } from '@ant-design/icons-vue'

interface Props {
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<{
  (e: 'submit', content: string): void
}>()

const content = ref('')

const handleSubmit = () => {
  const trimmed = content.value.trim()
  if (!trimmed) return
  emit('submit', trimmed)
  content.value = ''
}
</script>

<style scoped>
.reply-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reply-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
