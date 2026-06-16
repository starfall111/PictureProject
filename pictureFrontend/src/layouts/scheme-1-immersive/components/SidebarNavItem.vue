<template>
  <div
    class="sidebar-nav-item"
    :class="{ active, indented }"
    role="menuitem"
    :aria-label="label"
    tabindex="0"
    @click="$emit('click')"
    @keyup.enter="$emit('click')"
  >
    <div class="nav-item-indicator" v-if="active" />
    <component :is="icon" class="nav-item-icon" />
    <span v-if="showLabel" class="nav-item-label">
      {{ label }}
      <VipBadge v-if="feature" feature size="small" style="margin-left: 6px" />
    </span>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import VipBadge from '@/components/vip/VipBadge.vue'

defineProps<{
  icon: Component
  label: string
  active?: boolean
  indented?: boolean
  showLabel?: boolean
  feature?: boolean
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.sidebar-nav-item {
  display: flex;
  align-items: center;
  height: var(--sidebar-item-height, 44px);
  padding: 0 16px;
  gap: 12px;
  cursor: pointer;
  border-radius: var(--sidebar-item-radius, 10px);
  margin: var(--sidebar-item-gap, 2px) 6px;
  transition: background-color 150ms ease;
  position: relative;
  user-select: none;
}

.sidebar-nav-item.indented {
  padding-left: calc(16px + var(--sidebar-admin-indent, 8px));
}

.sidebar-nav-item:hover {
  background: var(--sidebar-item-bg-hover, rgba(51, 161, 201, 0.08));
}

.sidebar-nav-item:hover .nav-item-icon,
.sidebar-nav-item:hover .nav-item-label {
  color: var(--sidebar-item-accent, #33A1C9);
}

.sidebar-nav-item.active {
  background: var(--sidebar-item-bg-active, rgba(51, 161, 201, 0.12));
}

.sidebar-nav-item.active .nav-item-icon,
.sidebar-nav-item.active .nav-item-label {
  color: var(--sidebar-item-accent, #33A1C9);
  font-weight: 600;
}

.nav-item-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: var(--sidebar-item-indicator-width, 3px);
  height: 20px;
  border-radius: 0 2px 2px 0;
  background: var(--sidebar-item-accent, #33A1C9);
}

.nav-item-icon {
  font-size: 20px;
  color: var(--sidebar-item-icon-default, #4B5563);
  flex-shrink: 0;
  transition: color 150ms ease;
}

.nav-item-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--sidebar-item-text-default, #4B5563);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 150ms ease;
}

.sidebar-nav-item:focus-visible {
  outline: 2px solid var(--sidebar-item-accent, #33A1C9);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .sidebar-nav-item,
  .nav-item-icon,
  .nav-item-label {
    transition: none;
  }
}
</style>
