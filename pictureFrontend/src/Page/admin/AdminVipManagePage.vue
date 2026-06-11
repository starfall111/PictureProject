<template>
  <div class="admin-vip-page">
    <div class="page-header">
      <h2>VIP 管理</h2>
    </div>

    <VipStatsCards :stats="vipStats" :loading="statsLoading" />

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="members" tab="会员列表">
        <VipMemberTable />
      </a-tab-pane>
      <a-tab-pane key="stats" tab="秒杀统计">
        <SeckillStatsPanel />
      </a-tab-pane>
      <a-tab-pane key="degrade" tab="降级控制">
        <DegradeControl :current-level="degradeLevel" @change="handleDegradeChange" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminVipControllerGetVipStats } from '@/api/adminVipController'
import VipStatsCards from '@/components/admin/vip/VipStatsCards.vue'
import VipMemberTable from '@/components/admin/vip/VipMemberTable.vue'
import SeckillStatsPanel from '@/components/admin/vip/SeckillStatsPanel.vue'
import DegradeControl from '@/components/admin/vip/DegradeControl.vue'
import { message } from 'ant-design-vue'

const activeTab = ref('members')
const vipStats = ref<API.VipStatsVO>({})
const statsLoading = ref(false)
const degradeLevel = ref(0)

async function fetchVipStats() {
  statsLoading.value = true
  try {
    const res = await adminVipControllerGetVipStats()
    if (res.data.code === 0) {
      vipStats.value = res.data.data ?? {}
    }else{
      message.error("获取会员统计数据失败" + res.data.message);
    }
  } finally {
    statsLoading.value = false
  }
}

function handleDegradeChange(level: number) {
  degradeLevel.value = level
}

onMounted(() => {
  fetchVipStats()
})
</script>

<style scoped>
.admin-vip-page {
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
</style>
