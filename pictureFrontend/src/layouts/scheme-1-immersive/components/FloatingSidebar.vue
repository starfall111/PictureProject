<template>
  <aside
    class="floating-sidebar"
    :class="{ expanded: isExpanded, pinned: isPinned }"
    role="navigation"
    aria-label="主导航"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- 普通导航项 -->
    <nav class="sidebar-nav">
      <SidebarNavItem
        v-for="item in userNavItems"
        :key="item.path"
        :icon="item.icon"
        :label="item.label"
        :active="isCurrentRoute(item.path)"
        :show-label="isExpanded"
        @click="navigate(item.path)"
      />
    </nav>

    <!-- 管理员分组 -->
    <template v-if="isAdmin">
      <div class="sidebar-divider"></div>
      <div class="sidebar-admin-group">
        <div
          class="admin-group-header"
          :class="{ active: isAdminRouteActive }"
          @click="adminExpanded = !adminExpanded"
        >
          <SafetyCertificateOutlined class="nav-item-icon admin-icon" />
          <span v-if="isExpanded" class="admin-label">管理面板</span>
          <RightOutlined
            v-if="isExpanded"
            class="expand-arrow"
            :class="{ rotated: adminExpanded }"
          />
          <span v-if="isExpanded" class="admin-tag">Admin</span>
        </div>
        <Transition name="slide">
          <div v-if="adminExpanded && isExpanded" class="admin-group-items">
            <SidebarNavItem
              v-for="item in adminNavItems"
              :key="item.path"
              :icon="item.icon"
              :label="item.label"
              :active="isCurrentRoute(item.path)"
              :indented="true"
              :show-label="true"
              @click="navigate(item.path)"
            />
          </div>
        </Transition>
      </div>
    </template>

    <!-- 底部 pin 按钮 -->
    <div class="sidebar-footer">
      <button
        class="pin-btn"
        :class="{ active: isPinned }"
        @click="togglePin"
        :aria-label="isPinned ? '取消钉住侧边栏' : '钉住侧边栏'"
      >
        <PushpinOutlined :rotate="isPinned ? -45 : 0" />
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, h, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  PictureOutlined,
  FolderOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  FileImageOutlined,
  AppstoreOutlined,
  TagsOutlined,
  RightOutlined,
  PushpinOutlined,
} from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/user'
import checkAccess from '@/access/checkAccess'
import ACCESS_ENUM from '@/access/accessEnum'
import SidebarNavItem from './SidebarNavItem.vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = userLoginUserStore()

const isExpanded = ref(false)
const isPinned = ref(false)
const adminExpanded = ref(false)

let expandTimer: ReturnType<typeof setTimeout> | null = null
let collapseTimer: ReturnType<typeof setTimeout> | null = null

// 判断是否为管理员
const isAdmin = computed(() =>
  checkAccess(loginUserStore.loginUser, ACCESS_ENUM.ADMIN)
)

// 普通用户导航项
const userNavItems = computed(() => {
  const items: { path: string; icon: Component; label: string }[] = [
    { path: '/home', icon: PictureOutlined, label: '公共图库' },
    { path: '/my_space', icon: FolderOutlined, label: '个人空间' },
    { path: `/user/${loginUserStore.loginUser.id}`, icon: UserOutlined, label: '用户主页' },
  ]
  // 如果用户未登录，隐藏个人空间和用户主页
  if (!loginUserStore.loginUser.id) {
    return [items[0]]
  }
  return items
})

// 管理员导航项
const adminNavItems = [
  { path: '/admin/manage', icon: TeamOutlined, label: '用户管理' },
  { path: '/admin/pictureManage', icon: FileImageOutlined, label: '图片管理' },
  { path: '/admin/spaceManage', icon: AppstoreOutlined, label: '空间管理' },
  { path: '/admin/categoryManage', icon: FolderOutlined, label: '分类管理' },
  { path: '/admin/tagManage', icon: TagsOutlined, label: '标签管理' },
]

// 当前路由匹配
const isCurrentRoute = (path: string) => {
  if (path.includes(':')) {
    // 动态路由如 /user/:id，用正则匹配
    const regex = new RegExp('^' + path.replace(/:[^/]+/g, '[^/]+') + '$')
    return regex.test(route.path)
  }
  return route.path === path
}

// 管理路由是否激活
const isAdminRouteActive = computed(() =>
  adminNavItems.some(item => isCurrentRoute(item.path))
)

// 鼠标交互
const handleMouseEnter = () => {
  if (collapseTimer) {
    clearTimeout(collapseTimer)
    collapseTimer = null
  }
  expandTimer = setTimeout(() => {
    isExpanded.value = true
  }, 120)
}

