/**
 * 券状态（与后端 CouponStatusEnum 严格对齐）
 * 注意：后端"吊销"复用 EXPIRED，不独立成态，故前端无 REVOKED。
 */
export const COUPON_STATUS = {
  UNSOLD: 0,    // 未发放
  CLAIMED: 1,   // 已领取未使用
  ACTIVATED: 2, // 已激活
  EXPIRED: 3,   // 已过期（含管理员吊销）
} as const

export const COUPON_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '未发放', color: 'default' },
  1: { text: '待使用', color: 'warning' },
  2: { text: '已激活', color: 'success' },
  3: { text: '已过期', color: 'default' },
}

/** 券包页 Tab 分组 */
export const COUPON_TAB_LIST = [
  { key: 1, label: '待使用' },
  { key: 2, label: '已激活' },
  { key: 3, label: '已过期' },
] as const
