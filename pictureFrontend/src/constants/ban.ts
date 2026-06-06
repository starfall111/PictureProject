// ===================== 封禁类型 =====================
export const BAN_TYPE_ENUM = {
  TEMP: 'TEMP',
  PERMANENT: 'PERMANENT',
} as const

export const BAN_TYPE_MAP: Record<string, string> = {
  TEMP: '临时封禁',
  PERMANENT: '永久封禁',
}

export const BAN_TYPE_OPTIONS = Object.keys(BAN_TYPE_MAP).map((key) => ({
  label: BAN_TYPE_MAP[key],
  value: key,
}))

// ===================== 封禁状态 =====================
export const BAN_STATUS_ENUM = {
  ACTIVE: 'ACTIVE',
  EXPIRED: 'EXPIRED',
  UNBANNED: 'UNBANNED',
} as const

export const BAN_STATUS_MAP: Record<string, string> = {
  ACTIVE: '封禁中',
  EXPIRED: '已到期',
  UNBANNED: '已解封',
}

export const BAN_STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'red',
  EXPIRED: 'default',
  UNBANNED: 'green',
}

export const BAN_STATUS_OPTIONS = Object.keys(BAN_STATUS_MAP).map((key) => ({
  label: BAN_STATUS_MAP[key],
  value: key,
}))
