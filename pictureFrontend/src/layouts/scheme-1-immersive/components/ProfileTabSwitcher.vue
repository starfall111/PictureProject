<template>
  <div class="profile-tab-switcher">
    <div
      v-for="tab in tabs"
      :key="tab.key"
      :class="['tab-item', { 'tab-item--active': activeTab === tab.key }]"
      @click="handleTabChange(tab.key)"
    >
      <component :is="tab.icon" class="tab-icon" />
      <span class="tab-label">{{ tab.label }}</span>
      <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type ProfileTab = 'works' | 'likes' | 'favorites'

interface Tab {
  key: ProfileTab
  label: string
  icon: any
  count?: number
}

interface Props {
  activeTab?: ProfileTab
  worksCount?: number
  likesCount?: number
  favoritesCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  activeTab: 'works',
  worksCount: 0,
  likesCount: 0,
  favoritesCount: 0,
})

const emit = defineEmits<{
  (e: 'update:activeTab', value: ProfileTab): void
}>()

// 图标组件 (使用简单的 SVG 图标)
const WorksIcon = {
  template: `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M4 16L8.586 20.586C8.961 20.961 9.47 21.171 10 21.171H19C20.105 21.171 21 20.276 21 19.171V7.171C21 6.066 20.105 5.171 19 5.171H10C9.47 5.171 8.961 5.381 8.586 5.756L4 10.342V16Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `
}

const LikesIcon = {
  template: `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 21.35L10.55 20.03C5.4 15.36 2 12.27 2 8.5C2 5.41 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.08C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.41 22 8.5C22 12.27 18.6 15.36 13.45 20.03L12 21.35Z" fill="currentColor"/>
    </svg>
  `
}

const FavoritesIcon = {
  template: `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M5 4V20H19V4H5Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M9 9H15M9 12H15M9 15H12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
    </svg>
  `
}

const tabs = computed<Tab[]>(() => [
  {
    key: 'works',
    label: '作品',
    icon: WorksIcon,
    count: props.worksCount,
  },
  {
    key: 'likes',
    label: '点赞',
    icon: LikesIcon,
    count: props.likesCount,
  },
  {
    key: 'favorites',
    label: '收藏',
    icon: FavoritesIcon,
    count: props.favoritesCount,
  },
])

const handleTabChange = (tab: ProfileTab) => {
  emit('update:activeTab', tab)
}
</script>

<style scoped>
.profile-tab-switcher {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--surface-secondary, #F3F4F6);
  border-radius: var(--radius-pill, 9999px);
  margin-bottom: 16px;
  width: fit-content;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-pill, 9999px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
  color: var(--fg-secondary, #4B5563);
  font-size: 14px;
  font-weight: 500;
  user-select: none;
  position: relative;
}

.tab-item:hover {
  color: var(--fg-primary, #1A1A1A);
  background: rgba(255, 255, 255, 0.5);
}

.tab-item--active {
  background: var(--surface-primary, #FFFFFF);
  color: var(--fg-primary, #1A1A1A);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tab-label {
  white-space: nowrap;
}

.tab-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent, #33A1C9);
  background: rgba(51, 161, 201, 0.1);
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.tab-item--active .tab-count {
  background: var(--accent, #33A1C9);
  color: #FFFFFF;
}

@media (max-width: 480px) {
  .profile-tab-switcher {
    width: 100%;
    justify-content: space-between;
  }

  .tab-item {
    flex: 1;
    justify-content: center;
    padding: 8px 12px;
    font-size: 13px;
  }

  .tab-label {
    display: none;
  }

  .tab-icon {
    margin: 0;
  }
}
</style>
