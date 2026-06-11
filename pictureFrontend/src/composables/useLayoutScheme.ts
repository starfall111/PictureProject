import { ref, computed, readonly, defineAsyncComponent } from 'vue'
import type { Component } from 'vue'

/**
 * Layout scheme configuration.
 * Each scheme maps to a completely independent layout component.
 */
export interface SchemeConfig {
  /** Unique scheme identifier, e.g. 'scheme-1-immersive' */
  id: string
  /** Display name */
  name: string
  /** Short description */
  description: string
  /** Layout component to render (each scheme has its own layout structure) */
  layoutComponent: Component
  /** Ant Design Vue theme token overrides */
  antThemeOverrides?: Record<string, any>
}

const STORAGE_KEY = 'layoutScheme'

/** @deprecated 沉浸式画廊现为唯一维护的布局，默认布局已废弃 */
const _DEPRECATED_DEFAULT_ID = 'default'
const DEFAULT_SCHEME = 'scheme-1-immersive'

// Built-in default scheme (DEPRECATED — kept only as emergency fallback)
const defaultScheme: SchemeConfig = {
  id: _DEPRECATED_DEFAULT_ID,
  name: '默认布局（已废弃）',
  description: '原始布局，已废弃，不再维护',
  layoutComponent: defineAsyncComponent(() => import('@/layouts/BasicLayout.vue')),
}

// Registered schemes registry (pre-seeded with default as fallback)
const schemes = new Map<string, SchemeConfig>([[_DEPRECATED_DEFAULT_ID, defaultScheme]])

// Reactive active scheme id
const activeSchemeId = ref<string>(
  localStorage.getItem(STORAGE_KEY) || DEFAULT_SCHEME
)

/**
 * Register a layout scheme
 */
export function registerScheme(config: SchemeConfig) {
  schemes.set(config.id, config)
}

/**
 * Get all registered schemes
 */
export function getSchemes(): Map<string, SchemeConfig> {
  return schemes
}

/**
 * Layout scheme composable
 */
export function useLayoutScheme() {
  const currentScheme = computed<SchemeConfig>(
    () => schemes.get(activeSchemeId.value) || schemes.get(DEFAULT_SCHEME)!
  )

  /**
   * Switch to a different layout scheme
   */
  function switchScheme(schemeId: string) {
    if (!schemes.has(schemeId)) {
      console.warn(`[useLayoutScheme] Unknown scheme: ${schemeId}`)
      return
    }
    activeSchemeId.value = schemeId
    localStorage.setItem(STORAGE_KEY, schemeId)
    document.documentElement.setAttribute('data-scheme', schemeId)
  }

  /**
   * Initialize the scheme system.
   * Should be called once in main.ts before app.mount()
   */
  function initialize() {
    document.documentElement.setAttribute('data-scheme', activeSchemeId.value)
  }

  return {
    activeSchemeId: readonly(activeSchemeId),
    currentScheme,
    switchScheme,
    initialize,
  }
}
