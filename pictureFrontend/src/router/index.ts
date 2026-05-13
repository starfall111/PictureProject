import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginPage from '@/Page/loginPage.vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import RegisterPage from '@/Page/RegisterPage.vue'

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
          path: 'about',
          name: 'about',
          component: () => import('../views/AboutView.vue'),
        },
      ],
    },
  ],
})

export default router
