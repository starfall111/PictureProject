import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginPage from '@/Page/user/loginPage.vue'
import LayoutSwitcher from '@/layouts/LayoutSwitcher.vue'
import RegisterPage from '@/Page/user/RegisterPage.vue'
import UserManagePage from '@/Page/user/UserManagePage.vue'
import UserCenterPage from '@/Page/user/UserCenterPage.vue'
import NoAuthPage from '@/Page/noAuth.vue'
import ACCESS_ENUM from '@/access/accessEnum'
import AddPicturePage from '@/Page/picture/AddPicturePage.vue'
import PictureManagePage from '@/Page/picture/PictureManagePage.vue'
import PictureDetailPage from '@/Page/picture/PictureDetailPage.vue'
import CategoryManagePage from '@/Page/category/CategoryManagePage.vue'
import TagManagePage from '@/Page/tag/TagManagePage.vue'
import PictureUploadByBatchPage from '@/Page/picture/PictureUploadByBatchPage.vue'
import SpaceManagePage from '@/Page/space/SpaceManagePage.vue'
import AddSpacePage from '@/Page/space/AddSpacePage.vue'
import MySpacePage from '@/Page/space/MySpacePage.vue'
import SpaceDetailPage from '@/Page/space/SpaceDetailPage.vue'
import PictureSearchPage from '@/Page/picture/PictureSearchPage.vue'
import UserProfilePage from '@/Page/user/UserProfilePage.vue'
import NotificationPage from '@/Page/notification/NotificationPage.vue'
import FeedPage from '@/Page/feed/FeedPage.vue'
import SystemMessageManagePage from '@/Page/systemMessage/SystemMessageManagePage.vue'
import FeedbackSubmitPage from '@/Page/feedback/FeedbackSubmitPage.vue'
import FeedbackListPage from '@/Page/feedback/FeedbackListPage.vue'
import FeedbackDetailPage from '@/Page/feedback/FeedbackDetailPage.vue'
import MyReportListPage from '@/Page/report/MyReportListPage.vue'
import AdminFeedbackManagePage from '@/Page/admin/AdminFeedbackManagePage.vue'
import AdminReportManagePage from '@/Page/admin/AdminReportManagePage.vue'
import AdminBanManagePage from '@/Page/admin/AdminBanManagePage.vue'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    // 登录/注册等页面 —— 无 BasicLayout 框架
    {
      path: '/user/login',
      name: 'login',
      component: LoginPage,
    },
    {
      path: '/user/register',
      name: 'register',
      component: RegisterPage,
    },
    {
      path: '/noAuth',
      name: 'noAuth',
      component: NoAuthPage,
    },
    // 其他页面 —— 带 BasicLayout 框架（Header + Footer）
    {
      path: '/',
      component: LayoutSwitcher,
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'home',
          component: HomeView,
        },
        {
          path: '/admin/manage',
          name: 'adminManage',
          component: UserManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/user/center',
          name: 'userCenter',
          component: UserCenterPage,
          meta: {
            access: ACCESS_ENUM.USER
          }
        },
        {
          path: '/my_space',
          name: '我的空间',
          component: MySpacePage,
        },
        {
          path: '/space/:id',
          name: '空间详情',
          component: SpaceDetailPage,
          props: true,
        },

        {
          path: '/add_picture',
          name: '创建图片',
          component: AddPicturePage,
          meta: {
            access: ACCESS_ENUM.USER
          }
        },
        {
          path: '/admin/pictureManage',
          name: '图片管理',
          component: PictureManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/admin/spaceManage',
          name: '空间管理',
          component: SpaceManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/admin/categoryManage',
          name: '分类管理',
          component: CategoryManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/admin/tagManage',
          name: '标签管理',
          component: TagManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/admin/systemMessageManage',
          name: '系统消息管理',
          component: SystemMessageManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN
          }
        },
        {
          path: '/picture/:id',
          name: '图片详情',
          component: PictureDetailPage,
          props: true,
        },
        {
          path: '/add_picture/batch',
          name: '批量抓图',
          component: PictureUploadByBatchPage,
          props: true,
        },
        {
          path: '/search_picture',
          name: '图片搜索',
          component: PictureSearchPage,
        },
        {
          path: '/user/:id',
          name: '用户主页',
          component: UserProfilePage,
          props: true,
        },
        {
          path: '/add_space',
          name: '创建空间',
          component: AddSpacePage,
        },
        {
          path: '/notifications',
          name: '通知中心',
          component: NotificationPage,
          meta: {
            access: ACCESS_ENUM.USER,
          },
        },
        {
          path: '/feed',
          name: '动态',
          component: FeedPage,
          meta: {
            access: ACCESS_ENUM.USER,
          },
        },
        // 反馈路由
        {
          path: '/feedback/submit',
          name: '提交反馈',
          component: FeedbackSubmitPage,
          meta: {
            access: ACCESS_ENUM.USER,
          },
        },
        {
          path: '/feedback/list',
          name: '我的反馈',
          component: FeedbackListPage,
          meta: {
            access: ACCESS_ENUM.USER,
          },
        },
        {
          path: '/feedback/:id',
          name: '反馈详情',
          component: FeedbackDetailPage,
          props: true,
        },
        // 举报路由
        {
          path: '/report/list',
          name: '我的举报',
          component: MyReportListPage,
          meta: {
            access: ACCESS_ENUM.USER,
          },
        },
        // 管理端 — 反馈 / 举报 / 封禁
        {
          path: '/admin/feedbackManage',
          name: '反馈管理',
          component: AdminFeedbackManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN,
          },
        },
        {
          path: '/admin/reportManage',
          name: '举报管理',
          component: AdminReportManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN,
          },
        },
        {
          path: '/admin/banManage',
          name: '封禁管理',
          component: AdminBanManagePage,
          meta: {
            access: ACCESS_ENUM.ADMIN,
          },
        },
      ],
    },
  ],
})

export default router
