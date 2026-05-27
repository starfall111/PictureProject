<template>
  <div class="picture-form-card">
    <h3 class="form-card-title">图片信息</h3>
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="名称" name="name">
        <a-input v-model:value="formData.name" placeholder="请输入名称" />
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <a-textarea
          v-model:value="formData.introduction"
          placeholder="请输入简介"
          :rows="3"
          autoSize
          allowClear
        />
      </a-form-item>
      <a-form-item label="分类" name="categoryId">
        <a-select
          v-model:value="formData.categoryId"
          :options="categoryOptions"
          placeholder="请选择分类"
          allowClear
        />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="formData.tags"
          :options="tagOptions"
          mode="tags"
          placeholder="请输入标签"
          allowClear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">
          {{ isEdit ? '修改' : '创建' }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface Props {
  picture?: API.PictureVO
  categoryOptions: { value: number; label: string }[]
  tagOptions: { value: string; label: string }[]
  loading?: boolean
  isEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  isEdit: false,
})

const emit = defineEmits<{
  (e: 'submit', values: API.PictureEditDTO): void
}>()

const formData = reactive<API.PictureEditDTO>({})

watch(
  () => props.picture,
  (pic) => {
    if (pic) {
      formData.name = pic.name
      formData.introduction = pic.introduction
      formData.categoryId = pic.categoryId
      formData.tags = pic.tags
    }
  },
  { immediate: true }
)

const handleSubmit = () => {
  emit('submit', { ...formData })
}
</script>

<style scoped>
.picture-form-card {
  background: var(--surface-primary, #FFFFFF);
  border-radius: var(--radius-card, 12px);
  padding: 24px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
  height: fit-content;
}

.form-card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--fg-primary, #1A1A1A);
  margin: 0 0 20px;
}
</style>
