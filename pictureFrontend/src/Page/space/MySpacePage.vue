<template>
  <div id="mySpace">
    <p>正在跳转，请稍候...</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { spaceControllerGetSpaceByUserId } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import { userLoginUserStore } from '@/stores/user'

const router = useRouter()
const loginUserStore = userLoginUserStore()

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  try {
    const loginUser = loginUserStore.loginUser
    if (!loginUser?.id) {
      router.replace('/user/login')
      return
    }
    // 获取用户空间信息
    const res = await spaceControllerGetSpaceByUserId({
      id: loginUser.id,
    })
    if (res.data.code === 0) {
      if (res.data.data?.length > 0) {
        const space = res.data.data[0]
        router.replace(`/space/${space.id}`)
      } else {
        router.replace('/add_space')
        message.warn('请先创建空间')
      }
    } else {
      message.error('加载我的空间失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('加载我的空间失败：' + e.message)
  }
}

// 在页面加载时检查用户空间
onMounted(() => {
  checkUserSpace()
})
</script>
