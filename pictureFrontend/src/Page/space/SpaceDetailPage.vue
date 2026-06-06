<template>

  <div id="SpaceDetailPage">
    <!-- 沉浸式：SpaceBanner -->
    <SpaceBanner
      v-if="isImmersive"
      :space="space"
      :spaceId="id"
      :totalCount="total"
    />
    <!-- 默认布局：原始头部 -->
    <a-flex v-else justify="space-between">
      <h2>{{ space.spaceName }}（私有空间）</h2>
      <a-space size="middle">
        <a-button type="primary" :href="`/add_picture?spaceId=${id}`" target="_blank">
          + 创建图片
        </a-button>
        <a-tooltip :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
          <a-progress type="circle" :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)" :size="42" />
        </a-tooltip>
      </a-space>
      <!-- 搜索表单 -->
    </a-flex>

    <PictureSearchForm :onSearch="onSearch" />

    <div style="height: 16px;"></div>

    <!-- 图片列表 — 私人空间固定使用网格布局，无社交功能 -->
    <PictureList :dataList="dataList" :loading="loading" :showOp="true" :onReload="fetchData" layoutMode="grid" />
    <a-pagination style="text-align: right" v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize" :total="total" :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
      @change="onPageChange" />
  </div>



</template>

<script setup lang="ts">
import { pictureControllerQueryPictureUser } from '@/api/pictureController';
import { spaceControllerGetSpaceById } from '@/api/spaceController';
import { formatSize } from '@/util/format';
import { message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import PictureList from '@/components/PictureList/index.vue';
import PictureSearchForm from '@/components/PictureSearchForm.vue';
import { useLayoutScheme } from '@/composables/useLayoutScheme';
import SpaceBanner from '@/layouts/scheme-1-immersive/components/SpaceBanner.vue';

const { activeSchemeId } = useLayoutScheme();
const isImmersive = computed(() => activeSchemeId.value === 'scheme-1-immersive');

const props = defineProps<{
  id: string | number
}>()
const space = ref<API.SpaceVO>({})

// 获取空间详情
const fetchSpaceDetail = async () => {
  try {
    const res = await spaceControllerGetSpaceById({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

// 数据
const dataList = ref([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.PictureQueryDTO>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 分页参数
const onPageChange = (page, pageSize) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryDTO) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchData()
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 转换搜索参数
    const params = {
      spaceId: props.id,
      ...searchParams.value,
    }
    const res = await pictureControllerQueryPictureUser(params)
    if (res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取数据失败：' + e.message)
  } finally {
    loading.value = false
  }
}


// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

</script>

<style>

</style>
