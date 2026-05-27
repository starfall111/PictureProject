<template>
  <a-select
    :value="activeSchemeId"
    style="width: 160px"
    size="small"
    @change="handleSwitch"
  >
    <a-select-option
      v-for="scheme in schemeList"
      :key="scheme.id"
      :value="scheme.id"
    >
      {{ scheme.name }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  useLayoutScheme,
  getSchemes,
} from '@/composables/useLayoutScheme'
import type { SchemeConfig } from '@/composables/useLayoutScheme'

const { activeSchemeId, switchScheme } = useLayoutScheme()

const schemeList = computed<SchemeConfig[]>(() => {
  return Array.from(getSchemes().values())
})

const handleSwitch = (value: string) => {
  switchScheme(value)
}
</script>
