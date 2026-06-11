<template>
  <div>
    <a-table
      :dataSource="memberList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'vipType'">
          <a-tag :color="record.vipType === 1 ? 'gold' : 'default'">
            {{ record.vipType === 1 ? 'VIP' : '普通' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'vipExpireTime'">
          {{ formatTime(record.vipExpireTime) }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="isExpired(record.vipExpireTime) ? 'error' : 'success'">
            {{ isExpired(record.vipExpireTime) ? '已过期' : '活跃' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" type="link" @click="handleGrant(record.id)">赠送VIP</a-button>
            <a-button size="small" type="link" danger @click="handleRevoke(record.id)">撤销VIP</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      :open="grantModalVisible"
      title="赠送VIP"
      :confirm-loading="granting"
      @ok="doGrant"
      @cancel="grantModalVisible = false"
    >
      <a-form :label-col="{ span: 5 }" style="margin-top: 16px">
        <a-form-item label="赠送天数">
          <a-input-number v-model:value="grantDays" :min="1" :max="365" style="width: 100%" />
        </a-form-item>
        <a-form-item label="原因">
          <a-input v-model:value="grantReason" placeholder="请输入赠送原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  adminVipControllerListVipUsers,
  adminVipControllerGrantVip,
  adminVipControllerRevokeVip,
} from '@/api/adminVipController'

const memberList = ref<API.User[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const grantModalVisible = ref(false)
const granting = ref(false)
const grantUserId = ref<number | null>(null)
const grantDays = ref(7)
const grantReason = ref('')

const columns = [
  { title: '用户ID', dataIndex: 'id', width: 80 },
  { title: '用户名', dataIndex: 'userName', width: 120 },
  { title: 'VIP类型', key: 'vipType', width: 90 },
  { title: '到期时间', key: 'vipExpireTime', width: 160 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

async function fetchMembers() {
  loading.value = true
  try {
    const res = await adminVipControllerListVipUsers({
      current: pagination.value.current,
      pageSize: pagination.value.pageSize,
    })
    if (res.data.code === 0) {
      memberList.value = res.data.data?.records ?? []
      pagination.value.total = res.data.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  fetchMembers()
}

function handleGrant(userId: number) {
  grantUserId.value = userId
  grantDays.value = 7
  grantReason.value = ''
  grantModalVisible.value = true
}

async function doGrant() {
  if (!grantUserId.value) return
  granting.value = true
  try {
    const res = await adminVipControllerGrantVip(
      { userId: grantUserId.value },
      { days: grantDays.value, reason: grantReason.value },
    )
    if (res.data.code === 0) {
      message.success('赠送成功')
      grantModalVisible.value = false
      fetchMembers()
    } else {
      message.error(res.data.message || '赠送失败')
    }
  } catch (e: any) {
    message.error(e?.message || '赠送失败')
  } finally {
    granting.value = false
  }
}

function handleRevoke(userId: number) {
  Modal.confirm({
    title: '确认撤销VIP',
    content: '撤销后用户将失去VIP权益，确认撤销？',
    async onOk() {
      try {
        const res = await adminVipControllerRevokeVip({ userId })
        if (res.data.code === 0) {
          message.success('撤销成功')
          fetchMembers()
        } else {
          message.error(res.data.message || '撤销失败')
        }
      } catch (e: any) {
        message.error(e?.message || '撤销失败')
      }
    },
  })
}

function isExpired(time?: string) {
  if (!time) return true
  return new Date(time).getTime() < Date.now()
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchMembers()
})
</script>
