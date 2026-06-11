<template>
  <a-form layout="vertical" :model="formData">
    <!-- 只读提示 -->
    <a-alert
      v-if="readonly"
      message="正在查看任务详情，表单为只读状态"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 搜索关键字 -->
    <a-form-item label="搜索关键字" name="searchText" required>
      <a-input
        v-model:value="formData.searchText"
        placeholder="如：风景、猫咪、美食"
        :disabled="readonly"
      />
    </a-form-item>

    <!-- 搜索来源 -->
    <a-form-item label="搜索来源" name="searchSource">
      <a-select
        v-model:value="formData.searchSource"
        placeholder="请选择搜索来源"
        :disabled="readonly"
      >
        <a-select-option v-for="opt in SEARCH_SOURCE_OPTIONS" :key="opt.value" :value="opt.value">
          <div>
            <div>{{ opt.label }}</div>
            <div style="font-size: 12px; color: rgba(0,0,0,0.45)">{{ opt.description }}</div>
          </div>
        </a-select-option>
      </a-select>
      <template v-if="formData.searchSource === 'pexels'" #extra>
        <span>
          访问
          <a href="https://www.pexels.com/zh-cn/" target="_blank">Pexels 官网</a>
          了解更多——Pexles 图片质量很高,获取图片时间会较长
        </span>
      </template>
    </a-form-item>

    <!-- 获取数量 -->
    <a-form-item label="获取数量" name="count">
      <a-input-number
        v-model:value="formData.count"
        :min="1"
        :max="30"
        style="width: 100%"
        :disabled="readonly"
      />
      <template #extra>
        <span style="font-size: 12px; color: rgba(0,0,0,0.45)">建议单次不超过 20 张，过多可能导致超时</span>
      </template>
    </a-form-item>

    <!-- 名称前缀 -->
    <a-form-item label="名称前缀" name="profile">
      <a-input
        v-model:value="formData.profile"
        placeholder="请输入名称前缀，会自动补充序号"
        :disabled="readonly"
      />
      <template #extra>
        <span style="font-size: 12px; color: rgba(0,0,0,0.45)">留空则使用搜索关键字作为前缀</span>
      </template>
    </a-form-item>

    <!-- 保存目标 -->
    <a-divider orientation="left" style="font-size: 14px">保存目标</a-divider>
    <a-form-item label="选择空间" name="spaceId">
      <SpaceSelector
        v-model:modelValue="formData.spaceId"
        :spaces="spaces"
        :loading="spacesLoading"
        :requestCount="formData.count"
        :disabled="readonly"
        @createSpace="emit('createSpace')"
      />
      <template #extra>
        <span style="font-size: 12px; color: rgba(0,0,0,0.45)">不选择则保存到公共图库</span>
      </template>
    </a-form-item>

    <!-- 图片属性 -->
    <a-divider orientation="left" style="font-size: 14px">图片属性</a-divider>

    <!-- 分类 -->
    <a-form-item label="分类" name="categoryId">
      <a-select
        v-model:value="formData.categoryId"
        :options="categoryOptions"
        placeholder="请选择分类"
        allow-clear
        :disabled="readonly"
      />
      <div v-if="!readonly" style="margin-top: 4px">
        <a-button
          v-if="isAdmin"
          type="link"
          size="small"
          href="/admin/categoryManage"
          target="_blank"
        >
          去添加
        </a-button>
        <a-button v-else type="link" size="small" @click="emit('openFeedback', 'category')">
          没有想要的分类？去反馈
        </a-button>
      </div>
    </a-form-item>

    <!-- 标签 -->
    <a-form-item label="标签" name="tags">
      <a-select
        v-model:value="formData.tags"
        :options="tagOptions"
        mode="tags"
        placeholder="请输入或选择标签"
        allow-clear
        :disabled="readonly"
      />
      <div v-if="!readonly" style="margin-top: 4px">
        <a-button
          v-if="isAdmin"
          type="link"
          size="small"
          href="/admin/tagManage"
          target="_blank"
        >
          去添加
        </a-button>
        <a-button v-else type="link" size="small" @click="emit('openFeedback', 'tag')">
          没有想要的标签？去反馈
        </a-button>
      </div>
    </a-form-item>

    <!-- 提交按钮（只读模式下隐藏） -->
    <a-form-item v-if="!readonly" style="margin-top: 24px">
      <a-button
        type="primary"
        html-type="button"
        style="width: 100%"
        :loading="loading"
        :disabled="!canSubmit"
        @click="emit('submit')"
      >
        开始抓取
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import SpaceSelector from '@/components/space/SpaceSelector.vue'

const SEARCH_SOURCE_OPTIONS = [
  { value: 'bing', label: 'Bing 图片搜索', description: '通过 Bing 搜索引擎获取图片' },
  {
    value: 'pexels',
    label: 'Pexels 高质量图片',
    description: '免费高质量图片平台，为设计师、博客作者等提供专业照片素材',
  },
]

defineProps<{
  formData: API.PictureUploadByBatchDTO & { spaceId?: number }
  categoryOptions: { value: number; label: string }[]
  tagOptions: { value: string; label: string }[]
  spaces: API.SpaceVO[]
  spacesLoading: boolean
  isAdmin: boolean
  canSubmit: boolean
  loading: boolean
  readonly?: boolean
}>()

const emit = defineEmits<{
  'submit': []
  'openFeedback': [type: 'category' | 'tag']
  'createSpace': []
}>()
</script>
