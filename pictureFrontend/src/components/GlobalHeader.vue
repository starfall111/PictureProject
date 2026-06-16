<template>
    <div>
        <a-row :warp="false" align="middle">
            <a-col flex="200px">
                <router-link to="/">
                    <div class="title-bar">
                        <img class="img" src="../assets/test.jpg" alt="logo">
                        <div class="title">图境</div>
                    </div>
                </router-link>
            </a-col>
            <a-col flex="auto">
                <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick" />
            </a-col>
            <a-col flex="120px">
                <div class="user-login-status">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <NotificationBell v-if="loginUserStore.loginUser.id" />
                        <div v-if="loginUserStore.loginUser.id">
                            <a-dropdown>
                                <a-space>
                                    <a-avatar :src="loginUserStore.loginUser.userAvatar"></a-avatar>
                                    {{ loginUserStore.loginUser.userName ?? '无名' }}
                                </a-space>
                                <template #overlay>
                                    <a-menu>
                                        <a-menu-item @click="router.push('/user/center')">
                                            <UserOutlined />
                                            个人中心
                                        </a-menu-item>
                                        <a-menu-item>
                                            <router-link to="/my_space">
                                                <UserOutlined />
                                                我的空间
                                            </router-link>
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
                        </div>
                        <div v-else>
                            <a-button type="primary" href="/user/login">登录</a-button>
                        </div>
                    </div>
                </div>
            </a-col>

        </a-row>
    </div>
</template>


<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import { message, type MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userLoginUserStore } from '@/stores/user'
import { useSeckillStore } from '@/stores/seckill'
import { LogoutOutlined, UserOutlined, CommentOutlined } from '@ant-design/icons-vue'
import { userControllerLogOut } from '@/api/userController'
import checkAccess from '@/access/checkAccess'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import VipBadge from '@/components/vip/VipBadge.vue'


const loginUserStore = userLoginUserStore()

const current = ref<string[]>([])
const originItems = [
    {
        key: '/',
        icon: () => h(HomeOutlined),
        label: '主页',
        title: '主页',
    },
    {
        key: '/admin/manage',
        label: '用户管理',
        title: '用户管理',
    },
    {
        key: '/add_picture',
        label: '创建图片',
        title: '创建图片',
    },
    {
        key: '/add_picture/batch',
        label: () => h('span', { style: 'display: inline-flex; align-items: center;' }, [
            '批量获取',
            h(VipBadge, { feature: true, size: 'default', style: 'margin-left: 8px' }),
        ]),
        title: '批量获取',
    },
    {
        key: '/admin/pictureManage',
        label: '图片管理',
        title: '图片管理',
    },
    {
        key: '/admin/spaceManage',
        label: '空间管理',
        title: '空间管理',
    },

    {
        key: '/admin/categoryManage',
        label: '分类管理',
        title: '分类管理',
    },
    {
        key: '/admin/tagManage',
        label: '标签管理',
        title: '标签管理',
    },
    {
        key: '/admin/systemMessageManage',
        label: '系统消息管理',
        title: '系统消息管理',
    },
]

const router = useRouter()

const doMenuClick = ({ key }: { key: string }) => {
    router.push({
        path: key
    })
}

router.afterEach((to, from, next) => {
    current.value = [to.path]
})

const logout = async () => {
    const res = await userControllerLogOut();
    if (res.data.code === 0 && res.data.data) {
        loginUserStore.setLoginUser({});
        useSeckillStore().clearToken();
        message.success('退出登录成功');
        router.push('/user/login');
    } else {
        message.error('退出登录失败');
    }
}

const filterMenus = (menus = [] as MenuProps['items']) => {
    return menus?.filter(menu => {
        const menuItem = menu as { key: string }
        const route = router.getRoutes().find(r => r.path === menuItem.key)
        if (route?.meta?.access) {
            return checkAccess(loginUserStore.loginUser, route.meta.access as string)
        }
        return true
    })
}

const items = computed<MenuProps['items']>(() => filterMenus(originItems))

</script>

<style>
.title-bar {
    display: flex;
    align-items: center;
}

.img {
    height: 48px;
}

.title {
    font-size: 24px;
    color: black;
    margin-left: 16px;
}

</style>
