<template>
  <div class="picture-waterfall-layout">
    <Waterfall
      :list="waterfallList"
      :imgSelector="'img'"
      :heightDifference="0"
      :gutter="12"
      :width="220"
      :breakpoints="breakpoints"
      :animationDuration="300"
      :animationDelay="50"
      animationEffect="fadeIn"
      backgroundColor="#fff"
      :lazyload="true"
    >
      <template #default="{ item }">
        <WaterfallCard :picture="item" />
      </template>
    </Waterfall>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Waterfall } from 'vue-waterfall-plugin-next'
import 'vue-waterfall-plugin-next/dist/style.css'
import WaterfallCard from './cards/WaterfallCard.vue'

interface Props {
  dataList?: API.PictureVO[]
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
})

// todo 应该等待图片计算完成后再渲染瀑布流，否则会出现布局错乱的问题 并且需要响应式布局，再变化屏幕的大小的时候，不应该实时计算，应该提前计算
// 将 picWidth/picHeight 映射为库所需的 imgWidth/imgHeight，使库能预计算布局
const waterfallList = computed(() =>
  props.dataList.map((item) => ({
    ...item,
    imgWidth: item.picWidth ?? 220,
    imgHeight: item.picHeight ?? 220,
  }))
)

const breakpoints = {
  1600: { rowPerView: 4 },
  768: { rowPerView: 3 },
  500: { rowPerView: 2 },
}
</script>

<style scoped>
.picture-waterfall-layout {
  width: 100%;
}
</style>
