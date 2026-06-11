export const VIP_TYPE = {
  NORMAL: 0,
  VIP: 1,
} as const

export const VIP_TYPE_MAP: Record<number, string> = {
  0: '普通用户',
  1: 'VIP会员',
}

/** 降级等级 */
export const DEGRADE_LEVEL_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '正常', color: 'success' },
  1: { text: '限制频率', color: 'warning' },
  2: { text: '同步模式', color: 'warning' },
  3: { text: '暂停抢购', color: 'error' },
  4: { text: '全部降级', color: 'error' },
}
