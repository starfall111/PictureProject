// ===================== 举报原因 =====================
export const REPORT_REASON_ENUM = {
  PORNOGRAPHY: 'PORNOGRAPHY',
  VIOLENCE: 'VIOLENCE',
  SCAM: 'SCAM',
  COPYRIGHT: 'COPYRIGHT',
  SPAM: 'SPAM',
  HARASSMENT: 'HARASSMENT',
  OTHER: 'OTHER',
} as const

export const REPORT_REASON_MAP: Record<string, string> = {
  PORNOGRAPHY: '色情低俗',
  VIOLENCE: '暴力血腥',
  SCAM: '诈骗欺诈',
  COPYRIGHT: '侵权盗版',
  SPAM: '垃圾信息',
  HARASSMENT: '恶意骚扰',
  OTHER: '其他',
}

export const REPORT_REASON_ICON: Record<string, string> = {
  PORNOGRAPHY: 'StopOutlined',
  VIOLENCE: 'ThunderboltOutlined',
  SCAM: 'WarningOutlined',
  COPYRIGHT: 'CopyrightOutlined',
  SPAM: 'DeleteOutlined',
  HARASSMENT: 'FrownOutlined',
  OTHER: 'MoreOutlined',
}

// 严重度：HIGH=red, MEDIUM=orange, LOW=blue
export const REPORT_REASON_SEVERITY: Record<string, string> = {
  PORNOGRAPHY: 'HIGH',
  VIOLENCE: 'HIGH',
  SCAM: 'HIGH',
  COPYRIGHT: 'MEDIUM',
  HARASSMENT: 'MEDIUM',
  SPAM: 'LOW',
  OTHER: 'LOW',
}

export const REPORT_REASON_COLOR: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'blue',
}

export const REPORT_REASON_OPTIONS = Object.keys(REPORT_REASON_MAP).map((key) => ({
  label: REPORT_REASON_MAP[key],
  value: key,
}))

// ===================== 举报状态 =====================
export const REPORT_STATUS_ENUM = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  WARN_ONLY: 'WARN_ONLY',
  REMOVE_ONLY: 'REMOVE_ONLY',
} as const

export const REPORT_STATUS_MAP: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  WARN_ONLY: '仅警告',
  REMOVE_ONLY: '仅删除',
}

export const REPORT_STATUS_COLOR: Record<string, string> = {
  PENDING: 'orange',
  PROCESSING: 'blue',
  APPROVED: 'green',
  REJECTED: 'red',
  WARN_ONLY: 'orange',
  REMOVE_ONLY: 'default',
}

export const REPORT_STATUS_OPTIONS = Object.keys(REPORT_STATUS_MAP).map((key) => ({
  label: REPORT_STATUS_MAP[key],
  value: key,
}))

// ===================== 处理结果 =====================
export const REPORT_HANDLE_RESULT_MAP: Record<string, string> = {
  WARN_ONLY: '警告',
  BAN_TEMP_3: '封禁 3 天',
  BAN_TEMP_7: '封禁 7 天',
  BAN_TEMP_30: '封禁 30 天',
  BAN_PERMANENT: '永久封禁',
  REMOVE_ONLY: '删除图片',
  REJECT: '驳回',
}

export const REPORT_HANDLE_RESULT_OPTIONS = Object.keys(REPORT_HANDLE_RESULT_MAP).map((key) => ({
  label: REPORT_HANDLE_RESULT_MAP[key],
  value: key,
}))

// ===================== 目标类型 =====================
export const REPORT_TARGET_TYPE_MAP: Record<string, string> = {
  PICTURE: '图片',
  USER: '用户',
}

export const REPORT_TARGET_TYPE_OPTIONS = Object.keys(REPORT_TARGET_TYPE_MAP).map((key) => ({
  label: REPORT_TARGET_TYPE_MAP[key],
  value: key,
}))
