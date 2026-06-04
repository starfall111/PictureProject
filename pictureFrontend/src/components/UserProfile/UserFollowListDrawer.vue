<template>
  <a-drawer
    v-model:open="drawerOpen"
    :title="title"
    placement="right"
    :width="420"
    :closable="true"
    @close="handleClose"
  >
    <div class="user-follow-list">
      <!-- 加载状态 -->
      <div v-if="loading && userList.length === 0" class="loading-container">
        <a-spin />
      </div>

      <!-- 用户列表 -->
      <div v-else-if="userList.length > 0" class="list-container">
        <div
          v-for="user in userList"
          :key="user.id"
          class="user-item"
        >
          <div class="user-info" @click="handleUserClick(user.id!)">
            <a-avatar :src="user.userAvatar" :size="32">
              {{ user.userName?.[0] ?? '?' }}
            </a-avatar>
            <div class="user-details">
              <div class="user-name">{{ user.userName ?? '未知用户' }}</div>
            </div>
          </div>

          <FollowButton
            v-if="currentUserId && user.id !== currentUserId"
            :target-user-id="user.id!"
            :is-following="user.isFollowing ?? false"
            @follow-change="handleFollowChange"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <a-empty v-else :description="emptyDescription" />

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-container">
        <a-pagination
          v-model:current="current"
          v-model:page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          size="small"
          @change="handlePageChange"
        />
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  listFollowingUsingGet,
  listFollowersUsingGet,
  toggleFollowUsingPost,
} from '@/api/followController'
import { userLoginUserStore } from '@/stores/user'
import FollowButton from './FollowButton.vue'

interface Props {
  open: boolean
  userId: string
  type: 'following' | 'followers'
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  userId: '',
  type: 'following',
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'followChange', userId: string, isFollowing: boolean): void
}>()

const router = useRouter()
const loginUserStore = userLoginUserStore()

// 抽屉开关
const drawerOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

// 标题
const title = computed(() => {
  return props.type === 'following' ? '关注列表' : '粉丝列表'
})

// 空状态描述
const emptyDescription = computed(() => {
  return props.type === 'following' ? '暂无关注' : '暂无粉丝'
})

// 当前登录用户 ID
const currentUserId = computed(() => loginUserStore.loginUser.id)

// 列表数据
const userList = ref<API.FollowUserVO[]>([])
const loading = ref(false)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取用户列表
const fetchUserList = async () => {
  if (!props.userId) return

  loading.value = true
  try {
    const apiFunc =
      props.type === 'following'
        ? listFollowingUsingGet
        : listFollowersUsingGet

    const res = await apiFunc({
      userId: props.userId as string,
      current: current.value,
      pageSize: pageSize.value,
    })

    if (res.data.code === 0 && res.data.data) {
      userList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error(res.data.message ?? '获取列表失败')
    }
  } catch {
    message.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 页码变化
const handlePageChange = (page: number) => {
  current.value = page
  fetchUserList()
}

// 点击用户
const handleUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
  drawerOpen.value = false
}

// 关注状态变化
const handleFollowChange = (userId: number, isFollowing: boolean) => {
  // 更新列表中的关注状态
  const user = userList.value.find((u) => u.id === userId)
  if (user) {
    user.isFollowing = isFollowing
  }
  // 通知父组件
  emit('followChange', userId, isFollowing)
}

// 关闭抽屉
const handleClose = () => {
  drawerOpen.value = false
}

// 监听抽屉打开
watch(
  () => props.open,
  (newOpen) => {
    if (newOpen) {
      current.value = 1
      fetchUserList()
    }
  }
)

// 监听类型变化
watch(
  () => props.type,
  () => {
    if (props.open) {
      current.value = 1
      fetchUserList()
    }
  }
)
</script>

<style scoped>
.user-follow-list {
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: var(--surface-secondary, #F7F8FA);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.user-details {
  min-width: 0;
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--fg-primary, #1A1A1A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  :deep(.ant-drawer-body) {
    padding: 16px;
  }
}
</style>
