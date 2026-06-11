<template>
  <div id="batchPicturePage" :class="{ 'immersive-layout': isImmersive }">
    <!-- 沉浸式双栏布局 -->
    <template v-if="isImmersive">
      <div class="immersive-batch-container">
        <!-- 左栏：配置表单 -->
        <div class="immersive-batch-left">
          <h2 style="margin-bottom: 8px">{{ isViewMode ? '任务详情' : '批量获取图片' }}</h2>
          <a-typography-text type="secondary" style="display: block; margin-bottom: 16px">
            {{ isViewMode ? '查看批量获取任务的详细信息和进度' : '通过关键字搜索并批量获取图片到您的空间' }}
          </a-typography-text>
          <BatchForm
            :formData="formData"
            :categoryOptions="categoryOptions"
            :tagOptions="tagOptions"
            :spaces="spaces"
            :spacesLoading="spacesLoading"
            :isAdmin="isAdmin"
            :canSubmit="canSubmit"
            :loading="submitStatus === 'loading'"
            :readonly="isViewMode"
            @submit="handleSubmit"
            @openFeedback="openFeedback"
            @createSpace="goCreateSpace"
          />
        </div>
        <!-- 右栏：结果卡片 -->
        <div class="immersive-batch-right">
          <BatchResultCard
            :status="resultCardStatus"
            :resultCount="currentTask?.successCount ?? resultCount"
            :errorMessage="currentTask?.message ?? errorMessage"
            :spaceId="formData.spaceId"
            :spaceName="selectedSpaceName"
            :wsProgress="derivedWsProgress"
            :task="currentTask"
            @retry="handleRetry"
            @reset="handleReset"
          />
        </div>
      </div>
    </template>

    <!-- 默认布局 -->
    <template v-else>
      <h2 style="margin-bottom: 8px">{{ isViewMode ? '任务详情' : '批量获取图片' }}</h2>
      <a-typography-text type="secondary" style="display: block; margin-bottom: 24px">
        {{ isViewMode ? '查看批量获取任务的详细信息和进度' : '通过关键字搜索并批量获取图片到您的空间' }}
      </a-typography-text>

      <a-row :gutter="24">
        <!-- 左侧表单 -->
        <a-col :xs="24" :md="16">
          <BatchForm
            :formData="formData"
            :categoryOptions="categoryOptions"
            :tagOptions="tagOptions"
            :spaces="spaces"
            :spacesLoading="spacesLoading"
            :isAdmin="isAdmin"
            :canSubmit="canSubmit"
            :loading="submitStatus === 'loading'"
            :readonly="isViewMode"
            @submit="handleSubmit"
            @openFeedback="openFeedback"
            @createSpace="goCreateSpace"
          />
        </a-col>
        <!-- 右侧结果 -->
        <a-col :xs="24" :md="8">
          <BatchResultCard
            :status="resultCardStatus"
            :resultCount="currentTask?.successCount ?? resultCount"
            :errorMessage="currentTask?.message ?? errorMessage"
            :spaceId="formData.spaceId"
            :spaceName="selectedSpaceName"
            :wsProgress="derivedWsProgress"
            :task="currentTask"
            @retry="handleRetry"
            @reset="handleReset"
          />
        </a-col>
      </a-row>
    </template>

    <!-- 反馈弹窗 -->
    <FeedbackModal
      v-model:visible="feedbackVisible"
      :defaultTitle="feedbackTitle"
      defaultType="FEATURE"
      @success="onFeedbackSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { tagControllerListTag } from '@/api/tagController'
import { categoryControllerListCategory } from '@/api/categoryController'
import { spaceControllerGetSpaceByUserId } from '@/api/spaceController'
import { userLoginUserStore } from '@/stores/user'
import { useLayoutScheme } from '@/composables/useLayoutScheme'
import { useBatchTaskStore } from '@/stores/batchTask'
import FeedbackModal from '@/components/feedback/FeedbackModal.vue'
import BatchResultCard from '@/components/picture/BatchResultCard.vue'
import BatchForm from './BatchForm.vue'

// ==================== 路由 Props ====================
const props = defineProps<{
  id?: string
}>()

const { activeSchemeId } = useLayoutScheme()
const isImmersive = computed(() => activeSchemeId.value === 'scheme-1-immersive')
const router = useRouter()
const route = useRoute()
const batchTaskStore = useBatchTaskStore()

// ==================== 页面模式 ====================
const taskId = computed(() => props.id ? props.id : null)
const isViewMode = computed(() => taskId.value !== null)

// ==================== 表单数据 ====================
const formData = reactive<API.PictureUploadByBatchDTO & { spaceId?: number }>({
  count: 10,
  searchSource: 'bing',
})
const submitStatus = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const resultCount = ref(0)
const errorMessage = ref('')

// ==================== 用户与权限 ====================
const loginUserStore = userLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const userId = computed(() => loginUserStore.loginUser.id)

// ==================== 空间相关 ====================
const spaces = ref<API.SpaceVO[]>([])
const spacesLoading = ref(false)

const selectedSpaceName = computed(() => {
  if (!formData.spaceId) return '公共图库'
  const space = spaces.value.find(s => s.id === formData.spaceId)
  return space?.spaceName ?? '公共图库'
})

const insufficientSpace = computed(() => {
  if (!formData.spaceId || !formData.count) return false
  const space = spaces.value.find(s => s.id === formData.spaceId)
  if (!space) return false
  const remaining = (space.maxCount ?? 0) - (space.totalCount ?? 0)
  return remaining < (formData.count ?? 0)
})

const canSubmit = computed(() => {
  if (!formData.searchText?.trim()) return false
  if (insufficientSpace.value) return false
  return true
})

