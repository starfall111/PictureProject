<template>
  <div class="admin-coupon-page">
    <div class="page-header">
      <h2>券管理</h2>
    </div>

    <div class="filter-bar">
      <a-input-number
        v-model:value="filters.batchId"
        placeholder="批次ID"
        style="width: 140px"
      />
      <a-select
        v-model:value="filters.status"
        placeholder="状态"
        allow-clear
        style="width: 140px"
      >
        <a-select-option v-for="(v, k) in COUPON_STATUS_MAP" :key="k" :value="Number(k)">
          {{ v.text }}
        </a-select-option>
      </a-select>
      <a-input-number
        v-model:value="filters.userId"
        placeholder="用户ID"
        style="width: 140px"
      />
      <a-button @click="fetchCoupons">查询</a-button>
      <a-button @click="handleReset">重置</a-button>
    </div>

    <a-table
      :dataSource="couponList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button
            type="link"
            size="small"
            danger
            :disabled="record.status !== COUPON_STATUS.CLAIMED"
            @click="handleRevoke(record.id)"
          >
            吊销
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { message, Modal, Tag } from 'ant-design-vue'
import { adminCouponControllerListCoupons, adminCouponControllerRevoke } from '@/api/adminCouponController'
import { COUPON_STATUS_MAP, COUPON_STATUS } from '@/constants/coupon'
import { COUPON_TYPE_MAP } from '@/constants/seckill'

const couponList = ref<API.CodeCoupon[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })
const filters = ref<{
  batchId: number | undefined
  status: number | undefined
  userId: number | undefined
}>({
  batchId: undefined,
  status: undefined,
  userId: undefined,
})

const columns = [
  { title: '券ID', dataIndex: 'id', width: 80 },
  { title: '批次ID', dataIndex: 'batchId', width: 80 },
  { title: '用户ID', dataIndex: 'userId', width: 80, customRender: ({ text }: { text: number }) => text ?? '-' },
  {
    title: '券编码',
    dataIndex: 'code',
    width: 140,
    ellipsis: true,
    customRender: ({ text }: { text: string }) =>
      h('span', { style: { fontFamily: 'Courier New, monospace' } }, text ?? '-'),
  },
  {
    title: '类型',
    dataIndex: 'type',
    width: 80,
    customRender: ({ text }: { text: number }) => `${text}天`,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }: { text: number }) => {
      const m = COUPON_STATUS_MAP[text]
      return h(Tag, { color: m?.color ?? 'default' }, () => m?.text ?? '未知')
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right' as const,
  },
]

async function fetchCoupons() {
  loading.value = true
  try {
    const res = await adminCouponControllerListCoupons({
      current: pagination.value.current,
      pageSize: pagination.value.pageSize,
      batchId: filters.value.batchId,
      status: filters.value.status,
      userId: filters.value.userId,
    })
    if (res.data.code === 0) {
      couponList.value = res.data.data?.records ?? []
      pagination.value.total = res.data.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  fetchCoupons()
}

function handleReset() {
  filters.value = { batchId: undefined, status: undefined, userId: undefined }
  pagination.value.current = 1
  fetchCoupons()
}

function handleRevoke(id: number) {
  Modal.confirm({
    title: '确认吊销',
    content: '吊销后该券将失效，确认吊销？',
    async onOk() {
      try {
        const res = await adminCouponControllerRevoke({ id })
        if (res.data.code === 0) {
          message.success('吊销成功')
          fetchCoupons()
        } else {
          message.error(res.data.message || '吊销失败')
        }
      } catch (e: any) {
        message.error(e?.message || '吊销失败')
      }
    },
  })
}

onMounted(() => {
  fetchCoupons()
})
</script>

<style scoped>
.admin-coupon-page {
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
