<template>
  <div id="immersiveLayout">
    <header class="immersive-header">
      <router-link to="/" class="logo-area">
        <div class="logo-icon"></div>
        <span class="logo-text">智能云图库</span>
      </router-link>
      <div class="header-spacer"></div>
      <div class="search-area">
        <input class="search-box" type="text" placeholder="搜索图片..." @keyup.enter="handleSearch" />
      </div>
      <a-dropdown>
        <button class="upload-btn">+ 上传</button>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="router.push('/add_picture')">上传图片</a-menu-item>
            <a-menu-item @click="router.push('/add_picture/batch')">批量获取</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <NotificationBell style="margin-right: 24px" />
      <div class="user-area">
        <template v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space :size="8">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" />
              <VipBadge size="small" />
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/user/center')">个人中心</a-menu-item>
                <a-menu-item @click="router.push('/my_space')">我的空间</a-menu-item>
                <a-menu-item @click="router.push('/coupon/my')">
                  <IeOutlined />
                  我的券包
                  <VipBadge size="default" style="margin-left: 8px" />
                </a-menu-item>
                <a-menu-item @click="router.push('/feedback/list')">
                  <CommentOutlined />
                  我的反馈
                </a-menu-item>
                <a-menu-item @click="logout()">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <a-button v-else type="primary" href="/user/login">登录</a-button>
      </div>
    </header>
    <FloatingSidebar />
    <main class="immersive-content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CommentOutlined, IeOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/user'
import { useSeckillStore } from '@/stores/seckill'
import { userControllerLogOut } from '@/api/userController'
import FloatingSidebar from '@/layouts/scheme-1-immersive/components/FloatingSidebar.vue'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import VipBadge from '@/components/vip/VipBadge.vue'

const router = useRouter()
const loginUserStore = userLoginUserStore()

const handleSearch = (e: KeyboardEvent) => {
  const value = (e.target as HTMLInputElement).value.trim()
  if (value) {
    router.push({ path: '/home', query: { searchText: value } })
  }
}

const logout = async () => {
  const res = await userControllerLogOut()
  if (res.data.code === 0 && res.data.data) {
    loginUserStore.setLoginUser({})
    useSeckillStore().clearToken()
    message.success('退出登录成功')
    router.push('/user/login')
  } else {
    message.error('退出登录失败')
  }
}
</script>

<style scoped>
#immersiveLayout {
  min-height: 100vh;
  background: var(--bg-content, #F3F4F6);
}

.immersive-header {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 56px;
  padding: 0 24px;
  margin-bottom: 16px;
  background: var(--header-bg, #FFFFFF);
  border-bottom: 1px solid var(--border-color, #E5E7EB);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.logo-icon {
  width: 28px;
  height: 28px;
  background: var(--accent, #635BFF);
  border-radius: 8px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--fg-primary, #1A1A1A);
}

.header-spacer {
  flex: 1;
}

.search-area {
  display: flex;
  justify-content: center;
}

.search-box {
  width: var(--search-box-width, 360px);
  height: var(--search-box-height, 36px);
  border-radius: var(--radius-pill, 9999px);
  border: none;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: var(--surface-secondary, #F7F8FA);
  color: var(--fg-primary, #1A1A1A);
}

.search-box::placeholder {
  color: var(--fg-muted, #9CA3AF);
}

.search-box:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(99, 91, 255, 0.2);
}

.upload-btn {
  height: 32px;
  border: none;
  border-radius: var(--radius-pill, 9999px);
  padding: 0 16px;
  background: var(--accent, #635BFF);
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.upload-btn:hover {
  opacity: 0.9;
}

.user-area {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.immersive-content {
  padding: var(--content-padding, 24px);
  padding-left: calc(var(--content-padding, 24px) + var(--sidebar-collapsed-width, 56px) + var(--sidebar-left-offset, 12px));
}

@media (max-width: 767px) {
  .immersive-content {
    padding-left: var(--content-padding, 24px);
  }
}
</style>
