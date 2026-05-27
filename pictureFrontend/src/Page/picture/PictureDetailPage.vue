<template>
  <div id="PictureDetailPage" class="detail-page">
    <!-- 图片展示区 -->
    <DetailHeroSection :url="picture.url" :name="picture.name" />

    <!-- 内容区 -->
    <div class="detail-content">
      <!-- 作者信息 -->
      <DetailAuthorBar
        :userVO="picture.userVO"
        :categoryName="picture.categoryName"
        :introduction="picture.introduction"
      />

      <!-- 社交互动 + 操作 -->
      <DetailActionBar
        :socialInfo="picture.socialInfo"
        :isLiked="detailIsLiked"
        :isFavorited="detailIsFavorited"
        :canEdit="canEdit"
        @toggle-like="handleDetailLike"
        @toggle-favorite="handleDetailFavorite"
        @share="handleDetailShare"
        @download="doDownload"
        @edit="doEdit"
        @delete="doDelete"
      />

      <!-- 标签 -->
      <DetailTagsBar :tags="picture.tags" />

      <!-- 技术元数据 -->
      <DetailMetaPanel :picture="picture" />
    </div>

    <!-- 分享弹窗 -->
    <ShareModal v-model:open="shareModalOpen" :picture="picture" />
  </div>
</template>

<script setup lang="ts">
import { deletePictureUsingDelete, getPictureByIdUserUsingGet, toggleLikeUsingPost, toggleFavoriteUsingPost, recordShareUsingPost, recordViewUsingPost, recordDownloadCountUsingPost } from '@/api/pictureController';
import router from '@/router';
import { userLoginUserStore } from '@/stores/user';
import { downloadImage } from '@/util/download';
import { message, Modal } from 'ant-design-vue';
import { computed, onMounted, ref } from 'vue';
import ShareModal from '@/components/ShareModal.vue'
import DetailHeroSection from '@/components/PictureDetail/DetailHeroSection.vue'
import DetailAuthorBar from '@/components/PictureDetail/DetailAuthorBar.vue'
import DetailActionBar from '@/components/PictureDetail/DetailActionBar.vue'
import DetailTagsBar from '@/components/PictureDetail/DetailTagsBar.vue'
import DetailMetaPanel from '@/components/PictureDetail/DetailMetaPanel.vue'

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
      // 记录浏览量
      try {
        await recordViewUsingPost({ pictureId: props.id })
        if (picture.value.socialInfo) {
          picture.value.socialInfo.viewCount = (picture.value.socialInfo.viewCount ?? 0) + 1
        }
      } catch {
        // 静默失败
      }
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
const doDownload = async () => {
  downloadImage(picture.value.originUrl)
  // 记录下载量
  try {
    await recordDownloadCountUsingPost({ pictureId: props.id })
    if (picture.value.socialInfo) {
      picture.value.socialInfo.downloadCount = (picture.value.socialInfo.downloadCount ?? 0) + 1
    }
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  fetchPictureDetail()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #fafafa;
  margin: -28px;
}

.detail-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

@media (min-width: 768px) and (max-width: 1023px) {
  .detail-content {
    max-width: 100%;
    padding: 24px 16px;
  }
}

@media (max-width: 767px) {
  .detail-page {
    margin: -12px;
  }

  .detail-content {
    max-width: 100%;
    padding: 16px 12px;
  }
}
</style>
