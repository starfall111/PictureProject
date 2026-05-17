import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginPage from '@/Page/user/loginPage.vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import RegisterPage from '@/Page/user/RegisterPage.vue'
import UserManagePage from '@/Page/user/UserManagePage.vue'
import UserCenterPage from '@/Page/user/UserCenterPage.vue'
import NoAuthPage from '@/Page/noAuth.vue'
import ACCESS_ENUM from '@/access/accessEnum'

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
          path: 'about',
          name: 'about',
          component: () => import('../views/AboutView.vue'),
        },
      ],
    },
  ],
})

export default router
