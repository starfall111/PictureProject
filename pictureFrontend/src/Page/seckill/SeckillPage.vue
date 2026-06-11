<template>
  <div class="seckill-page">
    <SeckillHeroBanner />

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
    </a-tabs>

    <a-spin :spinning="loading">
      <div class="batch-grid">
        <BatchCard
          v-for="batch in filteredBatches"
          :key="batch.id"
          :batch="batch"
          :grabbing-batch-id="grabbingBatchId"
          @grab="handleGrab"
          @countdown-end="fetchBatchList"
        />
        <a-empty v-if="!loading && filteredBatches.length === 0" description="暂无活动" />
      </div>
    </a-spin>

    <GrabResultModal
      :visible="!!grabResult"
      :result="grabResult"
      @close="reset"
      @view-coupon="router.push('/coupon/my')"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import SeckillHeroBanner from '@/components/seckill/SeckillHeroBanner.vue'
import BatchCard from '@/components/seckill/BatchCard.vue'
import GrabResultModal from '@/components/seckill/GrabResultModal.vue'
import { useSeckill } from '@/composables/useSeckill'
import { BATCH_STATUS, type BatchTabKey } from '@/constants/seckill'
import { seckillControllerListBatches } from '@/api/seckillController'

const router = useRouter()
const { grabbing, grabbingBatchId, grabResult, grab, reset } = useSeckill()

const activeTab = ref<BatchTabKey>('ongoing')
const batchList = ref<API.CodeCouponBatch[]>([])
const loading = ref(false)

const tabs = [
  { key: 'ongoing' as const, label: '进行中' },
  { key: 'upcoming' as const, label: '即将开始' },
  { key: 'ended' as const, label: '已结束' },
]

/** tab → 后端 status 参数映射 */
const tabStatusMap: Record<BatchTabKey, number | undefined> = {
  ongoing: BATCH_STATUS.ONGOING,
  upcoming: BATCH_STATUS.PREHEATING,
  ended: BATCH_STATUS.ENDED,
}

const filteredBatches = computed(() => batchList.value)

async function fetchBatchList() {
  loading.value = true
  try {
    const status = tabStatusMap[activeTab.value]
    const res = await seckillControllerListBatches({
      status,
      current: 1,
      pageSize: 100,
    })
    if (res.data.code === 0) {
      batchList.value = res.data.data?.records ?? []
    }
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => fetchBatchList())

let lastGrabBatchId: number | null = null

async function handleGrab(batchId: number) {
  lastGrabBatchId = batchId
  await grab(batchId)
}

function handleRetry() {
  reset()
  if (lastGrabBatchId != null) {
    handleGrab(lastGrabBatchId)
  }
}

onMounted(() => {
  fetchBatchList()
})
</script>

<style scoped>
.seckill-page {
  max-width: 1200px;
  margin: 0 auto;
}

.batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .batch-grid {
    grid-template-columns: 1fr;
  }
}
</style>
