<template>
  <div class="feedback-list-page">
    <div class="page-container">
      <div class="page-header">
        <h2>我的反馈</h2>
        <a-button type="primary" @click="router.push('/feedback/submit')">
          <template #icon><plus-outlined /></template>
          提交反馈
        </a-button>
      </div>

      <!-- 统计卡片 -->
      <FeedbackStatsCards
        :stats="stats"
        :loading="statsLoading"
        :active-status="activeStatus"
        @filter="handleFilterByStatus"
        style="margin-bottom: 20px"
      />

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-select
          v-model:value="filters.status"
          placeholder="状态筛选"
          allow-clear
          style="width: 140px"
          :options="FEEDBACK_STATUS_OPTIONS"
          @change="fetchList"
        />
        <a-select
          v-model:value="filters.type"
          placeholder="类型筛选"
          allow-clear
          style="width: 140px"
          :options="FEEDBACK_TYPE_OPTIONS"
          @change="fetchList"
        />
      </div>

      <!-- 列表 -->
      <a-spin :spinning="loading">
        <a-list
          :grid="{ gutter: 16, xs: 1, sm: 1, md: 2, lg: 2, xl: 2 }"
          :data-source="list"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-card hoverable @click="router.push(`/feedback/${item.id}`)">
                <div class="card-title-row">
                  <span class="card-title">{{ item.title }}</span>
                </div>
                <div class="card-tags">
                  <a-tag :color="FEEDBACK_STATUS_COLOR[item.status ?? '']">
                    {{ FEEDBACK_STATUS_MAP[item.status ?? ''] ?? item.status }}
                  </a-tag>
                  <a-tag>{{ FEEDBACK_TYPE_MAP[item.type ?? ''] ?? item.type }}</a-tag>
                  <a-tag
                    v-if="item.priority"
                    :color="FEEDBACK_PRIORITY_COLOR[item.priority] ?? 'default'"
                  >
                    {{ item.priority }}
                  </a-tag>
                </div>
                <div class="card-meta">
                  <span class="card-time">{{ formatTime(item.createTime) }}</span>
                </div>
              </a-card>
            </a-list-item>
          </template>
        </a-list>

        <a-empty
          v-if="!loading && !list.length"
          description="还没有提交过反馈"
        >
          <a-button type="primary" @click="router.push('/feedback/submit')">
            去提交
          </a-button>
        </a-empty>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
          <a-pagination
            v-model:current="current"
            :total="total"
            :page-size="pageSize"
            show-quick-jumper
            @change="fetchList"
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import FeedbackStatsCards from '@/components/feedback/FeedbackStatsCards.vue'
import { formatTime } from '@/utils/formatTime'
import { feedbackControllerGetMyFeedbackList, feedbackControllerGetMyFeedbackStats } from '@/api/feedbackController'
import {
  FEEDBACK_STATUS_MAP,
  FEEDBACK_STATUS_COLOR,
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_TYPE_OPTIONS,
  FEEDBACK_PRIORITY_COLOR,
} from '@/constants/feedback'

const router = useRouter()

const loading = ref(false)
const statsLoading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const activeStatus = ref<string>('')

const stats = ref<any>({})
const filters = ref<Record<string, string | undefined>>({
  status: undefined,
  type: undefined,
})

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await feedbackControllerGetMyFeedbackStats()
    if (res.data?.code === 0 && res.data?.data) {
      stats.value = res.data.data
    }
  } finally {
    statsLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await feedbackControllerGetMyFeedbackList({
      dto: {
        current: current.value,
        pageSize,
        status: filters.value.status,
        type: filters.value.type,
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

const handleFilterByStatus = (status: string) => {
  activeStatus.value = activeStatus.value === status ? '' : status
  filters.value.status = activeStatus.value || undefined
  current.value = 1
  fetchList()
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.feedback-list-page {
  padding: 24px;
}
.page-container {
  max-width: 960px;
  margin: 0 auto;
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
.card-title-row {
  margin-bottom: 8px;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-tags {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
@media (max-width: 576px) {
  .feedback-list-page {
    padding: 12px;
  }
}
</style>
