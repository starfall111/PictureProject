/** 批次状态 */
export const BATCH_STATUS = {
  DRAFT: 0,      // 草稿
  PREHEATING: 1, // 预热中
  ONGOING: 2,    // 进行中
  ENDED: 3,      // 已结束
  CANCELLED: 4,  // 已取消
} as const

export const BATCH_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '预热中', color: 'processing' },
  2: { text: '进行中', color: 'success' },
  3: { text: '已结束', color: 'default' },
  4: { text: '已取消', color: 'error' },
}

/** 券类型天数 */
export const COUPON_TYPE_MAP: Record<number, { text: string; color: string }> = {
  3:  { text: '3天VIP体验券', color: '#52c41a' },
  7:  { text: '7天VIP畅享券', color: '#1890ff' },
  30: { text: '30天VIP至尊券', color: '#722ed1' },
}

/** Tab 筛选用的分组 */
export const BATCH_TAB_KEYS = ['ongoing', 'upcoming', 'ended'] as const
export type BatchTabKey = (typeof BATCH_TAB_KEYS)[number]
