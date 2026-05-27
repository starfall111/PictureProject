<script setup lang="ts">
import { computed } from 'vue'
import { useLayoutScheme } from '@/composables/useLayoutScheme'

const { currentScheme } = useLayoutScheme()
</script>

<template>
  <component :is="currentScheme.layoutComponent">
    <router-view />
  </component>
</template>
