<template>
    <div id="addSpacePage">
        <h2 style="margin-bottom: 16px">
            {{ route.query?.id ? '修改空间' : '创建空间' }}
        </h2>

        <a-form layout="vertical" :model="spaceForm" @finish="handleSubmit">
            <a-form-item label="名称" name="spaceName">
                <a-input v-model:value="spaceForm.spaceName" placeholder="请输入名称" />
            </a-form-item>
            <a-form-item label="空间级别" name="spaceLevel">
                <a-select v-model:value="spaceForm.spaceLevel" :options="SPACE_LEVEL_OPTIONS" placeholder="请输入空间级别"
                    style="min-width: 180px" allow-clear />
            </a-form-item>

            <a-form-item>
                <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
                    {{ route.query?.id ? '修改' : '创建' }}</a-button>
            </a-form-item>
        </a-form>

        <a-card title="空间级别介绍">
            <a-typography-paragraph>
                * 目前仅支持开通普通版，如需升级空间，请联系作者邮箱 
                1824109129@qq.com
                <!-- <a href="https://codefather.cn" target="_blank">程序员鱼皮</a>。 -->
            </a-typography-paragraph>
            <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
                {{ spaceLevel.text }}： 大小 {{ formatSize(spaceLevel.maxSize) }}， 数量
                {{ spaceLevel.maxCount }}
            </a-typography-paragraph>
        </a-card>

    </div>

</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { spaceControllerAddSpace, spaceControllerEditSpace, spaceControllerGetSpaceById, spaceControllerListSpaceLevel, spaceControllerUpdateSpace } from '@/api/spaceController';
import { message, Modal } from 'ant-design-vue';
import { userLoginUserStore } from '@/stores/user';
import { SPACE_LEVEL_OPTIONS } from '@/constants/space';
import { formatSize } from '@/util/format';

const spaceForm = reactive<API.SpaceAddDTO>({})
const space = ref<API.SpaceVO>({})

const router = useRouter()
const uploadType = ref<'file' | 'url'>('file')
const loading = ref(false)

const spaceLevelList = ref<API.SpaceLevel[]>([])

// 获取空间级别
const fetchSpaceLevelList = async () => {
    const res = await spaceControllerListSpaceLevel()
    if (res.data.code === 0 && res.data.data) {
        spaceLevelList.value = res.data.data
    } else {
        message.error('加载空间级别失败，' + res.data.message)
    }
}

onMounted(() => {
    fetchSpaceLevelList()
})

/**  
 * 提交表单  
 * @param values  
 */
const handleSubmit = async (values: any) => {
    loading.value = true
    let spaceId = space.value.id
    console.log(values)
    let res = null
    if (spaceId) {
        res = await spaceControllerUpdateSpace({
            id: spaceId,
            ...values,
        })
    } else {
        res = await spaceControllerAddSpace({
            ...values,
        })
    }

    if (res.data.code === 0 && res.data.data) {
        message.success('创建成功')
        loading.value = false
        spaceId = res.data.data
        // 跳转到空间详情页  
        router.push({
            path: `/space/${spaceId}`,
        })
    } else {
        message.error('创建失败，' + res.data.message)
    }
}


const route = useRoute()

// 获取老数据
const getOldSpace = async () => {
    const id = route.query?.id
    if (id) {
        const res = await spaceControllerGetSpaceById({
            id: id,
        })
        if (res.data.code === 0 && res.data.data) {
            const data = res.data.data
            space.value = data
            spaceForm.spaceName = data.spaceName
            spaceForm.spaceLevel = data.spaceLevel
        }
    }
}

onMounted(async () => {
    const loginUserStore = userLoginUserStore()
    await loginUserStore.getLoginUser(true)
    const loginUser = loginUserStore.loginUser
    if (!loginUser.userPhone && loginUser.userRole !== 'admin') {
        Modal.confirm({
            title: '请先绑定手机号',
            content: '创建空间前需要绑定手机号，是否前往个人中心绑定？',
            okText: '去绑定',
            cancelText: '返回主页',
            onOk: () => {
                router.push('/user/center')
            },
            onCancel: () => {
                router.push('/')
            },
        })
        return
    }
    await getOldSpace()
})


</script>

<style>
#addSpacePage {
    max-width: 720px;
    margin: 0 auto;
}
</style>
