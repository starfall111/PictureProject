import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginPage from '@/Page/user/loginPage.vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
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
import { pictureUploadByBatchUsingPost } from '@/api/pictureController'
import PictureUploadByBatchPage from '@/Page/picture/PictureUploadByBatchPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      component: BasicLayout,
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
          path: 'about',
          name: 'about',
          component: () => import('../views/AboutView.vue'),
        },
      ],
    },
  ],
})

export default router
