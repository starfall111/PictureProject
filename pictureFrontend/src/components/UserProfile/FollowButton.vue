<template>
  <a-button
    :type="isFollowing ? 'default' : 'primary'"
    :loading="loading"
    :size="'small'"
    :class="{ 'follow-btn-active': isFollowing }"
    @click="handleClick"
  >
    <template #icon>
      <component :is="isFollowing ? CheckOutlined : PlusOutlined" />
    </template>
    {{ isFollowing ? '已关注' : '关注' }}
  </a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { toggleFollowUsingPost } from '@/api/followController'

interface Props {
  targetUserId: string
  isFollowing: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'followChange', userId: number, isFollowing: boolean): void
}>()

const loading = ref(false)

const handleClick = async () => {
  loading.value = true
  try {
    const res = await toggleFollowUsingPost({
      targetUserId: props.targetUserId,
    })

    if (res.data.code === 0 && res.data.data !== undefined) {
      const newStatus = res.data.data
      emit('followChange', props.targetUserId, newStatus)
      message.success(newStatus ? '关注成功' : '已取消关注')
    } else {
      message.error(res.data.message ?? '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 已关注状态：始终可见的实心按钮 */
.follow-btn-active {
  color: var(--fg-secondary, #4B5563);
  border-color: var(--border-color, #D9D9D9);
  background-color: var(--surface-secondary, #F7F8FA);
}

.follow-btn-active:hover {
  color: #ff4d4f;
  border-color: #ff4d4f;
  background-color: #fff1f0;
}
</style>
