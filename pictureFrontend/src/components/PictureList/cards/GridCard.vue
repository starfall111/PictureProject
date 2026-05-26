<template>
  <a-card hoverable @click="doClickPicture">
    <template #cover>
      <img style="height: 180px; object-fit: cover" :alt="picture.name"
        :src="picture.url" loading="lazy" />
    </template>
    <a-card-meta :title="picture.name">
      <template #description>
        <a-flex>
          <a-tag color="green">
            {{ picture.categoryName ?? '默认' }}
          </a-tag>
          <a-tag v-for="tag in picture.tags" :key="tag">
            {{ tag }}
          </a-tag>
        </a-flex>
      </template>
    </a-card-meta>
    <template v-if="showOp" #actions>
      <a-space @click="doSearch">
        <search-outlined />
        搜索
      </a-space>
      <a-space @click="doEdit">
        <edit-outlined />
        编辑
      </a-space>
      <a-space @click="doDelete">
        <delete-outlined />
        删除
      </a-space>
    </template>
  </a-card>
</template>

<script setup lang="ts">
import { deletePictureUsingDelete } from '@/api/pictureController'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'

interface Props {
  picture: API.PictureVO
  showOp?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  showOp: false,
})

const router = useRouter()

const doClickPicture = () => {
  router.push({ path: `/picture/${props.picture.id}` })
}

const doSearch = (e: Event) => {
  e.stopPropagation()
  window.open(`/search_picture?pictureId=${props.picture.id}`)
}

const doEdit = (e: Event) => {
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: { id: String(props.picture.id), spaceId: String(props.picture.spaceId) },
  })
}

const doDelete = (e: Event) => {
  e.stopPropagation()
  const id = props.picture.id
  if (!id) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该图片吗？此操作不可恢复。',
    okText: '确定删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deletePictureUsingDelete({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        props?.onReload?.()
      } else {
        message.error('删除失败')
      }
    },
  })
}
</script>
