import type { SchemeConfig } from '@/composables/useLayoutScheme'
import { registerScheme } from '@/composables/useLayoutScheme'
import ImmersiveLayout from '@/layouts/ImmersiveLayout.vue'

const scheme1Config: SchemeConfig = {
  id: 'scheme-1-immersive',
  name: '沉浸式画廊',
  description: '全幅画廊布局，5列瀑布流，胶囊搜索框，沉浸式浏览体验',
  layoutComponent: ImmersiveLayout,
  antThemeOverrides: {
    token: {
      colorPrimary: '#33A1C9',
      borderRadius: 12,
    },
  },
}

// Self-register on import
registerScheme(scheme1Config)

export default scheme1Config
