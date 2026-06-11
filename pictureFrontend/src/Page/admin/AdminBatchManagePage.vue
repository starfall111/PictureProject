<template>
  <div class="admin-batch-page">
    <div class="page-header">
      <h2>批次管理</h2>
      <a-button type="primary" @click="createModalVisible = true">+ 创建批次</a-button>
    </div>

    <BatchStatsCards
      :stats="stats"
      :loading="loading"
      :active-filter="filters.status ?? null"
      @filter="handleStatsFilter"
    />

    <div class="filter-bar">
      <a-select
        v-model:value="filters.status"
        placeholder="状态筛选"
        allow-clear
        style="width: 140px"
      >
        <a-select-option v-for="(v, k) in BATCH_STATUS_MAP" :key="k" :value="Number(k)">
          {{ v.text }}
        </a-select-option>
      </a-select>
      <a-button @click="fetchBatchList">查询</a-button>
      <a-button @click="handleReset">重置</a-button>
    </div>

    <a-table
      :dataSource="batchList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="showDetail(record.id)">详情</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.DRAFT"
            type="link"
            size="small"
            @click="showEditModal(record)"
          >编辑</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.DRAFT"
            type="link"
            size="small"
            @click="handleGenerateCodes(record.id)"
          >生成兑换码</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.DRAFT"
            type="link"
            size="small"
            @click="handlePreheat(record.id)"
          >预热</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.DRAFT"
            type="link"
            size="small"
            danger
            @click="handleCancel(record.id)"
          >取消</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.PREHEATING"
            type="link"
            size="small"
            @click="handlePublish(record.id)"
          >发布</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.PREHEATING"
            type="link"
            size="small"
            danger
            @click="handleCancel(record.id)"
          >取消</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.ONGOING"
            type="link"
            size="small"
            @click="handleEnd(record.id)"
          >下架</a-button>
          <a-button
            v-if="record.status === BATCH_STATUS.CANCELLED"
            type="link"
            size="small"
            @click="handleRestore(record.id)"
          >恢复</a-button>
        </template>
      </template>
    </a-table>

    <BatchCreateModal
      :visible="createModalVisible"
      :loading="creating"
      @ok="handleCreate"
      @cancel="createModalVisible = false"
    />

    <BatchDetailDrawer
      :visible="detailVisible"
      :batch-id="selectedBatchId"
      @close="detailVisible = false"
    />

    <a-modal
      v-model:visible="editModalVisible"
      title="编辑批次"
      :confirm-loading="editLoading"
      @ok="handleEditSubmit"
      @cancel="editModalVisible = false"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="活动名称">
          <a-input v-model:value="editForm.name" />
        </a-form-item>
        <a-form-item label="券类型">
          <a-select v-model:value="editForm.type">
            <a-select-option v-for="(v, k) in COUPON_TYPE_MAP" :key="k" :value="Number(k)">
              {{ v.text }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始时间">
          <a-date-picker
            v-model:value="editForm.startTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="结束时间">
          <a-date-picker
            v-model:value="editForm.endTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  adminBatchControllerListBatches,
  adminBatchControllerCreateBatch,
  adminBatchControllerTransition,
  adminBatchControllerEnd,
  adminBatchControllerCancel,
  adminBatchControllerRestore,
  adminBatchControllerGenerateCodes,
  adminBatchControllerUpdateBatch,
} from '@/api/adminBatchController'
import { BATCH_STATUS, BATCH_STATUS_MAP, COUPON_TYPE_MAP } from '@/constants/seckill'
import BatchStatsCards from '@/components/admin/batch/BatchStatsCards.vue'
import BatchCreateModal from '@/components/admin/batch/BatchCreateModal.vue'
import BatchDetailDrawer from '@/components/admin/batch/BatchDetailDrawer.vue'

const batchList = ref<API.CodeCouponBatch[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })
const filters = ref<{ status: number | undefined }>({ status: undefined })

const createModalVisible = ref(false)
const creating = ref(false)
const detailVisible = ref(false)
const selectedBatchId = ref<number | null>(null)

// 编辑相关
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref<{ id: number | null; name: string; type: number | undefined; startTime: any; endTime: any }>({
  id: null,
  name: '',
  type: undefined,
  startTime: undefined,
  endTime: undefined,
})

const stats = computed(() => ({
  total: pagination.value.total,
  ongoing: batchList.value.filter(b => b.status === BATCH_STATUS.ONGOING).length,
  preheating: batchList.value.filter(b => b.status === BATCH_STATUS.PREHEATING).length,
  ended: batchList.value.filter(b => b.status === BATCH_STATUS.ENDED).length,
  cancelled: batchList.value.filter(b => b.status === BATCH_STATUS.CANCELLED).length,
}))

const columns = [
  { title: '批次号', dataIndex: 'batchNo', width: 100 },
  { title: '名称', dataIndex: 'name', width: 140 },
  {
    title: '券类型',
    dataIndex: 'type',
    width: 120,
    customRender: ({ text }: { text: number }) => COUPON_TYPE_MAP[text]?.text ?? `${text}天`,
  },
  { title: '总库存', dataIndex: 'totalStock', width: 80 },
  { title: '剩余', dataIndex: 'currentStock', width: 80 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }: { text: number }) => ({
      children: BATCH_STATUS_MAP[text]?.text ?? '未知',
      props: { color: BATCH_STATUS_MAP[text]?.color },
    }),
  },
  {
    title: '操作',
    key: 'action',
    width: 320,
    fixed: 'right' as const,
  },
]

