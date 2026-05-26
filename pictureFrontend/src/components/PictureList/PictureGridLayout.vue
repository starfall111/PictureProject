<template>
  <div class="picture-grid-layout">
    <a-list :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }" :data-source="dataList"
      :loading="loading">
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <GridCard :picture="picture" :showOp="showOp" :onReload="onReload" />
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import GridCard from './cards/GridCard.vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
}

withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
})
</script>
