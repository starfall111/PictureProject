<template>
  <div id="userProfilePage">
    <ProfileBanner
      :userInfo="userInfo"
      :uploadCount="total"
      :likeCount="totalLikes"
      :favoriteCount="totalFavorites"
      :isCurrentUser="isCurrentUser"
    />

    <FilterRow
      v-model:selectedTag="selectedTag"
      v-model:sortOrder="sortOrder"
      :tagList="tagList"
    />

    <PictureList
      :dataList="allPictures"
      :loading="isLoading && allPictures.length === 0"
      layoutMode="waterfall"
      :showSocial="true"
      :hasMore="hasMore"
      :isLoadingMore="isLoading"
      @loadMore="onLoadMore"
    />

    <ShareModal v-model:open="shareModalOpen" :picture="sharePicture" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
// TODO: 后端添加 getUserById 接口后启用
// import { getUserByIdUsingGet } from '@/api/userController'
import { queryPictureUserUsingPost } from '@/api/pictureController'
import { userLoginUserStore } from '@/stores/user'
import ProfileBanner from '@/layouts/scheme-1-immersive/components/ProfileBanner.vue'
import FilterRow from '@/layouts/scheme-1-immersive/components/FilterRow.vue'
import PictureList from '@/components/PictureList/index.vue'
import ShareModal from '@/components/ShareModal.vue'

const props = defineProps<{
  id: string | number
}>()

const router = useRouter()
const route = useRoute()
const loginUserStore = userLoginUserStore()

// 用户信息
const userInfo = ref<API.UserVO>({})
const isCurrentUser = computed(() => {
  return String(loginUserStore.loginUser.id) === String(props.id)
})

// TODO: 后端添加 getUserById 接口后启用
// const fetchUserInfo = async () => {
//   try {
//     const res = await getUserByIdUsingGet({ id: props.id })
//     if (res.data.code === 0 && res.data.data) {
//       userInfo.value = res.data.data
//     } else {
//       message.error('获取用户信息失败')
//     }
//   } catch {
//     message.error('获取用户信息失败')
//   }
// }

// 图片列表（无限滚动）
const allPictures = ref<API.PictureVO[]>([])
const currentPage = ref(1)
const pageSize = 12
const hasMore = ref(true)
const isLoading = ref(false)
const total = ref(0)
const totalLikes = ref(0)
const totalFavorites = ref(0)

const selectedTag = ref<string | undefined>(undefined)
const sortOrder = ref('newest')

const fetchData = async (reset = false) => {
  if (isLoading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    currentPage.value = 1
    allPictures.value = []
    hasMore.value = true
  }

  isLoading.value = true

  const sortField = sortOrder.value === 'popular' ? 'likeCount' : 'createTime'
  const sortOrderVal = sortOrder.value === 'oldest' ? 'ascend' : 'descend'

  const params: API.PictureQueryDTO = {
    current: currentPage.value,
    pageSize,
    sortField,
    sortOrder: sortOrderVal,
    userId: props.id,
  }
  if (selectedTag.value) {
    params.tags = [selectedTag.value]
  }

  try {
    const res = await queryPictureUserUsingPost(params)
    if (res.data.data) {
      const records = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0

      if (reset) {
        allPictures.value = records
      } else {
        allPictures.value = [...allPictures.value, ...records]
      }

      // 统计总点赞/收藏数
      totalLikes.value = allPictures.value.reduce((sum, p) => sum + (p.socialInfo?.likeCount ?? 0), 0)
      totalFavorites.value = allPictures.value.reduce((sum, p) => sum + (p.socialInfo?.favoriteCount ?? 0), 0)

      hasMore.value = allPictures.value.length < total.value
      currentPage.value++
    } else {
      message.error('获取数据失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const onLoadMore = () => {
  fetchData(false)
}

// 从图片中提取标签列表
const tagList = computed(() => {
  const tagSet = new Set<string>()
  allPictures.value.forEach((p) => {
    p.tags?.forEach((t) => tagSet.add(t))
  })
  return Array.from(tagSet)
})

// 筛选/排序变化时重新加载
const doSearch = () => {
  fetchData(true)
  nextTick(() => setupObserver())
}

// 监听筛选变化
import { watch } from 'vue'
watch([selectedTag, sortOrder], () => {
  doSearch()
})

// 分享弹窗
const shareModalOpen = ref(false)
const sharePicture = ref<API.PictureVO>()

// IntersectionObserver
let observer: IntersectionObserver | null = null

const setupObserver = () => {
  if (observer) {
    observer.disconnect()
    observer = null
  }

  setTimeout(() => {
    const sentinel = document.getElementById('scroll-sentinel')
    if (!sentinel) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore.value && !isLoading.value) {
          onLoadMore()
        }
      },
      { rootMargin: '200px' }
    )
    observer.observe(sentinel)
  }, 100)
}

onMounted(async () => {
  // TODO: 后端添加 getUserById 接口后启用
  // await fetchUserInfo()
  await fetchData(true)
  setupObserver()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped>
#userProfilePage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 24px;
}
</style>
