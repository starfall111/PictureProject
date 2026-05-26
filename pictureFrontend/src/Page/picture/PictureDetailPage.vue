<template>
    <div id="PictureDetailPage">

        <a-row :gutter="[16, 16]">
            <!-- 图片展示区 -->
            <a-col :sm="24" :md="16" :xl="18">
                <a-card title="图片预览">
                    <a-image style="max-height: 600px; object-fit: contain; margin: 0 auto;" :src="picture.url" />
                </a-card>
            </a-col>
            <!-- 图片信息区 -->
            <a-col :sm="24" :md="8" :xl="6">
                <a-card title="图片信息">
                    <a-descriptions :column="1">
                        <a-descriptions-item label="作者">
                            <a-space>
                                <a-avatar :size="24" :src="picture.userVO?.userAvatar" />
                                <div>{{ picture.userVO?.userName }}</div>
                            </a-space>
                        </a-descriptions-item>
                        <a-descriptions-item label="名称">
                            {{ picture.name ?? '未命名' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="简介">
                            {{ picture.introduction ?? '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="分类">
                            {{ picture.categoryName ?? '默认' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="标签">
                            <a-tag v-for="tag in picture.tags" :key="tag">
                                {{ tag }}
                            </a-tag>
                        </a-descriptions-item>
                        <a-descriptions-item label="格式">
                            {{ picture.picFormat ?? '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="宽度">
                            {{ picture.picWidth ?? '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="高度">
                            {{ picture.picHeight ?? '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="宽高比">
                            {{ picture.picScale ?? '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="大小">
                            {{ formatSize(picture.picSize) }}
                        </a-descriptions-item>
                    </a-descriptions>
                    <a-space wrap>
                        <a-button v-if="canEdit" type="default" @click="doEdit">
                            编辑
                            <template #icon>
                                <EditOutlined />
                            </template>
                        </a-button>
                        <a-button v-if="canEdit" danger @click="doDelete">
                            删除
                            <template #icon>
                                <DeleteOutlined />
                            </template>
                        </a-button>
                        <a-button type="primary" @click="doDownload">
                            免费下载
                            <template #icon>
                                <DownloadOutlined />
                            </template>
                        </a-button>
                    </a-space>

                </a-card>
            </a-col>
        </a-row>

    </div>

</template>


<script setup lang="ts">
import { deletePictureUsingDelete, getPictureByIdUserUsingGet } from '@/api/pictureController';
import router from '@/router';
import { userLoginUserStore } from '@/stores/user';
import { downloadImage } from '@/util/download';
import { formatSize } from '@/util/format';
import { message, Modal } from 'ant-design-vue';
import { computed, onMounted, ref } from 'vue';


const props = defineProps<{
    id: string | number
}>()

const picture = ref<API.PictureVO>({})

// 获取图片详情  
const fetchPictureDetail = async () => {
    try {
        const res = await getPictureByIdUserUsingGet({
            id: props.id,
        })
        if (res.data.code === 0 && res.data.data) {
            picture.value = res.data.data
        } else {
            message.error('获取图片详情失败，' + res.data.message)
        }
    } catch (e: any) {
        message.error('获取图片详情失败：' + e.message)
    }
}

const loginUserStore = userLoginUserStore()
// 是否具有编辑权限  
const canEdit = computed(() => {
    const loginUser = loginUserStore.loginUser;
    // 未登录不可编辑  
    if (!loginUser.id) {
        return false
    }
    // 仅本人或管理员可编辑  
    const user = picture.value.user || {}
    return loginUser.id === user.id || loginUser.userRole === 'admin'
})

// 编辑  
const doEdit = () => {
    router.push('/add_picture?id=' + picture.value.id)
}
// 删除
const doDelete = () => {
    Modal.confirm({
        title: '确认删除',
        content: '确定要删除该图片吗？此操作不可恢复。',
        okText: '确定删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
            const id = picture.value.id
            if (!id) {
                return
            }
            const res = await deletePictureUsingDelete({ id })
            if (res.data.code === 0) {
                message.success('删除成功')
            } else {
                message.error('删除失败')
            }
        },
    })
}

// 处理下载  
const doDownload = () => {  
  downloadImage(picture.value.originUrl)  
}



onMounted(() => {
    fetchPictureDetail()
})




</script>

<style></style>