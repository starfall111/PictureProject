<template>
  <div id="immersiveLayout">
    <header class="immersive-header">
      <button class="mobile-menu-btn" type="button" aria-label="打开导航菜单" @click="mobileNavOpen = true">
        <MenuOutlined />
      </button>
      <router-link to="/" class="logo-area">
        <div class="logo-icon"></div>
        <span class="logo-text">图境</span>
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
            <a-menu-item @click="router.push('/add_picture/batch')">
              批量获取
              <VipBadge feature size="default" style="margin-left: 8px" />
            </a-menu-item>
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
                <a-menu-item @click="router.push('/user/center')">
                  <UserOutlined />
                  个人中心
                </a-menu-item>
                <a-menu-item @click="router.push('/my_space')">
                  <AppstoreOutlined />
                  我的空间
                </a-menu-item>
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

    <!-- 移动端导航抽屉：承接被隐藏的悬浮侧边栏（hover 范式在触屏不可用） -->
    <a-drawer
      :open="mobileNavOpen"
      placement="left"
      title="导航"
      :width="260"
      :body-style="{ padding: '8px 0' }"
      @close="mobileNavOpen = false"
    >
      <nav class="mobile-nav">
        <SidebarNavItem
          v-for="item in userNavItems"
          :key="item.path"
          :icon="item.icon"
          :label="item.label"
          :active="isCurrentRoute(item.path)"
          :show-label="true"
          :feature="item.feature"
          @click="onMobileNavigate(item.path)"
        />
        <template v-if="isAdmin">
          <div class="mobile-nav-divider"></div>
          <div class="mobile-nav-group-title">
            <SafetyCertificateOutlined />
            <span>管理面板</span>
            <span class="mobile-admin-tag">Admin</span>
          </div>
          <SidebarNavItem
            v-for="item in adminNavItems"
            :key="item.path"
            :icon="item.icon"
            :label="item.label"
            :active="isCurrentRoute(item.path)"
            :show-label="true"
            :indented="true"
            @click="onMobileNavigate(item.path)"
          />
        </template>
      </nav>
    </a-drawer>

    <main class="immersive-content">
      <slot />
    </main>
    <footer class="site-footer">
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
        赣ICP备2026011163号
      </a>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, CommentOutlined, IeOutlined, LogoutOutlined, MenuOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/user'
import { useSeckillStore } from '@/stores/seckill'
import { userControllerLogOut } from '@/api/userController'
import FloatingSidebar from '@/layouts/scheme-1-immersive/components/FloatingSidebar.vue'
import SidebarNavItem from '@/layouts/scheme-1-immersive/components/SidebarNavItem.vue'
import { useSidebarNav } from '@/layouts/scheme-1-immersive/composables/useSidebarNav'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import VipBadge from '@/components/vip/VipBadge.vue'

const router = useRouter()
const loginUserStore = userLoginUserStore()

// 移动端导航抽屉（菜单数据与桌面 FloatingSidebar 共用同一 composable）
const mobileNavOpen = ref(false)
const { userNavItems, adminNavItems, isAdmin, isCurrentRoute } = useSidebarNav()
const onMobileNavigate = (path: string) => {
  router.push(path)
  mobileNavOpen.value = false
}

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
  display: flex;
  flex-direction: column;
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
  flex: 1;
  padding: var(--content-padding, 24px);
  padding-left: calc(var(--content-padding, 24px) + var(--sidebar-collapsed-width, 56px) + var(--sidebar-left-offset, 12px));
}

/* 汉堡按钮：桌面隐藏，仅移动端显示 */
.mobile-menu-btn {
  display: none;
}

@media (max-width: 767px) {
  .immersive-content {
    padding-left: var(--content-padding, 24px);
  }

  .mobile-menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    background: transparent;
    color: var(--fg-primary, #1A1A1A);
    font-size: 20px;
    cursor: pointer;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .mobile-menu-btn:hover {
    background: var(--surface-secondary, #F7F8FA);
  }

  .immersive-header {
    padding: 0 12px;
    gap: 8px;
  }

  .logo-text {
    display: none;
  }

  /* header-spacer 让位，搜索框自适应剩余空间（而非固定 360px） */
  .header-spacer {
    display: none;
  }

  .search-area {
    flex: 1;
    min-width: 0;
  }

  .search-box {
    width: 100%;
  }
}

.site-footer {
  padding: 16px 24px;
  text-align: center;
  font-size: 12px;
  color: var(--fg-muted, #9CA3AF);
}

.site-footer a {
  color: var(--fg-muted, #9CA3AF);
  text-decoration: none;
  transition: color 0.2s;
}

.site-footer a:hover {
  color: var(--accent, #635BFF);
}
</style>

<!-- 非 scoped：a-drawer 内容 teleport 到 body，scoped 选择器无法命中 -->
<style>
.mobile-nav {
  display: flex;
  flex-direction: column;
}

.mobile-nav-divider {
  margin: 12px 16px 8px;
  border-top: 1px dashed var(--border-color, #E5E7EB);
}

.mobile-nav-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 11px;
  font-weight: 600;
  color: var(--fg-muted, #9CA3AF);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mobile-admin-tag {
  font-size: 10px;
  font-weight: 600;
  color: var(--accent, #33A1C9);
  background: rgba(51, 161, 201, 0.1);
  padding: 1px 6px;
  border-radius: 9999px;
}
</style>
