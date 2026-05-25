import router from '@/router'
import { userLoginUserStore } from '@/stores/user'
import ACCESS_ENUM from './accessEnum'
import checkAccess from './checkAccess'

router.beforeEach(async (to, from, next) => {
  const loginUserStore = userLoginUserStore()
  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
  // 如果还没有用户信息，尝试从后端获取（解决刷新后状态丢失问题）
  if (!loginUserStore.loginUser.userRole) {
    await loginUserStore.getLoginUser(true)
  }
  let loginUser = loginUserStore.loginUser
  // 如果没登陆，跳转到登录页面
  if (!loginUser || !loginUser.userRole || loginUser.userRole === ACCESS_ENUM.NOT_LOGIN) {
    if (needAccess !== ACCESS_ENUM.NOT_LOGIN) {
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  // 如果已经登陆了，但是权限不足，那么跳转到无权限页面
  if (!checkAccess(loginUser, needAccess)) {
    next('/noAuth')
    return
  }
  next()
})
