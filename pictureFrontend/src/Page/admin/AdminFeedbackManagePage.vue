<template>
  <div class="admin-feedback-page">
    <div class="page-header">
      <h2>反馈管理</h2>
      <a-button
        v-if="selectedRowKeys.length"
        danger
        @click="handleBatchClose"
      >
        批量关闭 ({{ selectedRowKeys.length }})
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <FeedbackStatsCards
      :stats="stats"
      :loading="statsLoading"
      :admin="true"
      :active-status="activeStatus"
      @filter="handleFilterByStatus"
      style="margin-bottom: 20px"
    />

    <!-- 筛选面板 -->
    <a-card size="small" style="margin-bottom: 16px">
      <a-row :gutter="[16, 12]">
        <a-col :xs="24" :sm="8" :md="6">
          <a-select
            v-model:value="query.status"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            :options="FEEDBACK_STATUS_OPTIONS"
          />
        </a-col>
        <a-col :xs="24" :sm="8" :md="6">
          <a-select
            v-model:value="query.type"
            placeholder="类型"
            allow-clear
            style="width: 100%"
            :options="FEEDBACK_TYPE_OPTIONS"
          />
        </a-col>
        <a-col :xs="24" :sm="8" :md="6">
          <a-select
            v-model:value="query.priority"
            placeholder="优先级"
            allow-clear
            style="width: 100%"
            :options="FEEDBACK_PRIORITY_OPTIONS"
          />
        </a-col>
        <a-col :xs="24" :sm="24" :md="6">
          <a-space>
            <a-button type="primary" @click="fetchList">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="{
        current: current,
        total: total,
        pageSize: pageSize,
        showQuickJumper: true,
        onChange: onPageChange,
      }"
      :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
      row-key="id"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <a @click="openDetail(record)">{{ record.title }}</a>
        </template>
        <template v-if="column.key === 'userName'">
          {{ record.isAnonymous ? '匿名用户' : record.userName }}
        </template>
        <template v-if="column.key === 'type'">
          <a-tag>{{ FEEDBACK_TYPE_MAP[record.type] ?? record.type }}</a-tag>
        </template>
        <template v-if="column.key === 'priority'">
          <a-tag :color="FEEDBACK_PRIORITY_COLOR[record.priority] ?? 'default'">
            {{ record.priority ?? '-' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="FEEDBACK_STATUS_COLOR[record.status ?? '']">
            {{ FEEDBACK_STATUS_MAP[record.status ?? ''] ?? record.status }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button
              v-if="record.status === 'PENDING'"
              type="link"
              size="small"
              @click="handleClaim(record.id)"
            >
              认领
            </a-button>
            <a-button
              v-if="record.status === 'PROCESSING'"
              type="link"
              size="small"
              @click="openReplyModal(record)"
            >
              回复
            </a-button>
            <a-button type="link" size="small" @click="openNoteModal(record)">备注</a-button>
            <a-dropdown>
              <a-button type="link" size="small">更多</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="openPriorityModal(record)">调整优先级</a-menu-item>
                  <a-menu-item v-if="record.status === 'PROCESSING'" @click="openTransferModal(record)">转派</a-menu-item>
                  <a-menu-item v-if="(record.relatedUserId || record.relatedPictureId) && (record.status === 'PROCESSING' || record.status === 'PENDING')" @click="openConvertModal(record)">转为举报</a-menu-item>
                  <a-menu-item v-if="record.status !== 'CLOSED' && record.status !== 'REJECTED'" @click="openCloseModal(record)">关闭</a-menu-item>
                  <a-menu-item v-if="record.status !== 'REJECTED' && record.status !== 'CLOSED'" @click="openRejectModal(record)">拒绝</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 回复弹窗 -->
    <a-modal
      v-model:open="replyModalOpen"
      title="回复反馈"
      :confirm-loading="replyLoading"
      @ok="handleReply"
    >
      <a-textarea v-model:value="replyContent" :rows="4" placeholder="请输入回复内容" />
    </a-modal>

    <!-- 备注弹窗 -->
    <a-modal
      v-model:open="noteModalOpen"
      title="内部备注"
      :confirm-loading="noteLoading"
      @ok="handleNote"
    >
      <a-textarea v-model:value="noteContent" :rows="4" placeholder="仅管理员可见" />
    </a-modal>

    <!-- 优先级调整弹窗 -->
    <a-modal
      v-model:open="priorityModalOpen"
      title="调整优先级"
      @ok="handlePriorityChange"
    >
      <a-radio-group v-model:value="newPriority">
        <a-radio v-for="opt in FEEDBACK_PRIORITY_OPTIONS" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </a-radio>
      </a-radio-group>
    </a-modal>

    <!-- 拒绝弹窗 -->
    <a-modal
      v-model:open="rejectModalOpen"
      title="拒绝反馈"
      :confirm-loading="rejectLoading"
      @ok="handleReject"
    >
      <a-textarea v-model:value="rejectReason" :rows="3" placeholder="请输入拒绝原因" />
    </a-modal>

    <!-- 转派弹窗 -->
    <a-modal
      v-model:open="transferModalOpen"
      title="转派反馈"
      :confirm-loading="transferLoading"
      @ok="handleTransfer"
    >
      <a-spin :spinning="adminListLoading">
        <a-select
          v-model:value="transferTargetAdminId"
          placeholder="请选择目标管理员"
          style="width: 100%"
          :options="adminList.map(a => ({ label: a.userName ?? `用户${a.id}`, value: a.id }))"
        />
      </a-spin>
    </a-modal>

    <!-- 转为举报弹窗 -->
    <a-modal
      v-model:open="convertModalOpen"
      title="转为举报"
      :confirm-loading="convertLoading"
      @ok="handleConvert"
    >
      <a-form layout="vertical">
        <a-form-item label="举报对象类型" required>
          <a-select
            v-model:value="convertForm.targetType"
            placeholder="选择对象类型"
            :options="REPORT_TARGET_TYPE_OPTIONS"
          />
        </a-form-item>
        <a-form-item label="对象ID" required>
          <a-input-number
            v-model:value="convertForm.targetId"
            placeholder="输入目标ID"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="举报原因" required>
          <a-select
            v-model:value="convertForm.reasonType"
            placeholder="选择举报原因"
            :options="REPORT_REASON_OPTIONS"
          />
        </a-form-item>
        <a-form-item label="补充描述">
          <a-textarea v-model:value="convertForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 管理员关闭弹窗 -->
    <a-modal
      v-model:open="closeModalOpen"
      title="关闭反馈"
      :confirm-loading="closeLoading"
      @ok="handleAdminClose"
    >
      <a-form layout="vertical">
        <a-form-item label="关闭原因" required>
          <a-textarea v-model:value="closeReason" :rows="3" placeholder="请输入关闭原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import FeedbackStatsCards from '@/components/feedback/FeedbackStatsCards.vue'
import { formatTime } from '@/utils/formatTime'
import { userLoginUserStore } from '@/stores/user'
import { userControllerListUserVoByQuery } from '@/api/userController'
import {
  adminFeedbackControllerGetAdminFeedbackList,
  adminFeedbackControllerGetAdminFeedbackStats,
  adminFeedbackControllerClaimFeedback,
  adminFeedbackControllerAdminReplyFeedback,
  adminFeedbackControllerAddInternalNote,
  adminFeedbackControllerUpdatePriority,
  adminFeedbackControllerRejectFeedback,
  adminFeedbackControllerBatchCloseFeedback,
  adminFeedbackControllerTransferFeedback,
  adminFeedbackControllerConvertToReport,
} from '@/api/adminFeedbackController'
import {
  FEEDBACK_STATUS_MAP,
  FEEDBACK_STATUS_COLOR,
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_TYPE_OPTIONS,
  FEEDBACK_PRIORITY_COLOR,
  FEEDBACK_PRIORITY_OPTIONS,
} from '@/constants/feedback'
import {
  REPORT_REASON_OPTIONS,
  REPORT_TARGET_TYPE_OPTIONS,
} from '@/constants/report'

const loginUserStore = userLoginUserStore()

const loading = ref(false)
const statsLoading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const activeStatus = ref('')
const selectedRowKeys = ref<number[]>([])

const stats = ref<any>({})
const query = ref<Record<string, string | undefined>>({
  status: undefined,
  type: undefined,
  priority: undefined,
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '标题', key: 'title', dataIndex: 'title', ellipsis: true },
  { title: '提交人', key: 'userName', dataIndex: 'userName', width: 100 },
  { title: '类型', key: 'type', dataIndex: 'type', width: 90 },
  { title: '优先级', key: 'priority', dataIndex: 'priority', width: 80 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' },
]

// 弹窗状态
const replyModalOpen = ref(false)
const replyLoading = ref(false)
const replyContent = ref('')
const currentFeedbackId = ref<number>(0)

const noteModalOpen = ref(false)
const noteLoading = ref(false)
const noteContent = ref('')

const priorityModalOpen = ref(false)
const newPriority = ref('')

const rejectModalOpen = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')

// 转派
const transferModalOpen = ref(false)
const transferLoading = ref(false)
const transferTargetAdminId = ref<number | undefined>(undefined)
const adminList = ref<any[]>([])
const adminListLoading = ref(false)

// 转举报
const convertModalOpen = ref(false)
const convertLoading = ref(false)
const convertForm = ref<any>({
  targetType: undefined,
  targetId: undefined,
  reasonType: undefined,
  description: '',
})

// 关闭
const closeModalOpen = ref(false)
const closeLoading = ref(false)
const closeReason = ref('')

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await adminFeedbackControllerGetAdminFeedbackStats()
    if (res.data?.code === 0 && res.data?.data) stats.value = res.data.data
  } finally {
    statsLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminFeedbackControllerGetAdminFeedbackList({
      dto: {
        current: current.value,
        pageSize,
        ...query.value,
      },
    })
    if (res.data?.code === 0 && res.data?.data) {
      list.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => {
  current.value = page
  fetchList()
}

const handleFilterByStatus = (status: string) => {
  activeStatus.value = activeStatus.value === status ? '' : status
  query.value.status = activeStatus.value || undefined
  current.value = 1
  fetchList()
}

const resetQuery = () => {
  query.value = { status: undefined, type: undefined, priority: undefined }
  activeStatus.value = ''
  current.value = 1
  fetchList()
}

const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

const openDetail = (record: any) => {
  window.open(`/feedback/${record.id}`, '_blank')
}

// 认领
const handleClaim = async (id: number) => {
  try {
    await adminFeedbackControllerClaimFeedback({ id })
    message.success('认领成功')
    fetchList()
  } catch {
    message.error('认领失败')
  }
}

// 回复
const openReplyModal = (record: any) => {
  currentFeedbackId.value = record.id
  replyContent.value = ''
  replyModalOpen.value = true
}
const handleReply = async () => {
  if (!replyContent.value.trim()) return message.warning('请输入回复内容')
  replyLoading.value = true
  try {
    await adminFeedbackControllerAdminReplyFeedback(
      { id: currentFeedbackId.value },
      { content: replyContent.value.trim() },
    )
    message.success('回复成功')
    replyModalOpen.value = false
    fetchList()
  } finally {
    replyLoading.value = false
  }
}

// 备注
const openNoteModal = (record: any) => {
  currentFeedbackId.value = record.id
  noteContent.value = ''
  noteModalOpen.value = true
}
const handleNote = async () => {
  if (!noteContent.value.trim()) return message.warning('请输入备注')
  noteLoading.value = true
  try {
    await adminFeedbackControllerAddInternalNote(
      { id: currentFeedbackId.value },
      { content: noteContent.value.trim() },
    )
    message.success('备注已添加')
    noteModalOpen.value = false
  } finally {
    noteLoading.value = false
  }
}

// 优先级
const openPriorityModal = (record: any) => {
  currentFeedbackId.value = record.id
  newPriority.value = record.priority ?? 'P3'
  priorityModalOpen.value = true
}
const handlePriorityChange = async () => {
  try {
    await adminFeedbackControllerUpdatePriority({
      id: currentFeedbackId.value,
      priority: newPriority.value,
    })
    message.success('优先级已更新')
    priorityModalOpen.value = false
    fetchList()
  } catch {
    message.error('更新失败')
  }
}

// 拒绝
const openRejectModal = (record: any) => {
  currentFeedbackId.value = record.id
  rejectReason.value = ''
  rejectModalOpen.value = true
}
const handleReject = async () => {
  if (!rejectReason.value.trim()) return message.warning('请输入拒绝原因')
  rejectLoading.value = true
  try {
    await adminFeedbackControllerRejectFeedback({
      id: currentFeedbackId.value,
      reason: rejectReason.value.trim(),
    })
    message.success('已拒绝')
    rejectModalOpen.value = false
    fetchList()
  } finally {
    rejectLoading.value = false
  }
}

// 转派
const openTransferModal = async (record: any) => {
  currentFeedbackId.value = record.id
  transferTargetAdminId.value = undefined
  transferModalOpen.value = true
  adminListLoading.value = true
  try {
    const res = await userControllerListUserVoByQuery({
      userRole: 'admin',
      pageSize: 100,
    })
    if (res.data?.code === 0 && res.data?.data) {
      adminList.value = (res.data.data.records ?? []).filter(
        (u: any) => u.id !== loginUserStore.loginUser.id,
      )
    }
  } finally {
    adminListLoading.value = false
  }
}
const handleTransfer = async () => {
  if (!transferTargetAdminId.value) return message.warning('请选择目标管理员')
  transferLoading.value = true
  try {
    await adminFeedbackControllerTransferFeedback({
      id: currentFeedbackId.value,
      targetHandlerId: transferTargetAdminId.value,
    })
    message.success('转派成功')
    transferModalOpen.value = false
    fetchList()
  } catch {
    message.error('转派失败')
  } finally {
    transferLoading.value = false
  }
}

// 转为举报
const openConvertModal = (record: any) => {
  currentFeedbackId.value = record.id
  // 自动回填关联信息
  let targetType: string | undefined
  let targetId: number | undefined
  if (record.relatedUserId) {
    targetType = 'USER'
    targetId = record.relatedUserId
  } else if (record.relatedPictureId) {
    targetType = 'PICTURE'
    targetId = record.relatedPictureId
  }
  convertForm.value = {
    targetType,
    targetId,
    reasonType: undefined,
    description: record.content ?? '',
  }
  convertModalOpen.value = true
}
const handleConvert = async () => {
  if (!convertForm.value.targetType) return message.warning('请选择举报对象类型')
  if (!convertForm.value.targetId) return message.warning('请输入对象ID')
  if (!convertForm.value.reasonType) return message.warning('请选择举报原因')
  convertLoading.value = true
  try {
    await adminFeedbackControllerConvertToReport(
      { id: currentFeedbackId.value },
      convertForm.value,
    )
    message.success('已转为举报')
    convertModalOpen.value = false
    fetchList()
  } catch {
    message.error('转换失败')
  } finally {
    convertLoading.value = false
  }
}

// 管理员关闭
const openCloseModal = (record: any) => {
  currentFeedbackId.value = record.id
  closeReason.value = ''
  closeModalOpen.value = true
}
const handleAdminClose = async () => {
  if (!closeReason.value.trim()) return message.warning('请输入关闭原因')
  closeLoading.value = true
  try {
    await adminFeedbackControllerBatchCloseFeedback(
      { reason: 'ADMIN_CLOSE' },
      [currentFeedbackId.value],
    )
    message.success('已关闭')
    closeModalOpen.value = false
    fetchList()
  } catch {
    message.error('关闭失败')
  } finally {
    closeLoading.value = false
  }
}

// 批量关闭
const handleBatchClose = async () => {
  try {
    await adminFeedbackControllerBatchCloseFeedback({ reason: '批量关闭' }, selectedRowKeys.value)
    message.success('批量关闭成功')
    selectedRowKeys.value = []
    fetchList()
  } catch {
    message.error('批量关闭失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.admin-feedback-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}
.page-header h2 {
  margin: 0;
}
@media (max-width: 576px) {
  .admin-feedback-page {
    padding: 12px;
  }
}
</style>
