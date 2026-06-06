<template>
  <div class="my-report-page">
    <div class="page-container">
      <div class="page-header">
        <h2>我的举报</h2>
      </div>

      <a-spin :spinning="loading">
        <a-table
          v-if="list.length"
          :columns="columns"
          :data-source="list"
          :pagination="{
            current: current,
            total: total,
            pageSize: pageSize,
            showQuickJumper: true,
            onChange: onPageChange,
          }"
          row-key="id"
          :scroll="{ x: 700 }"
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
            <template v-if="column.key === 'status'">
              <a-tag :color="REPORT_STATUS_COLOR[record.status ?? '']">
                {{ REPORT_STATUS_MAP[record.status ?? ''] ?? record.status }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-popconfirm
                v-if="canCancel(record)"
                title="确定要取消此举报吗？"
                @confirm="handleCancel(record.id)"
                ok-text="确定"
                cancel-text="取消"
              >
                <a-button type="link" danger size="small">取消</a-button>
              </a-popconfirm>
              <a-button
                type="link"
                size="small"
                @click="viewDetail(record)"
              >
                详情
              </a-button>
            </template>
          </template>
        </a-table>

        <a-empty v-if="!loading && !list.length" description="没有举报记录">
          <template #image>
            <safety-certificate-outlined style="font-size: 64px; color: #bfbfbf" />
          </template>
        </a-empty>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SafetyCertificateOutlined } from '@ant-design/icons-vue'
import ReportTargetInfo from '@/components/report/ReportTargetInfo.vue'
import { reportControllerGetMyReportList, reportControllerCancelReport } from '@/api/reportController'
import { REPORT_REASON_MAP, REPORT_STATUS_MAP, REPORT_STATUS_COLOR, REPORT_REASON_SEVERITY, REPORT_REASON_COLOR } from '@/constants/report'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10

const columns = [
  { title: '被举报对象', key: 'target', dataIndex: 'target' },
  { title: '举报原因', key: 'reason', dataIndex: 'reason' },
  { title: '状态', key: 'status', dataIndex: 'status' },
  { title: '提交时间', key: 'createTime', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const getReasonColor = (reasonType?: string) =>
  REPORT_REASON_COLOR[REPORT_REASON_SEVERITY[reasonType ?? '']] ?? 'default'

const canCancel = (record: any) => {
  if (record.status !== 'PENDING') return false
  const created = new Date(record.createTime).getTime()
  return Date.now() - created < 10 * 60 * 1000
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await reportControllerGetMyReportList({
      dto: { current: current.value, pageSize },
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

const handleCancel = async (reportId: number) => {
  try {
    await reportControllerCancelReport({ reportId })
    message.success('已取消举报')
    fetchList()
  } catch {
    message.error('取消失败')
  }
}

const viewDetail = (record: any) => {
  // 简单展示详情信息（可后续扩展为弹窗）
  message.info(`举报 #${record.id} - ${REPORT_STATUS_MAP[record.status ?? ''] ?? record.status}`)
}

onMounted(fetchList)
</script>

<style scoped>
.my-report-page {
  padding: 24px;
}
.page-container {
  max-width: 960px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
}
@media (max-width: 576px) {
  .my-report-page {
    padding: 12px;
  }
}
</style>
