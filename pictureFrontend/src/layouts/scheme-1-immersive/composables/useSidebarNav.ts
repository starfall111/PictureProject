import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import {
  PictureOutlined,
  FolderOutlined,
  UserOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  FileImageOutlined,
  AppstoreOutlined,
  TagsOutlined,
  MessageOutlined,
  StopOutlined,
  WarningOutlined,
  CommentOutlined,
  CloudDownloadOutlined,
  ScheduleOutlined,
  CrownOutlined,
  IeOutlined,
  StarOutlined,
} from '@ant-design/icons-vue'
import { userLoginUserStore } from '@/stores/user'
import checkAccess from '@/access/checkAccess'
import ACCESS_ENUM from '@/access/accessEnum'

export interface SidebarNavItemData {
  path: string
  icon: Component
  label: string
  feature?: boolean
}

/**
 * 侧边栏导航数据源（桌面 FloatingSidebar 与移动端 Drawer 共用）
 * 集中维护菜单项、权限判断与路由高亮，避免双份菜单不同步。
 */
export function useSidebarNav() {
  const route = useRoute()
  const loginUserStore = userLoginUserStore()

  const isAdmin = computed(() =>
    checkAccess(loginUserStore.loginUser, ACCESS_ENUM.ADMIN)
  )

  // 普通用户导航项
  const userNavItems = computed<SidebarNavItemData[]>(() => {
    const items: SidebarNavItemData[] = [
      { path: '/home', icon: PictureOutlined, label: '公共图库' },
      { path: '/my_space', icon: FolderOutlined, label: '个人空间' },
      { path: '/add_picture/batch', icon: CloudDownloadOutlined, label: '批量获取', feature: true },
      { path: '/seckill', icon: CrownOutlined, label: '限时抢券' },
      { path: `/user/${loginUserStore.loginUser.id}`, icon: UserOutlined, label: '用户主页' },
      { path: '/feed', icon: ThunderboltOutlined, label: '动态' },
      { path: '/feedback/list', icon: CommentOutlined, label: '我的反馈' },
    ]
    // 未登录只保留公共图库
    if (!loginUserStore.loginUser.id) {
      return [items[0]]
    }
    return items
  })

  // 管理员导航项
  const adminNavItems: SidebarNavItemData[] = [
    { path: '/admin/manage', icon: TeamOutlined, label: '用户管理' },
    { path: '/admin/pictureManage', icon: FileImageOutlined, label: '图片管理' },
    { path: '/admin/spaceManage', icon: AppstoreOutlined, label: '空间管理' },
    { path: '/admin/categoryManage', icon: FolderOutlined, label: '分类管理' },
    { path: '/admin/tagManage', icon: TagsOutlined, label: '标签管理' },
    { path: '/admin/systemMessageManage', icon: MessageOutlined, label: '系统消息管理' },
    { path: '/admin/feedbackManage', icon: CommentOutlined, label: '反馈管理' },
    { path: '/admin/reportManage', icon: WarningOutlined, label: '举报管理' },
    { path: '/admin/banManage', icon: StopOutlined, label: '封禁管理' },
    { path: '/admin/batchTaskManage', icon: ScheduleOutlined, label: '批量任务管理' },
    { path: '/admin/batchManage', icon: CrownOutlined, label: '批次管理' },
    { path: '/admin/couponManage', icon: IeOutlined, label: '券管理' },
    { path: '/admin/vipManage', icon: StarOutlined, label: 'VIP管理' },
  ]

  // 当前路由匹配（支持 /user/:id 这类动态路由）
  const isCurrentRoute = (path: string) => {
    if (path.includes(':')) {
      const regex = new RegExp('^' + path.replace(/:[^/]+/g, '[^/]+') + '$')
      return regex.test(route.path)
    }
    return route.path === path
  }

  const isAdminRouteActive = computed(() =>
    adminNavItems.some((item) => isCurrentRoute(item.path))
  )

  return { userNavItems, adminNavItems, isAdmin, isCurrentRoute, isAdminRouteActive }
}