const fetchSpaces = async () => {
  if (!userId.value) return
  spacesLoading.value = true
  try {
    const res = await spaceControllerGetSpaceByUserId({ id: userId.value! })
    if (res.data.code === 0 && res.data.data) {
      spaces.value = res.data.data
    }
  } finally {
    spacesLoading.value = false
  }
}

// ==================== 分类/标签选项 ====================
const categoryOptions = ref<{ value: number; label: string }[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

const getTagCategoryOptions = async () => {
  const [resTag, resCategory] = await Promise.all([
    tagControllerListTag(),
    categoryControllerListCategory(),
  ])
  if (resCategory.data.code === 0 && resCategory.data.data) {
    categoryOptions.value = resCategory.data.data.map((d: any) => ({
      value: d.id,
      label: d.name,
    }))
  }
  if (resTag.data.code === 0 && resTag.data.data) {
    tagOptions.value = resTag.data.data.map((d: any) => ({
      value: d.name,
      label: d.name,
    }))
  }
}

// ==================== 反馈弹窗 ====================
const feedbackVisible = ref(false)
const feedbackTitle = ref('')

const openFeedback = (type: 'category' | 'tag') => {
  feedbackTitle.value = type === 'category' ? '建议新增分类' : '建议新增标签'
  feedbackVisible.value = true
}

const onFeedbackSuccess = () => {
  message.success('感谢您的反馈，我们会尽快处理')
}

// ==================== 当前任务 & 结果卡片状态 ====================
const currentTask = computed(() => {
  if (!taskId.value) return null
  return batchTaskStore.getTask(taskId.value).value
})

/** 从 task 中派生 WebSocket 进度 */
const derivedWsProgress = computed(() => {
  const task = currentTask.value
  if (!task || task.wsTotal === undefined) return undefined
  return {
    completed: task.wsFinished ?? 0,
    total: task.wsTotal,
    success: task.wsSuccess ?? 0,
    fail: task.wsFail ?? 0,
  }
})

/** 结果卡片状态 */
const resultCardStatus = computed(() => {
  // 创建模式下的提交状态
  if (submitStatus.value === 'loading' && !taskId.value) return 'loading'

  const task = currentTask.value
  if (!task) return submitStatus.value

  const s = task.status
  if (s === 'PENDING' || s === 'PROCESSING') return 'loading'
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'error'
  return submitStatus.value
})

// ==================== 提交/结果操作 ====================
const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitStatus.value = 'loading'
  try {
    const taskVO = await batchTaskStore.submitTask({ ...formData })
    // 跳转到查看模式
    router.replace(`/add_picture/batch/${taskVO.taskId}`)
  } catch (e: any) {
    errorMessage.value = e.message ?? '网络错误'
    submitStatus.value = 'error'
  }
}

const handleRetry = () => {
  submitStatus.value = 'idle'
  handleSubmit()
}

const handleReset = () => {
  submitStatus.value = 'idle'
  formData.searchText = undefined
  formData.count = 10
  formData.profile = undefined
  formData.categoryId = undefined
  formData.tags = undefined
  // 如果在查看模式，回到创建模式
  if (isViewMode.value) {
    router.replace('/add_picture/batch')
  }
}

// ==================== 空间创建跳转 ====================
const goCreateSpace = () => {
  router.push('/add_space')
}

// ==================== 页面初始化 ====================
onMounted(async () => {
  await loginUserStore.getLoginUser(true)
  const loginUser = loginUserStore.loginUser

  // 初始化 store（连接 WebSocket）
  batchTaskStore.initialize()

  // 手机号校验
  if (!loginUser.userPhone && loginUser.userRole !== 'admin') {
    Modal.confirm({
      title: '请先绑定手机号',
      content: '批量获取图片前需要绑定手机号，是否前往个人中心绑定？',
      okText: '去绑定',
      cancelText: '返回主页',
      onOk: () => router.push('/user/center'),
      onCancel: () => router.push('/'),
    })
    return
  }

  // 并行加载数据
  await Promise.all([
    getTagCategoryOptions(),
    fetchSpaces(),
  ])

  if (isViewMode.value && taskId.value) {
    // 查看模式：拉取任务，填充表单
    batchTaskStore.setViewingTaskId(taskId.value)
    await batchTaskStore.fetchTask(taskId.value)
    fillFormFromTask()
  } else {
    // 创建模式：从 query 参数回填 spaceId（从个人空间跳转来）
    const querySpaceId = route.query.spaceId
    if (querySpaceId) {
      formData.spaceId = querySpaceId
    }
  }
})

onUnmounted(() => {
  batchTaskStore.setViewingTaskId(null)
})

/** 从任务数据填充表单（只读模式展示） */
function fillFormFromTask() {
  const task = currentTask.value
  if (!task) return
  // BatchTaskVO 中没有 searchText 等字段，
  // 这些字段需要从 AdminBatchTaskVO 或在 API 扩展后才能获取
  // 目前仅填充已有的空间信息
  if (task.status === 'COMPLETED' || task.status === 'FAILED') {
    // 已完成任务无需填充表单细节
  }
}
</script>

<style scoped>
#batchPicturePage {
  max-width: 960px;
  margin: 0 auto;
}

/* 沉浸式布局 */
#batchPicturePage.immersive-layout {
  max-width: 100%;
}

.immersive-batch-container {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 24px;
  align-items: start;
}

.immersive-batch-right {
  position: sticky;
  top: 80px;
}

@media (max-width: 640px) {
  .immersive-batch-container {
    grid-template-columns: 1fr;
  }
}
</style>
