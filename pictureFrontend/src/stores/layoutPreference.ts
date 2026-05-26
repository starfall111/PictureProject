import { defineStore } from 'pinia'
import { ref } from 'vue'

export type LayoutMode = 'waterfall' | 'grid'

const STORAGE_KEY = 'pictureLayoutMode'

export const useLayoutPreferenceStore = defineStore('layoutPreference', () => {
  const layoutMode = ref<LayoutMode>(
    (localStorage.getItem(STORAGE_KEY) as LayoutMode) || 'waterfall'
  )

  function setLayoutMode(mode: LayoutMode) {
    layoutMode.value = mode
    localStorage.setItem(STORAGE_KEY, mode)
  }

  return { layoutMode, setLayoutMode }
})
