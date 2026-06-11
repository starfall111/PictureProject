<template>
  <div class="admin-batch-task-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>批量任务管理</h2>
      <a-button
        v-if="selectedRowKeys.length"
        danger
        @click="handleBatchDelete"
      >
        批量删除 ({{ selectedRowKeys.length }})
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <BatchTaskStatsCards
      :stats="stats"
      :loading="statsLoading"
      :active-status="query.status"
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
            :options="BATCH_TASK_STATUS_OPTIONS"
          />
        </a-col>
        <a-col :xs="24" :sm="8" :md="6">
          <a-select
            v-model:value="query.searchSource"
            placeholder="搜索来源"
            allow-clear
            style="width: 100%"
            :options="SEARCH_SOURCE_OPTIONS"
          />
        </a-col>
        <a-col :xs="24" :sm="8" :md="6">
          <a-input v-model:value="query.keyword" placeholder="搜索关键词" allow-clear />
        </a-col>
        <a-col :xs="24" :sm="12" :md="6">
          <a-range-picker
            v-model:value="dateRange"
            style="width: 100%"
            :placeholder="['开始时间', '结束时间']"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :md="6">
          <a-input-number v-model:value="query.userId" placeholder="用户ID" style="width: 100%" />
        </a-col>
        <a-col :xs="24" :sm="24" :md="6">
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- 数据表格 -->
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
      row-key="taskId"
      :scroll="{ x: 1400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'searchSource'">
          <a-tag>{{ SEARCH_SOURCE_MAP[record.searchSource] ?? record.searchSource }}</a-tag>
        </template>
        <template v-if="column.key === 'successCount'">
          <span style="color: #52c41a">{{ record.successCount ?? 0 }}</span>
        </template>
        <template v-if="column.key === 'failCount'">
          <span style="color: #f5222d">{{ record.failCount ?? 0 }}</span>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="BATCH_TASK_STATUS_COLOR[record.status]">
            {{ BATCH_TASK_STATUS_MAP[record.status] ?? record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'tags'">
          <template v-if="record.tags">
            <a-tag v-for="tag in record.tags.split(',')" :key="tag" size="small">{{ tag.trim() }}</a-tag>
          </template>
          <span v-else style="color: #999">-</span>
        </template>
        <template v-if="column.key === 'spaceId'">
          {{ record.spaceId ?? '公共图库' }}
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-if="column.dataIndex === 'finishTime'">
          {{ formatTime(record.finishTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openDetailDrawer(record)">详情</a-button>
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-button type="link" danger size="small" @click="handleDelete(record.taskId)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editModalOpen"
      title="编辑任务"
      :confirm-loading="editLoading"
      @ok="handleEdit"
    >
      <a-form layout="vertical">
        <a-form-item label="状态">
          <a-select v-model:value="editForm.status" :options="BATCH_TASK_STATUS_OPTIONS" />
        </a-form-item>
        <a-form-item label="标签">
          <a-input v-model:value="editForm.tags" placeholder="多个标签用逗号分隔" />
        </a-form-item>
        <a-form-item label="错误信息">
          <a-textarea v-model:value="editForm.errorMessage" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="drawerOpen" title="任务详情" width="520">
      <template v-if="detailRecord">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="任务ID">{{ detailRecord.taskId }}</a-descriptions-item>
          <a-descriptions-item label="用户ID">{{ detailRecord.userId }}</a-descriptions-item>
          <a-descriptions-item label="空间ID">{{ detailRecord.spaceId ?? '公共图库' }}</a-descriptions-item>
          <a-descriptions-item label="搜索关键词">{{ detailRecord.searchText }}</a-descriptions-item>
          <a-descriptions-item label="搜索来源">
            <a-tag>{{ SEARCH_SOURCE_MAP[detailRecord.searchSource] ?? detailRecord.searchSource }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="分类ID">{{ detailRecord.categoryId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="名称前缀">{{ detailRecord.namePrefix ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="标签">
            <template v-if="detailRecord.tags">
              <a-tag v-for="tag in detailRecord.tags.split(',')" :key="tag">{{ tag.trim() }}</a-tag>
            </template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="总数 / 成功 / 失败">
            {{ detailRecord.totalCount ?? 0 }} / <span style="color: #52c41a">{{ detailRecord.successCount ?? 0 }}</span> / <span style="color: #f5222d">{{ detailRecord.failCount ?? 0 }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="BATCH_TASK_STATUS_COLOR[detailRecord.status]">
              {{ BATCH_TASK_STATUS_MAP[detailRecord.status] ?? detailRecord.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detailRecord.errorMessage" label="错误信息">
            <span style="color: #f5222d">{{ detailRecord.errorMessage }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(detailRecord.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(detailRecord.updateTime) }}</a-descriptions-item>
          <a-descriptions-item label="完成时间">{{ formatDateTime(detailRecord.finishTime) }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { formatTime, formatDateTime } from '@/utils/formatTime'
import BatchTaskStatsCards from '@/components/batch/BatchTaskStatsCards.vue'
import {
  adminBatchTaskControllerGetAdminList,
  adminBatchTaskControllerGetAdminDetail,
  adminBatchTaskControllerGetAdminStats,
  adminBatchTaskControllerAdminUpdate,
  adminBatchTaskControllerAdminDelete,
} from '@/api/adminBatchTaskController'
import {
  BATCH_TASK_STATUS_MAP,
  BATCH_TASK_STATUS_COLOR,
  BATCH_TASK_STATUS_OPTIONS,
  SEARCH_SOURCE_MAP,
  SEARCH_SOURCE_OPTIONS,
} from '@/constants/batchTask'

// ---- 列表状态 ----
const loading = ref(false)
const list = ref<API.AdminBatchTaskVO[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10

// ---- 统计状态 ----
const statsLoading = ref(false)
const stats = ref<API.BatchTaskStatsVO>({})

// ---- 筛选状态 ----
const query = reactive({
  status: undefined as string | undefined,
  searchSource: undefined as string | undefined,
  keyword: undefined as string | undefined,
  userId: undefined as number | undefined,
})
const dateRange = ref<[any, any] | null>(null)

// ---- 多选 ----
const selectedRowKeys = ref<number[]>([])
const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

// ---- 弹窗状态 ----
const editModalOpen = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: undefined as number | undefined,
  status: undefined as string | undefined,
  tags: undefined as string | undefined,
  errorMessage: undefined as string | undefined,
})

const drawerOpen = ref(false)
const detailRecord = ref<API.AdminBatchTaskVO | null>(null)

// ---- 表格列 ----
const columns = [
  { title: '任务ID', dataIndex: 'taskId', width: 100 },
  { title: '用户ID', dataIndex: 'userId', width: 90 },
  { title: '空间ID', key: 'spaceId', width: 90 },
  { title: '搜索词', dataIndex: 'searchText', width: 180, ellipsis: true },
  { title: '来源', key: 'searchSource', width: 80 },
  { title: '总数', dataIndex: 'totalCount', width: 70 },
  { title: '成功', key: 'successCount', width: 70 },
  { title: '失败', key: 'failCount', width: 70 },
  { title: '状态', key: 'status', width: 100 },
  { title: '标签', key: 'tags', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '完成时间', dataIndex: 'finishTime', width: 160 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

// ---- 数据获取 ----
const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await adminBatchTaskControllerGetAdminStats()
    if (res.data?.code === 0 && res.data?.data) {
      stats.value = res.data.data
    }else{
      message.error("批量获取图片失败" + res.data.message)
    }
  } finally {
    statsLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminBatchTaskControllerGetAdminList({
      dto: {
        current: current.value,
        pageSize,
        status: query.status,
        searchSource: query.searchSource,
        keyword: query.keyword,
        userId: query.userId,
        startTime: dateRange.value?.[0]?.toISOString(),
        endTime: dateRange.value?.[1]?.toISOString(),
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

// ---- 交互 ----
const handleSearch = () => {
  current.value = 1
  fetchList()
}

const resetQuery = () => {
  query.status = undefined
  query.searchSource = undefined
  query.keyword = undefined
  query.userId = undefined
  dateRange.value = null
  current.value = 1
  fetchList()
}

const onPageChange = (page: number) => {
  current.value = page
  fetchList()
}

const handleFilterByStatus = (status: string) => {
  query.status = status || undefined
  handleSearch()
}

// ---- 操作 ----
const openEditModal = (record: API.AdminBatchTaskVO) => {
  editForm.id = record.taskId
  editForm.status = record.status
  editForm.tags = record.tags
  editForm.errorMessage = record.errorMessage
  editModalOpen.value = true
}

const handleEdit = async () => {
  if (!editForm.id) return
  editLoading.value = true
  try {
    const res = await adminBatchTaskControllerAdminUpdate({
      id: editForm.id,
      status: editForm.status,
      tags: editForm.tags,
      errorMessage: editForm.errorMessage,
    })
    if (res.data?.code === 0 && res.data?.data) {
      message.success('更新成功')
      editModalOpen.value = false
      fetchList()
      fetchStats()
    } else {
      message.error(res.data?.message ?? '更新失败')
    }
  } finally {
    editLoading.value = false
  }
}

const handleDelete = (taskId: number | undefined) => {
  if (!taskId) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除任务 ${taskId} 吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await adminBatchTaskControllerAdminDelete(taskId)
      if (res.data?.code === 0 && res.data?.data) {
        message.success('删除成功')
        fetchList()
        fetchStats()
      } else {
        message.error(res.data?.message ?? '删除失败')
      }
    },
  })
}

const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) return
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个任务吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      for (const taskId of selectedRowKeys.value) {
        await adminBatchTaskControllerAdminDelete(taskId)
      }
      message.success('批量删除成功')
      selectedRowKeys.value = []
      fetchList()
      fetchStats()
    },
  })
}

const openDetailDrawer = async (record: API.AdminBatchTaskVO) => {
  if (!record.taskId) return
  const res = await adminBatchTaskControllerGetAdminDetail({ id: record.taskId })
  if (res.data?.code === 0 && res.data?.data) {
    detailRecord.value = res.data.data
  } else {
    detailRecord.value = record
  }
  drawerOpen.value = true
}

// ---- 初始化 ----
onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.admin-batch-task-page {
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
  .admin-batch-task-page {
    padding: 12px;
  }
}
</style>
