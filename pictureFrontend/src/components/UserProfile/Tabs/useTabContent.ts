import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'

// 筛选参数接口（预留扩展）
export interface TabContentFilters {
  category?: string
  tags?: string[]
  sortBy?: string
}

interface UseTabContentOptions {
  userId: string | number
  fetchFunction: (params: {
    current: number
    pageSize: number
    filters?: TabContentFilters
  }) => Promise<any>
  isVisible: boolean
  filters?: TabContentFilters
}

export function useTabContent({
  userId,
  fetchFunction,
  isVisible,
  filters = computed(() => ({}))
}: UseTabContentOptions) {
  const dataList = ref<API.PictureBriefVO[]>([])
  const currentPage = ref(1)
  const pageSize = 12
  const hasMore = ref(true)
  const isLoading = ref(false)
  const total = ref(0)

  // 获取数据（支持筛选参数）
  const fetchData = async (reset = false) => {
    if (isLoading.value || (!reset && !hasMore.value)) return

    if (reset) {
      currentPage.value = 1
      dataList.value = []
      hasMore.value = true
    }

    isLoading.value = true

    try {
      const filtersValue = typeof filters === 'function' ? filters.value : filters
      const res = await fetchFunction({
        current: currentPage.value,
        pageSize,
        filters: Object.keys(filtersValue || {}).length > 0 ? filtersValue : undefined
      })

      if (res.data.code === 0 && res.data.data) {
        const records = res.data.data.records ?? []
        total.value = res.data.data.total ?? 0

        if (reset) {
          dataList.value = records
        } else {
          dataList.value = [...dataList.value, ...records]
        }

        hasMore.value = dataList.value.length < total.value
        currentPage.value++
      } else {
        message.error('获取数据失败')
      }
    } catch {
      message.error('网络错误，请稍后重试')
    } finally {
      isLoading.value = false
    }
  }

  // IntersectionObserver
  let observer: IntersectionObserver | null = null

  const setupObserver = () => {
    cleanupObserver()

    if (!isVisible.value) return

    setTimeout(() => {
      const sentinel = document.getElementById(`scroll-sentinel-${userId}`)
      if (!sentinel) return

      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && hasMore.value && !isLoading.value) {
            fetchData(false)
          }
        },
        { rootMargin: '200px' }
      )
      observer.observe(sentinel)
    }, 100)
  }

  const cleanupObserver = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  // 监听可见性变化
  watch(isVisible, (visible) => {
    if (visible && dataList.value.length === 0) {
      fetchData(true)
    }
    setupObserver()
  })

  // 监听筛选参数变化，重新加载数据
  if (typeof filters === 'function') {
    watch(filters, () => {
      if (isVisible.value) {
        fetchData(true)
      }
    }, { deep: true })
  }

  // 首次加载
  onMounted(() => {
    if (isVisible.value) {
      fetchData(true)
    }
    setupObserver()
  })

  onUnmounted(() => {
    cleanupObserver()
  })

  return {
    dataList,
    hasMore,
    isLoading,
    total,
    fetchData,
    setupObserver
  }
}
