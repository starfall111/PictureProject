<script setup lang="ts">
import { computed } from 'vue'
import { useLayoutScheme } from '@/composables/useLayoutScheme'

const { currentScheme } = useLayoutScheme()

const antdTheme = computed(() => {
  const overrides = currentScheme.value?.antThemeOverrides
  if (!overrides) return {}
  return overrides
})
</script>

<template>
  <a-config-provider :theme="antdTheme">
    <div id="app">
      <router-view></router-view>
    </div>
  </a-config-provider>
</template>

<style scoped>
</style>
