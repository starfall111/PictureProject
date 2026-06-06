<template>
  <div class="admin-report-page">
    <div class="page-header">
      <h2>举报管理</h2>
      <a-button
        v-if="selectedRowKeys.length"
        type="primary"
        @click="handleBatchHandle"
      >
        批量处理 ({{ selectedRowKeys.length }})
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="8" :sm="8">
        <a-card size="small">
          <a-statistic title="待审核" :value="stats.pendingCount ?? 0" :loading="statsLoading">
            <template #prefix>
              <clock-circle-outlined style="color: #fa8c16" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="8">
        <a-card size="small">
          <a-statistic title="今日新增" :value="stats.todayNewCount ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="8">
        <a-card size="small">
          <a-statistic title="累计处理" :value="stats.totalHandledCount ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 行内筛选 -->
    <div class="filter-bar">
      <a-select
        v-model:value="query.status"
        placeholder="状态"
        allow-clear
        style="width: 140px"
        :options="REPORT_STATUS_OPTIONS"
      />
      <a-select
        v-model:value="query.targetType"
        placeholder="目标类型"
        allow-clear
        style="width: 120px"
        :options="REPORT_TARGET_TYPE_OPTIONS"
      />
      <a-button type="primary" @click="fetchList">查询</a-button>
      <a-button @click="resetQuery">重置</a-button>
    </div>

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
      :scroll="{ x: 1000 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'target'">
          <ReportTargetInfo :target="record.targetInfo" />
        </template>
        <template v-if="column.key === 'reason'">
          <a-tag :color="getReasonColor(record.reasonType)">
            {{ REPORT_REASON_MAP[record.reasonType] ?? record.reasonType }}
          </a-tag>
        </template>
        <template v-if="column.key === 'reportCount'">
          <a-badge :dot="(record.reportCount ?? 0) >= 3" :offset="[6, 0]">
            <span :style="{ color: (record.reportCount ?? 0) >= 3 ? '#f5222d' : '' }">
              {{ record.reportCount ?? 1 }}
            </span>
          </a-badge>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="REPORT_STATUS_COLOR[record.status ?? '']">
            {{ REPORT_STATUS_MAP[record.status ?? ''] ?? record.status }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button
            v-if="record.status === 'PENDING'"
            type="link"
            size="small"
            @click="openHandleModal(record)"
          >
            处理
          </a-button>
          <span v-else style="color: #999">已处理</span>
        </template>
      </template>
    </a-table>

    <!-- 处理弹窗 -->
    <ReportHandleModal
      v-model:open="handleModalOpen"
      :report-id="handleReportId"
      @success="onHandleSuccess"
    />

    <!-- 批量处理弹窗 -->
    <a-modal
      v-model:open="batchModalOpen"
      title="批量处理"
      :confirm-loading="batchLoading"
      @ok="handleBatchOk"
    >
      <a-form layout="vertical">
        <a-form-item label="处理结果" required>
          <a-radio-group v-model:value="batchResult">
            <a-radio v-for="opt in REPORT_HANDLE_RESULT_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="处理备注">
          <a-textarea v-model:value="batchReason" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined } from '@ant-design/icons-vue'
import { formatTime } from '@/utils/formatTime'
import ReportTargetInfo from '@/components/report/ReportTargetInfo.vue'
import ReportHandleModal from '@/components/report/ReportHandleModal.vue'
import {
  adminReportControllerGetAdminReportList,
  adminReportControllerBatchHandleReports,
  adminReportControllerGetAdminReportStats,
} from '@/api/adminReportController'
import {
  REPORT_REASON_MAP,
  REPORT_REASON_SEVERITY,
  REPORT_REASON_COLOR,
  REPORT_STATUS_MAP,
  REPORT_STATUS_COLOR,
  REPORT_STATUS_OPTIONS,
  REPORT_TARGET_TYPE_OPTIONS,
  REPORT_HANDLE_RESULT_OPTIONS,
} from '@/constants/report'

const loading = ref(false)
const statsLoading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const selectedRowKeys = ref<number[]>([])

const stats = ref<any>({})
const query = ref<Record<string, string | undefined>>({
  status: undefined,
  targetType: undefined,
})

const columns = [
  { title: '被举报对象', key: 'target', width: 200 },
  { title: '举报原因', key: 'reason', width: 100 },
  { title: '举报次数', key: 'reportCount', dataIndex: 'reportCount', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '处理人', dataIndex: 'handlerId', width: 90 },
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' },
]

const handleModalOpen = ref(false)
const handleReportId = ref<number>()

const batchModalOpen = ref(false)
const batchLoading = ref(false)
const batchResult = ref('')
const batchReason = ref('')

const getReasonColor = (reasonType?: string) =>
  REPORT_REASON_COLOR[REPORT_REASON_SEVERITY[reasonType ?? '']] ?? 'default'

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminReportControllerGetAdminReportList({
      dto: {
        current: current.value,
        pageSize,
        ...query.value,
      },
    })
    if (res.data?.code === 0 && res.data?.data) {
      list.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    }else{
      message.error('获取举报列表失败')
    }
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => { 
  const res = await adminReportControllerGetAdminReportStats()
  if (res.data?.code === 0 && res.data?.data) {
    stats.value = res.data.data
  }else{
    message.error('获取举报统计失败')
  }
}

const onPageChange = (page: number) => {
  current.value = page
  fetchList()
}

const resetQuery = () => {
  query.value = { status: undefined, targetType: undefined }
  current.value = 1
  fetchList()
}

const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

const openHandleModal = (record: any) => {
  handleReportId.value = record.id
  handleModalOpen.value = true
}

const onHandleSuccess = () => {
  fetchList()
}

const handleBatchHandle = () => {
  batchResult.value = ''
  batchReason.value = ''
  batchModalOpen.value = true
}

const handleBatchOk = async () => {
  if (!batchResult.value) return message.warning('请选择处理结果')
  batchLoading.value = true
  try {
    const dtos = selectedRowKeys.value.map((id) => ({
      reportId: id,
      handleResult: batchResult.value,
      handleReason: batchReason.value || undefined,
    }))
    await adminReportControllerBatchHandleReports(dtos)
    message.success('批量处理成功')
    batchModalOpen.value = false
    selectedRowKeys.value = []
    fetchList()
  } catch {
    message.error('批量处理失败')
  } finally {
    batchLoading.value = false
  }
}

onMounted(
  async () => {
    await fetchList()
    await fetchStats()
  }
)
</script>

<style scoped>
.admin-report-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
@media (max-width: 576px) {
  .admin-report-page {
    padding: 12px;
  }
}
</style>
