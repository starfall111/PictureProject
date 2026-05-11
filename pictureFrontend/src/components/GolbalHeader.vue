<template>
    <div>
        <a-row :warp="false">
            <a-col flex="200px">
                <router-link to="/">
                    <div class="title-bar">
                        <img class="img" src="../assets/test.jpg" alt="logo">
                        <div class="title">智能云图库</div>
                    </div>
                </router-link>
            </a-col>
            <a-col flex="auto">
                <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick" />
            </a-col>
            <a-col flex="120px">
                <div class="user-login-status">
                    <div v-if="loginUserStore.loginUser.id">
                        {{ loginUserStore.loginUser.userName ?? '无名' }} 
                    </div>
                    <div v-else>
                        <a-button type="primary" href="/user/login">登录</a-button>
                    </div>

                </div>
            </a-col>

        </a-row>
    </div>
</template>


<script lang="ts" setup>
import { h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userLoginUserStore } from '@/stores/user'

const loginUserStore = userLoginUserStore()

const current = ref<string[]>([])
const items = ref<MenuProps['items']>([
    {
        key: '/',
        icon: () => h(HomeOutlined),
        label: '主页',
        title: '主页',
    },
    {
        key: '/about',
        label: '关于',
        title: '关于',
    },
    {
        key: 'others',
        label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
        title: '编程导航',
    },
])

const router = useRouter()

const doMenuClick = ({ key }: { key: string }) => {
    router.push({
        path: key
    })
}

router.afterEach((to, from, next) => {
    current.value = [to.path]
})
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
