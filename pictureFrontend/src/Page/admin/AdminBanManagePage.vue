<template>
  <div class="admin-ban-page">
    <div class="page-header">
      <h2>封禁管理</h2>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="全部记录" :value="stats.totalRecords ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="当前封禁中" :value="stats.activeBanned ?? 0" :loading="statsLoading">
            <template #prefix><stop-outlined style="color: #f5222d" /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="临时封禁" :value="stats.tempBanned ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="永久封禁" :value="stats.permanentBanned ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card size="small">
          <a-statistic title="本月解封" :value="stats.monthlyUnbanned ?? 0" :loading="statsLoading" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 筛选 -->
    <div class="filter-bar">
      <a-select v-model:value="banStatus" placeholder="封禁状态" allow-clear style="width: 140px"
        :options="BAN_STATUS_OPTIONS" @change="fetchList" />
    </div>

    <!-- 表格 -->
    <a-table :columns="columns" :data-source="list" :loading="loading" :pagination="{
      current: current,
      total: total,
      pageSize: pageSize,
      showQuickJumper: true,
      onChange: onPageChange,
    }" row-key="id" :scroll="{ x: 1100 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div class="user-cell">
            <a-avatar :size="32">{{ record.userName?.charAt(0) }}</a-avatar>
            <div class="user-info">
              <span class="user-name">{{ record.userName }}</span>
              <span class="user-id">ID: {{ record.userId }}</span>
            </div>
          </div>
        </template>
        <template v-if="column.key === 'banType'">
          <a-tag :color="record.banType === 'PERMANENT' ? 'red' : 'orange'">
            {{ BAN_TYPE_MAP[record.banType] ?? record.banType }}
          </a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="getBanStatusColor(record)">
            {{ getBanStatusLabel(record) }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'banStartTime'">
          {{ formatTime(record.banStartTime) }}
        </template>
        <template v-if="column.dataIndex === 'banEndTime'">
          {{ record.banEndTime ? formatTime(record.banEndTime) : '永久' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openDetailDrawer(record)">详情</a-button>
            <a-button v-if="!record.unbanned && isBanActive(record)" type="link" danger size="small"
              @click="openUnbanModal(record)">
              解封
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 解封弹窗 -->
    <UnbanModal v-model:open="unbanModalOpen" :user-id="unbanUserId" @success="onUnbanSuccess" />

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="drawerOpen" title="封禁详情" width="420">
      <template v-if="currentRecord">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="用户">{{ currentRecord.userName }} (ID: {{ currentRecord.userId
            }})</a-descriptions-item>
          <a-descriptions-item label="封禁类型">
            <a-tag :color="currentRecord.banType === 'PERMANENT' ? 'red' : 'orange'">
              {{ BAN_TYPE_MAP[currentRecord.banType] }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="封禁时长">{{ currentRecord.banDuration }} 天</a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ formatTime(currentRecord.banStartTime) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ currentRecord.banEndTime ? formatTime(currentRecord.banEndTime) : '永久'
            }}</a-descriptions-item>
          <a-descriptions-item label="封禁原因">{{ currentRecord.banReason }}</a-descriptions-item>
          <a-descriptions-item label="违规次数">{{ currentRecord.violationCount }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ currentRecord.banOperatorName }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getBanStatusColor(currentRecord)">
              {{ getBanStatusLabel(currentRecord) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="currentRecord.unbanned" label="解封时间">{{ formatTime(currentRecord.unbanTime)
            }}</a-descriptions-item>
          <a-descriptions-item v-if="currentRecord.unbanned" label="解封原因">{{ currentRecord.unbanReason
            }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { StopOutlined } from '@ant-design/icons-vue'
import { formatTime } from '@/utils/formatTime'
import UnbanModal from '@/components/ban/UnbanModal.vue'
import {
  adminBanControllerGetBanRecordList,
  adminBanControllerGetBanStats,
} from '@/api/adminBanController'
import { BAN_TYPE_MAP, BAN_STATUS_OPTIONS } from '@/constants/ban'
import type { API } from '@/api/typings'

const loading = ref(false)
const statsLoading = ref(false)
const list = ref<API.BanRecordVO[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const banStatus = ref<string | undefined>(undefined)

const stats = ref<any>({})

const columns = [
  { title: '用户', key: 'user', width: 180 },
  { title: '封禁类型', key: 'banType', width: 100 },
  { title: '时长(天)', dataIndex: 'banDuration', width: 90 },
  { title: '开始时间', dataIndex: 'banStartTime', width: 170 },
  { title: '结束时间', dataIndex: 'banEndTime', width: 170 },
  { title: '违规次数', dataIndex: 'violationCount', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作人', dataIndex: 'banOperatorName', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

const unbanModalOpen = ref(false)
const unbanUserId = ref<number>()

const drawerOpen = ref(false)
const currentRecord = ref<API.BanRecordVO | null>(null)

const isBanActive = (record: API.BanRecordVO) => {
  if (record.unbanned) return false
  if (record.banType === 'PERMANENT') return true
  return record.banEndTime ? new Date(record.banEndTime) > new Date() : true
}

const getBanStatusLabel = (record: API.BanRecordVO) => {
  if (record.unbanned) return '已解封'
  if (record.banType === 'PERMANENT') return '封禁中'
  if (record.banEndTime && new Date(record.banEndTime) <= new Date()) return '已到期'
  return '封禁中'
}

const getBanStatusColor = (record: API.BanRecordVO) => {
  if (record.unbanned) return 'green'
  if (!isBanActive(record)) return 'default'
  return 'red'
}

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await adminBanControllerGetBanStats()
    if (res.data?.code === 0 && res.data?.data) stats.value = res.data.data
  } finally {
    statsLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminBanControllerGetBanRecordList({
      current: current.value,
      pageSize,
      banStatus: banStatus.value,
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

const openUnbanModal = (record: API.BanRecordVO) => {
  unbanUserId.value = record.userId
  unbanModalOpen.value = true
}

const onUnbanSuccess = () => {
  fetchList()
  fetchStats()
}

const openDetailDrawer = (record: API.BanRecordVO) => {
  currentRecord.value = record
  drawerOpen.value = true
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.admin-ban-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 500;
  font-size: 13px;
}

.user-id {
  font-size: 11px;
  color: #999;
}

@media (max-width: 576px) {
  .admin-ban-page {
    padding: 12px;
  }
}
</style>
