<template>
  <div id="globalSider">
    <a-layout-sider v-if="loginUserStore.loginUser.id" class="sider" width="200" breakpoint="lg" collapsed-width="0">
      <a-menu mode="inline" v-model:selectedKeys="current" :items="menuItems" @click="doMenuClick" />
    </a-layout-sider>
  </div>

</template>

<script setup lang="ts">
import { h, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  PictureOutlined,
  UserOutlined,
  CommentOutlined,
  WarningOutlined,
  StopOutlined,
} from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/user'
const loginUserStore = userLoginUserStore()

const isAdmin = computed(() => {
  const access = loginUserStore.loginUser?.userRole
  return access === 'admin'
})

// 菜单列表
const menuItems = computed(() => {
  const items: any[] = [
    {
      key: '/',
      label: '公共图库',
      icon: () => h(PictureOutlined),
    },
    {
      key: '/my_space',
      label: '我的空间',
      icon: () => h(UserOutlined),
    },
  ]
  if (isAdmin.value) {
    items.push(
      {
        key: '/admin/feedbackManage',
        label: '反馈管理',
        icon: () => h(CommentOutlined),
      },
      {
        key: '/admin/reportManage',
        label: '举报管理',
        icon: () => h(WarningOutlined),
      },
      {
        key: '/admin/banManage',
        label: '封禁管理',
        icon: () => h(StopOutlined),
      },
    )
  }
  return items
})

const router = useRouter()

// 当前选中菜单
const current = ref<string[]>([])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, failure) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}

</script>

<style></style>