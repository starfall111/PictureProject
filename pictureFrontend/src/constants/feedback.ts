// ===================== 反馈类型 =====================
export const FEEDBACK_TYPE_ENUM = {
  BUG: 'BUG',
  FEATURE: 'FEATURE',
  ACCOUNT: 'ACCOUNT',
  EXPERIENCE: 'EXPERIENCE',
  OTHER: 'OTHER',
} as const

export const FEEDBACK_TYPE_MAP: Record<string, string> = {
  BUG: 'Bug 反馈',
  FEATURE: '功能建议',
  ACCOUNT: '账号问题',
  EXPERIENCE: '体验问题',
  OTHER: '其他',
}

export const FEEDBACK_TYPE_ICON: Record<string, string> = {
  BUG: 'BugOutlined',
  FEATURE: 'BulbOutlined',
  ACCOUNT: 'UserOutlined',
  EXPERIENCE: 'HeartOutlined',
  OTHER: 'MoreOutlined',
}

export const FEEDBACK_TYPE_COLOR: Record<string, string> = {
  BUG: '#f5222d',
  FEATURE: '#722ed1',
  ACCOUNT: '#fa8c16',
  EXPERIENCE: '#1890ff',
  OTHER: '#8c8c8c',
}

export const FEEDBACK_TYPE_OPTIONS = Object.keys(FEEDBACK_TYPE_MAP).map((key) => ({
  label: FEEDBACK_TYPE_MAP[key],
  value: key,
}))

// ===================== 反馈状态 =====================
export const FEEDBACK_STATUS_ENUM = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  RESOLVED: 'RESOLVED',
  REJECTED: 'REJECTED',
  CLOSED: 'CLOSED',
} as const

export const FEEDBACK_STATUS_MAP: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  REJECTED: '已拒绝',
  CLOSED: '已关闭',
}

export const FEEDBACK_STATUS_COLOR: Record<string, string> = {
  PENDING: 'orange',
  PROCESSING: 'blue',
  RESOLVED: 'green',
  REJECTED: 'red',
  CLOSED: 'default',
}

export const FEEDBACK_STATUS_OPTIONS = Object.keys(FEEDBACK_STATUS_MAP).map((key) => ({
  label: FEEDBACK_STATUS_MAP[key],
  value: key,
}))

// ===================== 反馈优先级 =====================
export const FEEDBACK_PRIORITY_ENUM = {
  P0: 'P0',
  P1: 'P1',
  P2: 'P2',
  P3: 'P3',
} as const

export const FEEDBACK_PRIORITY_MAP: Record<string, string> = {
  P0: '紧急',
  P1: '高',
  P2: '中',
  P3: '低',
}

export const FEEDBACK_PRIORITY_COLOR: Record<string, string> = {
  P0: '#f5222d',
  P1: '#fa8c16',
  P2: '#1890ff',
  P3: '#8c8c8c',
}

export const FEEDBACK_PRIORITY_OPTIONS = Object.keys(FEEDBACK_PRIORITY_MAP).map((key) => ({
  label: `${key} - ${FEEDBACK_PRIORITY_MAP[key]}`,
  value: key,
}))

// ===================== 反馈回复类型 =====================
export const FEEDBACK_REPLY_TYPE_MAP: Record<string, string> = {
  USER_REPLY: '用户回复',
  ADMIN_REPLY: '管理员回复',
  INTERNAL_NOTE: '内部备注',
}