const handleMouseLeave = () => {
  if (expandTimer) {
    clearTimeout(expandTimer)
    expandTimer = null
  }
  if (isPinned.value) return
  collapseTimer = setTimeout(() => {
    isExpanded.value = false
  }, 300)
}

const togglePin = () => {
  isPinned.value = !isPinned.value
  if (isPinned.value) {
    isExpanded.value = true
  }
}

const navigate = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.floating-sidebar {
  position: fixed;
  top: var(--sidebar-top-offset, 68px);
  left: var(--sidebar-left-offset, 12px);
  bottom: var(--sidebar-bottom-offset, 24px);
  width: var(--sidebar-collapsed-width, 56px);
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg, rgba(255, 255, 255, 0.92));
  backdrop-filter: blur(var(--sidebar-blur, 20px));
  -webkit-backdrop-filter: blur(var(--sidebar-blur, 20px));
  border: 1px solid var(--sidebar-border, rgba(0, 0, 0, 0.06));
  box-shadow: var(--sidebar-shadow, 0 8px 32px rgba(0, 0, 0, 0.08));
  border-radius: var(--sidebar-radius, 16px);
  padding: 12px 0;
  z-index: 100;
  transition: width var(--sidebar-expand-duration, 200ms) var(--sidebar-expand-easing, ease-out);
  overflow: hidden;
}

.floating-sidebar.expanded {
  width: var(--sidebar-expanded-width, 220px);
}

/* 导航区 */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 分隔线 */
.sidebar-divider {
  margin: 8px 16px;
  border-top: 1px dashed var(--sidebar-admin-divider-color, rgba(0, 0, 0, 0.08));
}

/* 管理员分组 */
.sidebar-admin-group {
  flex-shrink: 0;
}

.admin-group-header {
  display: flex;
  align-items: center;
  height: var(--sidebar-item-height, 44px);
  padding: 0 16px;
  gap: 12px;
  cursor: pointer;
  border-radius: var(--sidebar-item-radius, 10px);
  margin: var(--sidebar-item-gap, 2px) 6px;
  transition: background-color 150ms ease;
  user-select: none;
}

.admin-group-header:hover {
  background: var(--sidebar-item-bg-hover, rgba(51, 161, 201, 0.08));
}

.admin-group-header.active {
  background: var(--sidebar-item-bg-active, rgba(51, 161, 201, 0.12));
}

.admin-icon {
  font-size: 20px;
  color: var(--sidebar-item-icon-default, #4B5563);
  flex-shrink: 0;
}

.admin-group-header:hover .admin-icon {
  color: var(--sidebar-item-accent, #33A1C9);
}

.admin-label {
  font-size: var(--sidebar-admin-label-size, 11px);
  font-weight: 600;
  color: var(--sidebar-admin-label-color, #9CA3AF);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  flex: 1;
}

.admin-group-header:hover .admin-label {
  color: var(--sidebar-item-accent, #33A1C9);
}

.admin-tag {
  font-size: 10px;
  font-weight: 600;
  color: var(--sidebar-item-accent, #33A1C9);
  background: rgba(51, 161, 201, 0.1);
  padding: 1px 6px;
  border-radius: 9999px;
}

.expand-arrow {
  font-size: 10px;
  color: var(--fg-muted, #9CA3AF);
  transition: transform 200ms ease;
  flex-shrink: 0;
}

.expand-arrow.rotated {
  transform: rotate(90deg);
}

/* 管理子项展开动画 */
.admin-group-items {
  overflow: hidden;
}

.slide-enter-active,
.slide-leave-active {
  transition: max-height 200ms ease-out, opacity 200ms ease;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
}

.slide-enter-to,
.slide-leave-from {
  max-height: 300px;
  opacity: 1;
}

/* 底部 pin 按钮 */
.sidebar-footer {
  margin-top: auto;
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

.pin-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  color: var(--fg-muted, #9CA3AF);
  font-size: 16px;
  transition: background-color 150ms ease, color 150ms ease, transform 300ms ease;
}

.pin-btn:hover {
  background: var(--sidebar-item-bg-hover, rgba(51, 161, 201, 0.08));
  color: var(--sidebar-item-accent, #33A1C9);
}

.pin-btn.active {
  color: var(--sidebar-item-accent, #33A1C9);
}

/* 移动端隐藏侧边栏 */
@media (max-width: 767px) {
  .floating-sidebar {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .floating-sidebar,
  .admin-group-header,
  .expand-arrow,
  .pin-btn {
    transition: none;
  }
}
</style>
