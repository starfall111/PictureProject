// ===================== 批量任务状态 =====================
export const BATCH_TASK_STATUS_MAP: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  FAILED: '已失败',
}

export const BATCH_TASK_STATUS_COLOR: Record<string, string> = {
  PENDING: 'orange',
  PROCESSING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error',
}

export const BATCH_TASK_STATUS_OPTIONS = Object.keys(BATCH_TASK_STATUS_MAP).map((key) => ({
  label: BATCH_TASK_STATUS_MAP[key],
  value: key,
}))

// ===================== 搜索来源 =====================
export const SEARCH_SOURCE_MAP: Record<string, string> = {
  bing: 'Bing',
  pexels: ' pexels',
}

export const SEARCH_SOURCE_OPTIONS = Object.keys(SEARCH_SOURCE_MAP).map((key) => ({
  label: SEARCH_SOURCE_MAP[key],
  value: key,
}))
