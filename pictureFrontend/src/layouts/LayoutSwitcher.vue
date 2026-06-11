<script setup lang="ts">
import { computed } from 'vue'
import { useLayoutScheme } from '@/composables/useLayoutScheme'
import BatchTaskFloatingWidget from '@/components/batch/BatchTaskFloatingWidget.vue'

const { currentScheme } = useLayoutScheme()
</script>

<template>
  <component :is="currentScheme.layoutComponent">
    <router-view />
  </component>
  <!-- 全局悬浮窗：后台批量任务进度 -->
  <BatchTaskFloatingWidget />
</template>