async function fetchBatchList() {
  loading.value = true
  try {
    const res = await adminBatchControllerListBatches({
      current: pagination.value.current,
      pageSize: pagination.value.pageSize,
      status: filters.value.status,
    })
    if (res.data.code === 0 && res.data?.data) {
      batchList.value = res.data?.data.records ?? []
      pagination.value.total = res.data?.data.total ?? 0
    } else {
      message.error(res.data.message)
    }
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  fetchBatchList()
}

function handleStatsFilter(status: number | null) {
  filters.value.status = status ?? undefined
  pagination.value.current = 1
  fetchBatchList()
}

function handleReset() {
  filters.value.status = undefined
  pagination.value.current = 1
  fetchBatchList()
}

async function handleCreate(data: API.BatchCreateDTO) {
  creating.value = true
  try {
    const res = await adminBatchControllerCreateBatch(data)
    if (res.data.code === 0) {
      message.success('创建成功')
      createModalVisible.value = false
      fetchBatchList()
    } else {
      message.error(res.data.message || '创建失败')
    }
  } catch (e: any) {
    message.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 预热：草稿 → 预热中
async function handlePreheat(id: number) {
  try {
    const res = await adminBatchControllerTransition({ id, targetStatus: BATCH_STATUS.PREHEATING })
    if (res.data.code === 0 && res.data.data) {
      message.success('预热成功')
      fetchBatchList()
    } else {
      message.error(res.data.message || '预热失败')
    }
  } catch (e: any) {
    message.error(e?.message || '预热失败')
  }
}

// 发布：预热中 → 进行中
function handlePublish(id: number) {
  Modal.confirm({
    title: '确认发布',
    content: '发布后活动将进入进行中状态，确认发布？',
    async onOk() {
      const res = await adminBatchControllerTransition({ id, targetStatus: BATCH_STATUS.ONGOING })
      if (res.data.code === 0 && res.data.data) {
        message.success('发布成功')
        fetchBatchList()
      } else {
        message.error(res.data.message || '发布失败')
      }
    },
  })
}

// 下架：进行中 → 已结束
function handleEnd(id: number) {
  Modal.confirm({
    title: '确认下架',
    content: '下架后活动将停止发放兑换码，确认下架？',
    async onOk() {
      const res = await adminBatchControllerEnd({ id })
      if (res.data.code === 0 && res.data.data) {
        message.success('下架成功')
        fetchBatchList()
      } else {
        message.error(res.data.message || '下架失败')
      }
    },
  })
}

// 取消：预热中/草稿 → 已取消
function handleCancel(id: number) {
  Modal.confirm({
    title: '确认取消',
    content: '取消后批次将停止发放，确认取消？',
    async onOk() {
      const res = await adminBatchControllerCancel({ id })
      if (res.data.code === 0 && res.data.data) {
        message.success('取消成功')
        fetchBatchList()
      } else {
        message.error(res.data.message || '取消失败')
      }
    },
  })
}

// 恢复：已取消 → 草稿
function handleRestore(id: number) {
  Modal.confirm({
    title: '确认恢复',
    content: '恢复后活动将回到草稿状态，确认恢复？',
    async onOk() {
      const res = await adminBatchControllerRestore({ id })
      if (res.data.code === 0 && res.data.data) {
        message.success('恢复成功')
        fetchBatchList()
      } else {
        message.error(res.data.message || '恢复失败')
      }
    },
  })
}

// 生成兑换码：仅草稿
function handleGenerateCodes(id: number) {
  Modal.confirm({
    title: '确认生成兑换码',
    content: '生成后兑换码将按总库存数量创建，确认生成？',
    async onOk() {
      const res = await adminBatchControllerGenerateCodes({ id })
      if (res.data.code === 0 && res.data.data) {
        message.success('兑换码生成成功')
        fetchBatchList()
      } else {
        message.error(res.data.message || '生成失败')
      }
    },
  })
}

// 编辑：仅草稿，只改 name/type/startTime/endTime
function showEditModal(record: API.CodeCouponBatch) {
  editForm.value = {
    id: record.id!,
    name: record.name ?? '',
    type: record.type,
    startTime: record.startTime ? dayjs(record.startTime) : undefined,
    endTime: record.endTime ? dayjs(record.endTime) : undefined,
  }
  editModalVisible.value = true
}

async function handleEditSubmit() {
  if (!editForm.value.name) {
    message.warning('请填写活动名称')
    return
  }
  editLoading.value = true
  try {
    const res = await adminBatchControllerUpdateBatch(
      { id: editForm.value.id! },
      {
        name: editForm.value.name,
        type: editForm.value.type,
        startTime: editForm.value.startTime?.toDate?.(),
        endTime: editForm.value.endTime?.toDate?.(),
      }
    )
    if (res.data.code === 0 && res.data.data) {
      message.success('修改成功')
      editModalVisible.value = false
      fetchBatchList()
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (e: any) {
    message.error(e?.message || '修改失败')
  } finally {
    editLoading.value = false
  }
}

function showDetail(id: number) {
  selectedBatchId.value = id
  detailVisible.value = true
}

onMounted(() => {
  fetchBatchList()
})
</script>

<style scoped>
.admin-batch-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
