<template>
  <a-drawer
    :open="visible"
    title="批次详情"
    :width="560"
    @close="emit('close')"
  >
    <a-spin :spinning="batchLoading">
      <template v-if="batch">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="批次号">{{ batch.batchNo }}</a-descriptions-item>
          <a-descriptions-item label="名称">{{ batch.name }}</a-descriptions-item>
          <a-descriptions-item label="券类型">
            <a-tag :color="couponTypeColor">{{ couponTypeName }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="总库存">{{ batch.totalStock }}</a-descriptions-item>
          <a-descriptions-item label="剩余库存">{{ batch.currentStock }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="BATCH_STATUS_MAP[batch.status ?? 0]?.color">
              {{ BATCH_STATUS_MAP[batch.status ?? 0]?.text }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ formatTime(batch.startTime) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ batch.endTime ? formatTime(batch.endTime) : '抢完即止' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(batch.createTime) }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>该批次下的券</a-divider>

        <a-spin :spinning="couponLoading">
          <a-table
            :dataSource="couponList"
            :columns="couponColumns"
            :pagination="couponPagination"
            :scroll="{ y: 300 }"
            size="small"
            @change="handleCouponTableChange"
          />
        </a-spin>
      </template>
      <a-empty v-else description="未找到批次信息" />
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { adminBatchControllerListBatches } from '@/api/adminBatchController'
import { adminCouponControllerListCoupons } from '@/api/adminCouponController'
import { BATCH_STATUS_MAP, COUPON_TYPE_MAP } from '@/constants/seckill'
import { COUPON_STATUS_MAP } from '@/constants/coupon'

const props = defineProps<{
  visible: boolean
  batchId: number | null
}>()

const emit = defineEmits<{
  close: []
}>()

const batch = ref<API.CodeCouponBatch | null>(null)
const batchLoading = ref(false)

const couponList = ref<API.CodeCoupon[]>([])
const couponLoading = ref(false)
const couponPagination = ref({ current: 1, pageSize: 10, total: 0 })

const couponTypeName = computed(() => COUPON_TYPE_MAP[batch.value?.type ?? 0]?.text ?? 'VIP券')
const couponTypeColor = computed(() => COUPON_TYPE_MAP[batch.value?.type ?? 0]?.color ?? '#1677ff')

const couponColumns = [
  { title: '券ID', dataIndex: 'id', width: 80 },
  { title: '券编码', dataIndex: 'code', width: 120, ellipsis: true },
  { title: '用户ID', dataIndex: 'userId', width: 80 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }: { text: number }) => {
      const m = COUPON_STATUS_MAP[text]
      return { children: m?.text ?? '未知', props: {} }
    },
  },
]

async function fetchBatchDetail() {
  if (!props.batchId) return
  batchLoading.value = true
  try {
    const res = await adminBatchControllerListBatches({ current: 1, pageSize: 1 })
    // 遍历找到对应批次（API没有单条查询，用列表）
    if (res.data.code === 0 && res.data.data?.records) {
      batch.value = res.data.data.records.find(b => b.id === props.batchId) ?? null
    }
  } finally {
    batchLoading.value = false
  }
}

async function fetchCoupons() {
  if (!props.batchId) return
  couponLoading.value = true
  try {
    const res = await adminCouponControllerListCoupons({
      batchId: props.batchId,
      current: couponPagination.value.current,
      pageSize: couponPagination.value.pageSize,
    })
    if (res.data.code === 0) {
      couponList.value = res.data.data?.records ?? []
      couponPagination.value.total = res.data.data?.total ?? 0
    }
  } finally {
    couponLoading.value = false
  }
}

function handleCouponTableChange(pag: any) {
  couponPagination.value.current = pag.current
  fetchCoupons()
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

watch(() => props.visible, (val) => {
  if (val && props.batchId) {
    fetchBatchDetail()
    couponPagination.value.current = 1
    fetchCoupons()
  }
})
</script>
