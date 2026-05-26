<template>
    <div id="PictureDetailPage">

        <a-row :gutter="[16, 16]">
            <!-- 图片展示区 -->
            <a-col :sm="24" :md="16" :xl="18">
                <a-card title="图片预览">
                    <div style="text-align: center;">
                        <a-image style="max-height: 600px; object-fit: contain;" :src="picture.url" />
                    </div>
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
                        <!-- 社交数据 -->
                        <a-descriptions-item label="社交数据">
                            <a-space :size="16">
                                <span>
                                    <HeartOutlined :style="{ color: picture.socialInfo?.isLiked ? '#ff4d4f' : '#8c8c8c' }" />
                                    {{ picture.socialInfo?.likeCount ?? 0 }} 赞
                                </span>
                                <span>
                                    <StarOutlined :style="{ color: picture.socialInfo?.isFavorited ? '#faad14' : '#8c8c8c' }" />
                                    {{ picture.socialInfo?.favoriteCount ?? 0 }} 收藏
                                </span>
                                <span>
                                    <ShareAltOutlined />
                                    {{ picture.socialInfo?.shareCount ?? 0 }} 分享
                                </span>
                            </a-space>
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
                            {{ picture.picScale != null ? picture.picScale.toFixed(2) : '-' }}
                        </a-descriptions-item>
                        <a-descriptions-item label="大小">
                            {{ formatSize(picture.picSize) }}
                        </a-descriptions-item>
                    </a-descriptions>
                    <a-space wrap style="margin-top: 12px">
                        <!-- 社交操作按钮 -->
                        <a-button :type="detailIsLiked ? 'primary' : 'default'" :danger="detailIsLiked" @click="handleDetailLike">
                            <template #icon>
                                <HeartFilled v-if="detailIsLiked" />
                                <HeartOutlined v-else />
                            </template>
                            {{ detailIsLiked ? '已点赞' : '点赞' }}
                        </a-button>
                        <a-button :type="detailIsFavorited ? 'primary' : 'default'" @click="handleDetailFavorite"
                            :style="detailIsFavorited ? { background: '#faad14', borderColor: '#faad14', color: '#fff' } : {}">
                            <template #icon>
                                <StarFilled v-if="detailIsFavorited" />
                                <StarOutlined v-else />
                            </template>
                            {{ detailIsFavorited ? '已收藏' : '收藏' }}
                        </a-button>
                        <a-button @click="handleDetailShare">
                            <template #icon>
                                <ShareAltOutlined />
                            </template>
                            分享
                        </a-button>
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

        <!-- 分享弹窗 -->
        <ShareModal v-model:open="shareModalOpen" :picture="picture" />

    </div>

</template>


<script setup lang="ts">
import { deletePictureUsingDelete, getPictureByIdUserUsingGet, toggleLikeUsingPost, toggleFavoriteUsingPost, recordShareUsingPost } from '@/api/pictureController';
import router from '@/router';
import { userLoginUserStore } from '@/stores/user';
import { downloadImage } from '@/util/download';
import { formatSize } from '@/util/format';
import { message, Modal } from 'ant-design-vue';
import { computed, onMounted, ref } from 'vue';
import {
    HeartOutlined,
    HeartFilled,
    StarOutlined,
    StarFilled,
    ShareAltOutlined,
    EditOutlined,
    DeleteOutlined,
    DownloadOutlined,
} from '@ant-design/icons-vue'
import ShareModal from '@/components/ShareModal.vue'

const props = defineProps<{
    id: string | number
}>()

const picture = ref<API.PictureVO>({})

// 社交状态
const detailIsLiked = ref(false)
const detailIsFavorited = ref(false)
const shareModalOpen = ref(false)

// 获取图片详情  
const fetchPictureDetail = async () => {
    try {
        const res = await getPictureByIdUserUsingGet({
            id: props.id,
        })
        if (res.data.code === 0 && res.data.data) {
            picture.value = res.data.data
            detailIsLiked.value = res.data.data.socialInfo?.isLiked ?? false
            detailIsFavorited.value = res.data.data.socialInfo?.isFavorited ?? false
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
    if (!loginUser.id) {
        return false
    }
    const user = picture.value.userVO || {}
    return loginUser.id === user.id || loginUser.userRole === 'admin'
})

// 点赞
const handleDetailLike = async () => {
    try {
        const res = await toggleLikeUsingPost({ pictureId: props.id })
        if (res.data.code === 0 && res.data.data) {
            detailIsLiked.value = res.data.data.liked ?? false
            if (picture.value.socialInfo) {
                picture.value.socialInfo.likeCount = res.data.data.likeCount ?? 0
            }
        } else {
            message.error('操作失败')
        }
    } catch {
        message.error('操作失败')
    }
}

// 收藏
const handleDetailFavorite = async () => {
    try {
        const res = await toggleFavoriteUsingPost({ pictureId: props.id })
        if (res.data.code === 0 && res.data.data) {
            detailIsFavorited.value = res.data.data.favorited ?? false
            if (picture.value.socialInfo) {
                picture.value.socialInfo.favoriteCount = res.data.data.favoriteCount ?? 0
            }
        } else {
            message.error('操作失败')
        }
    } catch {
        message.error('操作失败')
    }
}

// 分享
const handleDetailShare = async () => {
    try {
        await recordShareUsingPost({ pictureId: props.id })
        if (picture.value.socialInfo) {
            picture.value.socialInfo.shareCount = (picture.value.socialInfo.shareCount ?? 0) + 1
        }
    } catch {
        // 分享计数失败不影响用户操作
    }
    shareModalOpen.value = true
}

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
